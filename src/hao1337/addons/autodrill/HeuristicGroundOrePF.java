package hao1337.addons.autodrill;

import java.util.PriorityQueue;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.production.Drill;

/**
 * A heuristic-based pathfinder for automatically placing drills and bridges to
 * harvest connected ore clusters.
 * <br>
 * <br>
 * <strong>Limitations and notes:</strong>
 * <ul>
 * <li>Ore cluster detection is capped at 200 tiles to prevent performance
 * degradation on very large deposits.</li>
 * <li>The pathfinder does not force bridges to avoid jumping over buildings,
 * which can potentially block future drill placements.</li>
 * <li>Water extractors are only considered for drills larger than 2×2
 * blocks.</li>
 * <li>All placement operations respect the player's current team and the
 * engine's build validity rules.</li>
 * </ul>
 * 
 * TODO still incomplete, but work!
 * 
 * @see GroundOrePathFinding
 * @author Hao-1337
 */
public class HeuristicGroundOrePF extends GroundOrePathFinding {
    protected static final int MAX_ROUTE_TARGETS_TO_TRY = 16;
    protected static final int MIN_ROUTE_EXPANSIONS = 160;

    protected Tile outputAnchorTile;
    protected Tile heuristicCenterTile;

    /** Present direction (the side where the final output bridge will point) */
    public static enum Direction {
        RIGHT(new Point2(1, 0), 0),
        UP(new Point2(0, 1), 1),
        LEFT(new Point2(-1, 0), 2),
        DOWN(new Point2(0, -1), 3);

        public final Point2 p;
        public final int r; // rotation index for BuildPlan

        Direction(Point2 p, int r) {
            this.p = p;
            this.r = r;
        }

        public static Direction getOpposite(Direction direction) {
            switch (direction) {
                case RIGHT:
                    return LEFT;
                case UP:
                    return DOWN;
                case LEFT:
                    return RIGHT;
                default:
                    return UP;
            }
        }
    }

    protected enum BatchStage {
        INIT,
        GENERATE_CANDIDATES,
        PREPARE_SELECTION,
        SCORE_CANDIDATES,
        APPLY_SELECTION,
        DONE
    }

    /**
     * Collection of static helper methods used throughout the pathfinder. These
     * utilities perform common world queries such as finding nearby tiles,
     * counting ore in a drill's range, area expansion and flood-fill of ore
     * clusters. They are intentionally generic and do not depend on the
     * instance state of {@link HeuristicGroundOrePF}.
     */
    protected static class Utils {
        /**
         * Get all tiles that touch the border of a block (does NOT include the block's
         * own tiles).
         * Used to find adjacent positions for bridges.
         */
        public static Seq<Tile> getNearbyTiles(int x, int y, int size) {
            Seq<Tile> nearbyTiles = new Seq<>();
            Point2[] nearby = Edges.getEdges(size);
            for (Point2 point2 : nearby) {
                Tile t = Vars.world.tile(x + point2.x, y + point2.y);
                if (t != null)
                    nearbyTiles.add(t);
            }
            return nearbyTiles;
        }

        /**
         * Advanced version that handles different block sizes (e.g. drill size vs
         * bridge size).
         * Calculates correct offset so the "nearby" area is correct when placing a
         * size-N drill next to a size-M bridge.
         */
        public static Seq<Tile> getNearbyTiles(int x, int y, int size1, int size2) {
            int offset1 = (size1 % 2 == 1 && size2 % 2 == 0) ? 1 : 0;
            int offset2 = ((size2 * 2 - 1) / 2);
            return getNearbyTiles(x - offset1, y - offset1, size1 + offset2);
        }

        /**
         * Counts how many tiles of each ore a drill would mine if placed here.
         * Returns the best ore (highest count, preferring non-lowPriority ores) and its
         * count.
         */
        public static ObjectIntMap.Entry<Item> countOre(Tile tile, Drill drill) {
            ObjectIntMap<Item> oreCount = new ObjectIntMap<>();
            for (Tile other : tile.getLinkedTilesAs(drill, new Seq<>())) {
                if (drill.canMine(other)) {
                    oreCount.increment(drill.getDrop(other), 0, 1);
                }
            }
            Seq<Item> itemArray = new Seq<>();
            for (Item i : oreCount.keys())
                itemArray.add(i);
            itemArray.sort((a, b) -> {
                int c = Boolean.compare(!a.lowPriority, !b.lowPriority);
                if (c != 0)
                    return c;
                c = Integer.compare(oreCount.get(a, 0), oreCount.get(b, 0));
                if (c != 0)
                    return c;
                return Integer.compare(a.id, b.id);
            });
            if (itemArray.isEmpty())
                return null;
            Item top = itemArray.peek();
            ObjectIntMap.Entry<Item> entry = new ObjectIntMap.Entry<>();
            entry.key = top;
            entry.value = oreCount.get(top, 0);
            return entry;
        }

        /**
         * Expand a set of tiles by a given radius (adds a "buffer" zone around the
         * ore).
         */
        public static void expandArea(Seq<Tile> tiles, int radius) {
            Seq<Tile> expanded = new Seq<>();
            for (Tile tile : tiles) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (dx == 0 && dy == 0)
                            continue;
                        Tile n = tile.nearby(dx, dy);
                        if (n != null && !tiles.contains(n) && !expanded.contains(n)) {
                            expanded.add(n);
                        }
                    }
                }
            }
            tiles.addAll(expanded);
        }

        /**
         * Flood-fill to find a whole connected ore cluster (max tiles to prevent lag).
         * Uses a fake wall to test if a tile is buildable.
         */
        public static Seq<Tile> getConnectedTiles(Tile start, int maxTiles) {
            Queue<Tile> queue = new Queue<>();
            Seq<Tile> tiles = new Seq<>();
            Seq<Tile> visited = new Seq<>();
            queue.addLast(start);
            Item sourceItem = start.drop();

            while (!queue.isEmpty() && tiles.size < maxTiles) {
                Tile current = queue.removeFirst();
                if (!Build.validPlace(
                        Blocks.copperWall.environmentBuildable() ? Blocks.copperWall : Blocks.berylliumWall,
                        Vars.player.team(), current.x, current.y, 0) ||
                        visited.contains(current)) {
                    continue;
                }
                if (current.drop() == sourceItem) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0)
                                continue;
                            Tile n = current.nearby(dx, dy);
                            if (n != null && !visited.contains(n)) {
                                queue.addLast(n);
                            }
                        }
                    }
                    tiles.add(current);
                }
                visited.add(current);
            }
            tiles.sort(Tile::pos);
            return tiles;
        }

        /** Returns the hitbox rectangle of a block when placed on a tile. */
        public static Rect getBlockRect(Tile tile, Block block) {
            int offset = (block.size - 1) / 2;
            return new Rect(tile.x - offset, tile.y - offset, block.size, block.size);
        }
    }

    /**
     * Creates a new heuristic ground ore pathfinder.
     *
     * @param drill           The drill type the user selected (used as
     *                        default/forced size)
     * @param startTile       The tile the player clicked on (starting point of the
     *                        ore cluster)
     * @param outputDirection Which direction the final output bridge should face
     *                        (RIGHT/UP/LEFT/DOWN)
     * @param bridgeType      (Nullable) Specific bridge type to use (currently
     *                        unused in this version, kept for future compatibility)
     */
    public HeuristicGroundOrePF(Block drill, Tile startTile, Direction outputDirection,
            @Nullable ItemBridge bridgeType) {
        super(drill, startTile, outputDirection, bridgeType);
    }

    /**
     * Generate a sequence of build plans that place drills and bridges to
     * harvest the connected ore area starting from the selected tile.
     * 
     * @return a sequence of {@link BuildPlan} objects representing the
     *         structures to place. The sequence may be empty if no valid
     *         placement could be found.
     */
    @Override
    public BuildBatch createBuildBatch() {
        return new HeuristicBuildBatch();
    }

    public Seq<BuildPlan> build() {
        BuildBatch batch = createBuildBatch();
        while (!batch.isDone()) {
            batch.step(Integer.MAX_VALUE);
        }
        return batch.result();
    }

    protected void sortOreTilesForOutput(Seq<Tile> oreTiles) {
        switch (outDirection) {
            case RIGHT:
                oreTiles.sort((a, b) -> {
                    int dx = Integer.compare(a.x, b.x);
                    if (dx != 0) return dx;
                    return Integer.compare(a.y, b.y);
                });
                break;
            case LEFT:
                oreTiles.sort((a, b) -> {
                    int dx = Integer.compare(b.x, a.x);
                    if (dx != 0) return dx;
                    return Integer.compare(a.y, b.y);
                });
                break;
            case UP:
                oreTiles.sort((a, b) -> {
                    int dy = Integer.compare(a.y, b.y);
                    if (dy != 0) return dy;
                    return Integer.compare(a.x, b.x);
                });
                break;
            case DOWN:
                oreTiles.sort((a, b) -> {
                    int dy = Integer.compare(b.y, a.y);
                    if (dy != 0) return dy;
                    return Integer.compare(a.x, b.x);
                });
                break;
        }
    }

    protected class HeuristicBuildBatch implements BuildBatch {
        protected final Seq<BuildPlan> finalPlans = new Seq<>();
        protected Seq<Tile> clusterOreTiles = new Seq<>();
        protected Seq<Tile> oreTiles = new Seq<>();
        protected ObjectSet<Tile> oreCluster = new ObjectSet<>();
        protected ObjectSet<Tile> globalOccupied = new ObjectSet<>();
        protected ObjectSet<Tile> drillOccupied = new ObjectSet<>();
        protected ObjectSet<Tile> networkNodes = new ObjectSet<>();
        protected ObjectSet<Tile> coveredOre = new ObjectSet<>();
        protected Seq<Candidate> candidates = new Seq<>();

        protected BatchStage stage = BatchStage.INIT;
        protected int oreIndex;
        protected int drillIndex;
        protected int candidateIndex;
        protected int totalCandidateSlots;
        protected int processedCandidateSlots;
        protected int initialCandidateCount;

        protected Candidate bestCandidate;
        protected Seq<BuildPlan> bestRoute;
        protected Seq<BuildPlan> bestExtractors = new Seq<>();
        protected double bestScore = Double.NEGATIVE_INFINITY;
        protected int bestGap = Integer.MAX_VALUE;
        protected int bestTouch = Integer.MIN_VALUE;

        @Override
        public void step(int operationBudget) {
            int remaining = Math.max(1, operationBudget);

            while (remaining > 0 && !isDone()) {
                switch (stage) {
                    case INIT:
                        init();
                        remaining--;
                        break;
                    case GENERATE_CANDIDATES:
                        remaining -= Math.max(1, processCandidateGeneration(remaining));
                        break;
                    case PREPARE_SELECTION:
                        prepareSelection();
                        remaining--;
                        break;
                    case SCORE_CANDIDATES:
                        remaining -= Math.max(1, scoreCandidates(remaining));
                        break;
                    case APPLY_SELECTION:
                        applyBestCandidate();
                        remaining--;
                        break;
                    case DONE:
                        remaining = 0;
                        break;
                }
            }
        }

        @Override
        public boolean isDone() { return stage == BatchStage.DONE; }

        @Override
        public float progress() {
            if (stage == BatchStage.DONE)
                return 1f;

            float generationProgress = totalCandidateSlots <= 0
                    ? (stage.ordinal() >= BatchStage.PREPARE_SELECTION.ordinal() ? 1f : 0f)
                    : processedCandidateSlots / (float) totalCandidateSlots;
            float coveredProgress = clusterOreTiles.isEmpty() ? 1f : coveredOre.size / (float) clusterOreTiles.size;
            float reductionProgress = initialCandidateCount <= 0
                    ? (stage == BatchStage.DONE ? 1f : 0f)
                    : (initialCandidateCount - candidates.size) / (float) initialCandidateCount;

            if (stage == BatchStage.SCORE_CANDIDATES && !candidates.isEmpty() && initialCandidateCount > 0)
                reductionProgress += (candidateIndex / (float) candidates.size) / initialCandidateCount;

            float selectionProgress = Math.min(1f, Math.max(coveredProgress, reductionProgress));

            switch (stage) {
                case INIT:
                    return 0f;
                case GENERATE_CANDIDATES:
                    return Math.min(0.55f, 0.05f + generationProgress * 0.5f);
                case PREPARE_SELECTION:
                    return 0.6f;
                case SCORE_CANDIDATES:
                case APPLY_SELECTION:
                    return Math.min(0.99f, 0.6f + selectionProgress * 0.39f);
                case DONE:
                default:
                    return 1f;
            }
        }

        @Override
        public Seq<BuildPlan> result() { return finalPlans; }

        protected void init() {
            clusterOreTiles = Utils.getConnectedTiles(selectedTile, maxTiles);
            oreTiles = new Seq<>();
            oreTiles.addAll(clusterOreTiles);
            Utils.expandArea(oreTiles, maxDrillSize / 4);

            if (clusterOreTiles.isEmpty()) {
                stage = BatchStage.DONE;
                return;
            }

            oreCluster = new ObjectSet<>();
            for (Tile tile : clusterOreTiles) {
                oreCluster.add(tile);
            }

            outputAnchorTile = findOutputAnchorTile(clusterOreTiles, selectedTile);
            heuristicCenterTile = findClusterCenter(clusterOreTiles);

            Point2 outlet = outDirection.p.cpy();
            int step = Math.min(4, Math.max(0, bridgeRange - 2));
            for (int i = 0; i < step; i++) {
                outlet.add(outDirection.p);
            }

            allowedDrill.sort((a, b) -> {
                int sizeCompare = Integer.compare(b.size, a.size);
                if (sizeCompare != 0)
                    return sizeCompare;
                return Integer.compare(b.id, a.id);
            });

            Tile outputBridge = Vars.world.tile(outputAnchorTile.x + outlet.x, outputAnchorTile.y + outlet.y);
            networkNodes.add(outputAnchorTile);
            globalOccupied.add(outputAnchorTile);
            finalPlans.add(new BuildPlan(outputAnchorTile.x, outputAnchorTile.y, outDirection.r, bridgeBlock, outlet));

            if (outputBridge != null) {
                globalOccupied.add(outputBridge);
                finalPlans.add(new BuildPlan(outputBridge.x, outputBridge.y, outDirection.r, bridgeBlock));
            }

            sortOreTilesForOutput(oreTiles);
            totalCandidateSlots = oreTiles.size * allowedDrill.size;
            processedCandidateSlots = 0;
            stage = BatchStage.GENERATE_CANDIDATES;
        }

        protected int processCandidateGeneration(int budget) {
            int processed = 0;

            while (processed < budget && oreIndex < oreTiles.size) {
                Tile tile = oreTiles.get(oreIndex);
                Block drillMode = allowedDrill.get(drillIndex);
                processed++;
                processedCandidateSlots++;

                if (!forceSelectedSize || drillMode == selectedDrill) {
                    if (drillMode instanceof Drill drill && Build.validPlace(drill, Vars.player.team(), tile.x, tile.y, 0)) {
                        ObjectIntMap.Entry<Item> ores = Utils.countOre(tile, drill);
                        if (ores != null && ores.value > 0 && ores.key == targetOre) {
                            float coverage = ores.value / (float) (drillMode.size * drillMode.size);
                            if (coverage >= minimumCoverage) {
                                int distToOut = Math.abs(tile.x - heuristicCenterTile.x) + Math.abs(tile.y - heuristicCenterTile.y);
                                int score = drill.size * 10000 + ores.value * 100 + Math.round(coverage * 100f) * 4 - distToOut;
                                Seq<Tile> coveredTiles = getCoveredOreTiles(tile, drill);
                                candidates.add(new Candidate(tile, drill, score, ores.value, coverage, coveredTiles));
                            }
                        }
                    }
                }

                drillIndex++;
                if (drillIndex >= allowedDrill.size) {
                    drillIndex = 0;
                    oreIndex++;
                }
            }

            if (oreIndex >= oreTiles.size)
                stage = BatchStage.PREPARE_SELECTION;

            return processed;
        }

        protected void prepareSelection() {
            candidates = pruneDominatedCandidates(candidates);
            candidates.sort(HeuristicGroundOrePF.this::compareInitialCandidates);
            initialCandidateCount = candidates.size;

            if (candidates.isEmpty()) {
                stage = BatchStage.DONE;
                return;
            }

            resetSelectionScan();
            stage = BatchStage.SCORE_CANDIDATES;
        }

        protected void resetSelectionScan() {
            candidateIndex = 0;
            bestCandidate = null;
            bestRoute = null;
            bestExtractors = new Seq<>();
            bestScore = Double.NEGATIVE_INFINITY;
            bestGap = Integer.MAX_VALUE;
            bestTouch = Integer.MIN_VALUE;
        }

        protected int scoreCandidates(int budget) {
            int processed = 0;

            while (processed < budget && candidateIndex < candidates.size) {
                Candidate candidate = candidates.get(candidateIndex++);
                processed++;

                if (isHitboxOccupied(candidate.tile, candidate.drill, globalOccupied)) {
                    continue;
                }

                int gain = countUncoveredOre(candidate.tile, candidate.drill, coveredOre);
                if (gain <= 0) {
                    continue;
                }

                Seq<BuildPlan> route = aStarBridgeRoute(candidate.tile, candidate.drill, networkNodes, globalOccupied);
                if (route == null) {
                    continue;
                }

                Seq<BuildPlan> extractorPlans = useWaterExtractor
                        ? findBestWaterExtractor(candidate.tile, candidate.drill, globalOccupied, oreCluster)
                        : new Seq<>();
                if (extractorPlans == null) {
                    continue;
                }

                int extractorCompactness = extractorPlanCompactness(extractorPlans, candidate.tile, candidate.drill,
                        globalOccupied);
                int compactness = countTouchingOccupied(candidate.tile, candidate.drill, drillOccupied);
                int supportTouch = countTouchingOccupied(candidate.tile, candidate.drill, globalOccupied);
                int gap = drillOccupied.isEmpty()
                        ? distanceFromHitboxToTiles(candidate.tile, candidate.drill, networkNodes)
                        : distanceFromHitboxToTiles(candidate.tile, candidate.drill, drillOccupied);
                int dist = distanceToNetwork(candidate.tile, networkNodes);
                int redundancy = candidate.oreCount - gain;
                int routeCost = routePlacementCost(route);
                int routeCompactness = routeCompactnessScore(route, globalOccupied);
                int bboxGrowth = boundingAreaGrowth(drillOccupied, candidate.tile, candidate.drill);
                double marginalScore = gain * 260.0
                        + compactness * 500.0
                        + supportTouch * 80.0
                        + extractorCompactness * 120.0
                        - gap * 320.0
                        - bboxGrowth * 60.0
                        - routeCost * 90.0
                        - redundancy * 200.0
                        - dist * 8.0;

                if (shouldRejectDedicatedRoute(route, gain, compactness, supportTouch, routeCompactness, gap,
                        marginalScore)) {
                    continue;
                }

                double score = candidate.score
                        + gain * 300.0
                        + candidate.coverage * 120.0
                        + compactness * 500.0
                        + supportTouch * 50.0
                        + extractorCompactness * 180.0
                        - gap * 700.0
                        - bboxGrowth * 60.0
                        - routeCost * 30.0
                        - redundancy * 200.0
                        - dist * 8.0;

                if (route.isEmpty()) {
                    score += 50.0;
                }

                if (bestCandidate == null || isBetterCandidate(candidate, bestCandidate, gap, bestGap, compactness,
                        bestTouch, score, bestScore)) {
                    bestGap = gap;
                    bestTouch = compactness;
                    bestScore = score;
                    bestCandidate = candidate;
                    bestRoute = route;
                    bestExtractors = extractorPlans;
                }
            }

            if (candidateIndex >= candidates.size) {
                stage = BatchStage.APPLY_SELECTION;
            }

            return processed;
        }

        protected void applyBestCandidate() {
            if (bestCandidate == null) {
                stage = BatchStage.DONE;
                return;
            }

            finalPlans.addAll(bestRoute);
            finalPlans.add(new BuildPlan(bestCandidate.tile.x, bestCandidate.tile.y, 0, bestCandidate.drill));
            finalPlans.addAll(bestExtractors);

            for (Tile tile : bestCandidate.tile.getLinkedTilesAs((Drill) bestCandidate.drill, new Seq<>())) {
                if (tile != null && tile.drop() == targetOre) {
                    coveredOre.add(tile);
                }
            }

            lockHitbox(bestCandidate.tile, bestCandidate.drill, globalOccupied);
            lockHitbox(bestCandidate.tile, bestCandidate.drill, drillOccupied);

            for (BuildPlan extractorPlan : bestExtractors) {
                Tile extractorTile = Vars.world.tile(extractorPlan.x, extractorPlan.y);
                if (extractorTile != null) {
                    lockHitbox(extractorTile, WATER_EXTRACTOR, globalOccupied);
                }
            }

            for (BuildPlan plan : bestRoute) {
                Tile routeTile = Vars.world.tile(plan.x, plan.y);
                if (routeTile != null) {
                    globalOccupied.add(routeTile);
                    networkNodes.add(routeTile);
                }
            }

            candidates.remove(bestCandidate);
            if (candidates.isEmpty()) {
                stage = BatchStage.DONE;
                return;
            }

            resetSelectionScan();
            stage = BatchStage.SCORE_CANDIDATES;
        }
    }

    /**
     * Count how many tiles within a square area centred at (x,y) contain the
     * current target ore. The area is based on a block of the given size.
     *
     * @param x    center x-coordinate of the area
     * @param y    center y-coordinate of the area
     * @param size size of the square (block size) to inspect
     * @return the number of tiles inside the area whose {@code drop()} equals
     *         {@link #targetOre}
     */
    protected int countOreInArea(int x, int y, int size) {
        int count = 0;
        int offset = (size - 1) / 2;
        int maxo = offset + (size % 2 == 0 ? 1 : 0);
        for (int dx = -offset; dx <= maxo; dx++) {
            for (int dy = -offset; dy <= maxo; dy++) {
                Tile t = Vars.world.tile(x + dx, y + dy);
                if (t != null && t.drop() == targetOre)
                    count++;
            }
        }
        return count;
    }

    /**
     * Determine whether any tile in the extractor hitbox is orthogonally
     * adjacent to any tile in the drill hitbox.
     *
     * @param drillTiles sequence of tiles occupied by the drill
     * @param exTiles    sequence of tiles occupied by the prospective extractor
     * @return {@code true} if there is at least one pair of tiles at
     *         Manhattan distance 1, otherwise {@code false}
     */
    protected boolean isAdjacentToDrill(Seq<Tile> drillTiles, Seq<Tile> exTiles) {
        return isHitboxAdjacent(drillTiles, exTiles);
    }

    protected boolean isHitboxAdjacent(Seq<Tile> firstTiles, Seq<Tile> secondTiles) {
        for (Tile first : firstTiles) {
            for (Tile second : secondTiles) {
                if (first == null || second == null)
                    continue;
                if (Math.abs(first.x - second.x) + Math.abs(first.y - second.y) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
    * Search the neighbourhood around a drill for the required number of valid
    * water extractor placements. Extractors are chosen to block as little ore
    * as possible while still touching the drill and keeping the layout compact.
     *
     * @param drillCenter centre tile of the drill
     * @param drill       drill block being placed (used for size and range)
     * @param occupied    set of tiles already reserved by other structures
     * @param oreCluster  exact target-ore cluster tiles; extractors must not overlap them
     * @return a sequence containing the required extractor build plans, or
     *         {@code null} if the drill cannot be fully supported.
     */
    protected Seq<BuildPlan> findBestWaterExtractor(Tile drillCenter, Block drill, ObjectSet<Tile> occupied,
            ObjectSet<Tile> oreCluster) {
        int required = requiredWaterExtractors(drill);
        if (required == 0)
            return new Seq<>();

        Seq<ExtractorCandidate> options = new Seq<>();
        int range = drill.size + 4;
        Seq<Tile> drillHitbox = getHitboxTiles(drillCenter, drill);

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                int ex = drillCenter.x + dx;
                int ey = drillCenter.y + dy;
                Tile exCenter = Vars.world.tile(ex, ey);
                if (exCenter == null)
                    continue;

                Seq<Tile> exHitbox = getHitboxTiles(exCenter, WATER_EXTRACTOR);

                boolean overlap = false;
                for (Tile t : exHitbox) {
                    if (t == null || occupied.contains(t) || drillHitbox.contains(t)
                            || oreCluster.contains(t) || t.drop() == targetOre) {
                        overlap = true;
                        break;
                    }
                }
                if (overlap)
                    continue;

                if (!Build.validPlace(WATER_EXTRACTOR, Vars.player.team(), ex, ey, 0))
                    continue;

                if (isAdjacentToDrill(drillHitbox, exHitbox)) {
                    int blockedOre = countOreInArea(ex, ey, WATER_EXTRACTOR.size);
                    int touching = countTouchingOccupied(exCenter, WATER_EXTRACTOR, occupied);
                    int drillTouch = countAdjacentPairs(drillHitbox, exHitbox);
                    int totalTouch = touching + drillTouch;
                    int dist = Math.abs(ex - drillCenter.x) + Math.abs(ey - drillCenter.y);
                    options.add(new ExtractorCandidate(ex, ey, blockedOre, touching, drillTouch, totalTouch, dist));
                }
            }
        }

        if (options.size < required)
            return null;

        options.sort((a, b) -> {
            int c = Integer.compare(b.totalTouch, a.totalTouch);
            if (c == 0)
                c = Integer.compare(b.drillTouch, a.drillTouch);
            if (c == 0)
                c = Integer.compare(a.blockedOre, b.blockedOre);
            if (c == 0)
                c = Integer.compare(b.touching, a.touching);
            if (c == 0)
                c = Integer.compare(a.distanceToDrill, b.distanceToDrill);
            if (c == 0)
                c = Integer.compare(tilePos(a.x, a.y), tilePos(b.x, b.y));
            return c;
        });

        if (required == 1) {
            ExtractorCandidate best = options.first();
            return Seq.with(new BuildPlan(best.x, best.y, 0, WATER_EXTRACTOR));
        }

        ExtractorCandidate first = null;
        ExtractorCandidate second = null;
        int bestPairScore = Integer.MIN_VALUE;
        int bestPairPos = Integer.MAX_VALUE;

        for (int i = 0; i < options.size; i++) {
            ExtractorCandidate left = options.get(i);
            Tile leftTile = Vars.world.tile(left.x, left.y);
            if (leftTile == null)
                continue;

            for (int j = i + 1; j < options.size; j++) {
                ExtractorCandidate right = options.get(j);
                Tile rightTile = Vars.world.tile(right.x, right.y);
                if (rightTile == null)
                    continue;
                if (hitboxesOverlap(leftTile, WATER_EXTRACTOR, rightTile, WATER_EXTRACTOR))
                    continue;

                int blockedOre = left.blockedOre + right.blockedOre;
                int touching = left.totalTouch + right.totalTouch;
                int spacing = manhattan(leftTile, rightTile);
                int mutualTouch = countAdjacentPairs(getHitboxTiles(leftTile, WATER_EXTRACTOR), getHitboxTiles(rightTile, WATER_EXTRACTOR));
                int score = touching * 30 + mutualTouch * 40 - blockedOre * 100 - spacing;

                if (mutualTouch > 0)
                    score += 15;

                int pairPos = Math.min(tilePos(left.x, left.y), tilePos(right.x, right.y));
                if (score > bestPairScore || (score == bestPairScore && pairPos < bestPairPos)) {
                    bestPairScore = score;
                    bestPairPos = pairPos;
                    first = left;
                    second = right;
                }
            }
        }

        if (first == null || second == null)
            return null;

        return Seq.with(
                new BuildPlan(first.x, first.y, 0, WATER_EXTRACTOR),
                new BuildPlan(second.x, second.y, 0, WATER_EXTRACTOR));
    }

    protected int requiredWaterExtractors(Block drill) {
        if (drill.size >= 4)
            return 2;
        if (drill.size == 3)
            return 1;
        return 0;
    }

    /**
     * Attempt to connect a candidate drill placement to the existing bridge
     * network using A* pathfinding. The search expands from the drill edges
     * and respects the maximum bridge jump range. Occupied tiles are treated
     * as impassable.
     *
     * @param drillCenter  the centre tile of the drill being connected
     * @param drillType    type of drill (used only to compute edge tiles)
     * @param networkNodes set of tiles currently part of the bridge network
     * @param occupied     tiles that cannot be used for new bridges
     * @return a sequence of {@link BuildPlan}s for the bridges leading to the
     *         network, or {@code null} if no connection is possible. An empty
     *         sequence means the drill is already adjacent to the network.
     */
    protected Seq<BuildPlan> aStarBridgeRoute(Tile drillCenter, Block drillType,
            ObjectSet<Tile> networkNodes,
            ObjectSet<Tile> occupied) {

        Seq<Tile> startEdges = getAdjacentTiles(drillCenter, drillType);
        startEdges.sort(this::compareTilesForPreference);

        // Already touching network?
        for (Tile adj : startEdges) {
            if (networkNodes.contains(adj))
                return new Seq<>();
        }

        Seq<Tile> sortedTargets = sortedNetworkNodes(networkNodes, startEdges);
        Seq<BuildPlan> bestRoute = null;
        int bestScore = Integer.MAX_VALUE;
        Tile bestTarget = null;
        int bestTargetDistance = Integer.MAX_VALUE;
        int bestRouteCost = Integer.MAX_VALUE;
        int triedTargets = 0;

        for (Tile target : sortedTargets) {
            int targetDistance = distanceFromTilesToTile(startEdges, target);
            if (bestTarget != null && targetDistance > bestTargetDistance) {
                break;
            }
            if (triedTargets >= MAX_ROUTE_TARGETS_TO_TRY && bestTarget != null) {
                break;
            }
            triedTargets++;

            Seq<Tile> path = aStarBridgePathToTarget(startEdges, target, occupied);
            if (path == null)
                continue;
            if (!isValidBridgePath(path, drillCenter, drillType, networkNodes))
                continue;

            Seq<BuildPlan> route = buildBridgePlans(path);
            int routeCost = routePlacementCost(route);
            int score = routeCost * 100
                    - routeCompactnessScore(route, occupied) * 35
                    + routeCenterDrift(route) * 12
                    + targetDistance * 80
                    + distanceToOutputRoot(target) * 4
                    + perpendicularDistanceToOutputAxis(target) * 20;

            if (bestTarget == null
                    || targetDistance < bestTargetDistance
                    || (targetDistance == bestTargetDistance && routeCost < bestRouteCost)
                    || (targetDistance == bestTargetDistance && routeCost == bestRouteCost && score < bestScore)
                    || (targetDistance == bestTargetDistance && routeCost == bestRouteCost && score == bestScore
                            && compareTilesForPreference(target, bestTarget) < 0)) {
                bestTargetDistance = targetDistance;
                bestRouteCost = routeCost;
                bestScore = score;
                bestTarget = target;
                bestRoute = route;
            }
        }

        return bestRoute;
    }

    protected boolean isValidBridgePath(Seq<Tile> path, Tile drillCenter, Block drillType, ObjectSet<Tile> networkNodes) {
        if (path.isEmpty())
            return false;

        Seq<Tile> startEdges = getAdjacentTiles(drillCenter, drillType);
        if (!startEdges.contains(path.first()))
            return false;
        if (!networkNodes.contains(path.peek()))
            return false;

        for (int i = 0; i < path.size - 1; i++) {
            Tile from = path.get(i);
            Tile to = path.get(i + 1);
            if (!isAxisAligned(from, to) || manhattan(from, to) > bridgeRange)
                return false;
        }

        return true;
    }

    protected Seq<Tile> aStarBridgePathToTarget(Seq<Tile> startEdges, Tile target, ObjectSet<Tile> occupied) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        ObjectSet<Tile> closedSet = new ObjectSet<>();
        ObjectIntMap<Tile> bestCost = new ObjectIntMap<>();
        int expansions = 0;
        int maxExpansions = Math.max(MIN_ROUTE_EXPANSIONS, maxDepth * 6);

        for (Tile adj : startEdges) {
            if (!isValidBridge(adj, occupied))
                continue;

            int h = bridgeTargetHeuristic(adj, target);
            bestCost.put(adj, 0);
            openSet.add(new AStarNode(adj, null, -1, 0, h));
        }

        AStarNode goal = null;

        while (!openSet.isEmpty()) {
            AStarNode curr = openSet.poll();
            if (++expansions > maxExpansions)
                break;

            if (closedSet.contains(curr.tile))
                continue;

            closedSet.add(curr.tile);

            if (curr.tile == target) {
                goal = curr;
                break;
            }

            for (Direction d : Direction.values()) {
                for (int len = bridgeRange; len >= 1; len--) {
                    Tile next = curr.tile.nearby(d.p.x * len, d.p.y * len);
                    if (next == null || closedSet.contains(next))
                        continue;
                    if (!isAxisAligned(curr.tile, next))
                        continue;
                    if (!isLineClear(curr.tile, next, occupied))
                        continue;
                    if (next != target && !isValidBridge(next, occupied))
                        continue;

                    int turnPenalty = 0;
                    if (curr.parent != null && curr.dir != -1 && curr.dir != d.ordinal()) {
                        turnPenalty = 2;
                    }

                    int newG = curr.gCost + bridgeGravityStepCost(curr.tile, next, len, turnPenalty, occupied, target);
                    int existingBest = bestCost.get(next, Integer.MAX_VALUE);
                    if (newG >= existingBest)
                        continue;

                    int newH = bridgeTargetHeuristic(next, target);
                    bestCost.put(next, newG);
                    openSet.add(new AStarNode(next, curr, d.ordinal(), newG, newH));
                }
            }
        }

        if (goal == null)
            return null;

        Seq<Tile> path = new Seq<>();
        for (AStarNode node = goal; node != null; node = node.parent) {
            path.add(node.tile);
        }
        path.reverse();
        return compressPath(path);
    }

    protected int bridgeTargetHeuristic(Tile tile, Tile target) {
        int heuristic = manhattan(tile, target) * 10;
        heuristic += distanceToHeuristicCenter(tile) * 3;
        heuristic += perpendicularDistanceToOutputAxis(tile) * 4;
        return heuristic;
    }

    protected int bridgeGravityStepCost(Tile from, Tile to, int len, int turnPenalty, ObjectSet<Tile> occupied, Tile target) {
        int cost = len * 10 + turnPenalty * 12;

        int fromCenter = distanceToHeuristicCenter(from);
        int toCenter = distanceToHeuristicCenter(to);
        if (toCenter > fromCenter) {
            cost += (toCenter - fromCenter) * 6;
        } else if (toCenter == fromCenter) {
            cost += 2;
        }

        int fromAxis = perpendicularDistanceToOutputAxis(from);
        int toAxis = perpendicularDistanceToOutputAxis(to);
        if (toAxis > fromAxis) {
            cost += (toAxis - fromAxis) * 8;
        }

        int touching = countTouchingOccupied(to, bridgeBlock, occupied);
        cost -= Math.min(touching * 6, 18);

        int targetProgress = manhattan(from, target) - manhattan(to, target);
        if (targetProgress <= 0) {
            cost += 10;
        }

        return Math.max(1, cost);
    }

    protected Seq<BuildPlan> buildBridgePlans(Seq<Tile> path) {
        Seq<BuildPlan> plans = new Seq<>();

        for (int i = 0; i < path.size - 1; i++) {
            Tile from = path.get(i);
            Tile to = path.get(i + 1);
            Point2 config = new Point2(to.x - from.x, to.y - from.y);
            plans.add(new BuildPlan(from.x, from.y, 0, bridgeBlock, config));
        }

        return plans;
    }

    protected Seq<Tile> sortedNetworkNodes(ObjectSet<Tile> networkNodes) {
        Seq<Tile> sorted = new Seq<>();
        for (Tile tile : networkNodes) {
            if (tile != null) {
                sorted.add(tile);
            }
        }
        sorted.sort(this::compareTilesForPreference);
        return sorted;
    }

    protected Seq<Tile> sortedNetworkNodes(ObjectSet<Tile> networkNodes, Seq<Tile> startEdges) {
        Seq<Tile> sorted = new Seq<>();
        for (Tile tile : networkNodes) {
            if (tile != null) {
                sorted.add(tile);
            }
        }
        sorted.sort((a, b) -> {
            int c = Integer.compare(distanceFromTilesToTile(startEdges, a), distanceFromTilesToTile(startEdges, b));
            if (c != 0)
                return c;
            return compareTilesForPreference(a, b);
        });
        return sorted;
    }

    protected int distanceFromTilesToTile(Seq<Tile> tiles, Tile target) {
        int best = Integer.MAX_VALUE;

        for (Tile tile : tiles) {
            if (tile == null || target == null)
                continue;
            int distance = manhattan(tile, target);
            if (distance < best) {
                best = distance;
            }
        }

        return best == Integer.MAX_VALUE ? Integer.MAX_VALUE / 2 : best;
    }

    protected int nearestNetworkDistance(Tile t, ObjectSet<Tile> networkNodes) {
        int best = Integer.MAX_VALUE;

        for (Tile n : networkNodes) {
            int d = Math.abs(t.x - n.x) + Math.abs(t.y - n.y);
            if (d < best)
                best = d;
        }

        return best;
    }

    protected int distanceToHeuristicCenter(Tile tile) {
        Tile center = heuristicCenterTile != null ? heuristicCenterTile : selectedTile;
        return center == null || tile == null ? Integer.MAX_VALUE : manhattan(tile, center);
    }

    /**
     * Check whether a tile is a legal location for a new item bridge, taking
     * into account the occupied set and the engine's placement rules.
     *
     * @param t        the tile to validate (may be {@code null})
     * @param occupied set of tiles already reserved by other structures
     * @return {@code true} if a bridge can be placed here, {@code false}
     *         otherwise
     */
    protected boolean isValidBridge(Tile t, ObjectSet<Tile> occupied) {
        return t != null && !occupied.contains(t)
                && Build.validPlace(bridgeBlock, Vars.player.team(), t.x, t.y, 0);
    }

    /**
     * Compute the Manhattan distance between two tiles (L1 norm).
     *
     * @param a first tile
     * @param b second tile
     * @return |a.x-b.x| + |a.y-b.y|
     */
    protected int manhattan(Tile a, Tile b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    /**
     * Simple wrapper around
     * {@link Build#validPlace(Block, mindustry.game.Team, int, int, int)}
     * using the current player's team. Exposed for subclasses.
     *
     * @param block    block to check
     * @param tile     target tile
     * @param rotation rotation index for placement
     * @return {@code true} if the block can be legally built here
     */
    protected boolean isPlaceable(Block block, Tile tile, int rotation) {
        return Build.validPlace(block, Vars.player.team(), tile.x, tile.y, rotation);
    }

    /**
     * Determine if a given block is a valid drill placement, including the
     * additional requirement that it would mine at least one tile of the
     * target ore.
     *
     * @param block block to test (should be a drill)
     * @param tile  location to attempt placement
     * @return {@code true} if the block can be placed and would mine ore
     */
    protected boolean isDrillPlaceable(Block block, Tile tile) {
        if (!isPlaceable(block, tile, 0))
            return false;
        if (block instanceof Drill drill) {
            ObjectIntMap.Entry<Item> ores = Utils.countOre(tile, drill);
            return ores != null && ores.value > 0;
        }
        return false;
    }

    /**
     * Return all tiles covered by a block's hitbox when centred on the
     * specified tile. Handles odd and even-sized blocks correctly.
     *
     * @param center central tile for the block placement
     * @param block  block whose hitbox is required
     * @return sequence of world tiles that the block would occupy
     */
    protected Seq<Tile> getHitboxTiles(Tile center, Block block) {
        Seq<Tile> tiles = new Seq<>();
        int offset = (block.size - 1) / 2;
        int limit = offset + (block.size % 2 == 0 ? 1 : 0);

        for (int dx = -offset; dx <= limit; dx++) {
            for (int dy = -offset; dy <= limit; dy++) {
                tiles.add(Vars.world.tile(center.x + dx, center.y + dy));
            }
        }
        return tiles;
    }

    /**
     * Check whether any tile within a block's hitbox is already marked as
     * occupied in the provided set.
     *
     * @param center   centre tile of the block placement
     * @param block    block to test
     * @param occupied set of tiles that are already reserved
     * @return {@code true} if at least one tile of the hitbox is occupied
     */
    protected boolean isHitboxOccupied(Tile center, Block block, ObjectSet<Tile> occupied) {
        for (Tile t : getHitboxTiles(center, block)) {
            if (t == null || occupied.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mark every tile in a block's hitbox as occupied by adding it to the
     * supplied set. Null tiles are ignored.
     *
     * @param center   central tile where the block will be placed
     * @param block    block type
     * @param occupied set to mutate with blocked tiles
     */
    protected void lockHitbox(Tile center, Block block, ObjectSet<Tile> occupied) {
        for (Tile t : getHitboxTiles(center, block)) {
            if (t != null)
                occupied.add(t);
        }
    }

    protected boolean hitboxesOverlap(Tile firstCenter, Block firstBlock, Tile secondCenter, Block secondBlock) {
        Seq<Tile> firstHitbox = getHitboxTiles(firstCenter, firstBlock);
        for (Tile tile : getHitboxTiles(secondCenter, secondBlock)) {
            if (tile != null && firstHitbox.contains(tile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute all tiles directly adjacent to a block's hitbox (4‑way
     * neighbours) that are not themselves part of the hitbox.
     *
     * @param center central tile of the block
     * @param block  block whose adjacency is required
     * @return sequence of distinct neighbouring tiles
     */
    protected Seq<Tile> getAdjacentTiles(Tile center, Block block) {
        Seq<Tile> hitbox = getHitboxTiles(center, block);
        Seq<Tile> adj = new Seq<>();
        for (Tile t : hitbox) {
            if (t == null)
                continue;
            for (Direction d : Direction.values()) {
                int nx = t.x + (int) d.p.x;
                int ny = t.y + (int) d.p.y;
                Tile neighbor = Vars.world.tile(nx, ny);
                if (neighbor != null && !hitbox.contains(neighbor) && !adj.contains(neighbor)) {
                    adj.add(neighbor);
                }
            }
        }
        return adj;
    }

    /**
     * Helper used when orienting drills: compute the cardinal rotation index
     * corresponding to the direction from one tile to another, favouring the
     * axis with larger delta.
     *
     * @param from start tile
     * @param to   destination tile
     * @return rotation index defined by {@link Direction} enum (0=RIGHT,1=UP,..
     */
    @SuppressWarnings("unused")
    protected int getDirectionTo(Tile from, Tile to) {
        if (Math.abs(from.x - to.x) > Math.abs(from.y - to.y)) {
            return from.x < to.x ? Direction.RIGHT.r : Direction.LEFT.r;
        } else {
            return from.y < to.y ? Direction.UP.r : Direction.DOWN.r;
        }
    }

    /**
     * Checks if a line of tiles between two points is clear of occupied tiles.
     * Uses Bresenham-like algorithm to trace a line from the source tile to the
     * destination tile,
     * checking if any intermediate tiles are occupied.
     *
     * @param from     the starting tile
     * @param to       the ending tile
     * @param occupied a set of tiles that are considered occupied/blocked
     * @return true if the line is clear (no occupied tiles in between), false
     *         otherwise
     */
    @SuppressWarnings("unused")
    protected boolean isLineClear(Tile from, Tile to, ObjectSet<Tile> occupied) {
        int dx = Integer.signum(to.x - from.x);
        int dy = Integer.signum(to.y - from.y);
        int steps = Math.max(Math.abs(to.x - from.x), Math.abs(to.y - from.y));
        for (int i = 1; i < steps; i++) {
            Tile mid = Vars.world.tile(from.x + dx * i, from.y + dy * i);
            if (mid != null && occupied.contains(mid))
                return false;
        }
        return true;
    }

    /**
     * Remove intermediate bridge tiles from a straight, colinear path when the
     * remaining jump distance is within {@code bridgeRange}. This simplifies
     * build plans by avoiding unnecessary short bridges.
     *
     * @param path original list of consecutive bridge tiles
     * @return a compressed sequence with redundant midpoints removed
     */
    protected Seq<Tile> compressPath(Seq<Tile> path) {
        if (path.size < 3)
            return path;
        Seq<Tile> result = new Seq<>();
        result.add(path.first());

        for (int i = 1; i < path.size - 1; i++) {
            Tile last = result.peek();
            Tile curr = path.get(i);
            Tile next = path.get(i + 1);
            if (manhattan(last, next) <= bridgeRange && isColinear(last, curr, next)) {
                continue; // skip middle bridge
            }
            result.add(curr);
        }
        result.add(path.peek());
        return result;
    }

    /**
     * Test whether three tiles lie on a straight line (colinear in the
     * mathematical sense).
     *
     * @param a first tile
     * @param b middle tile
     * @param c last tile
     * @return {@code true} if b lies on the line segment joining a and c
     */
    protected boolean isColinear(Tile a, Tile b, Tile c) {
        return (b.x - a.x) * (c.y - a.y) == (c.x - a.x) * (b.y - a.y);
    }

    /**
     * Convenience check for whether two tiles share either the same x or the
     * same y coordinate (aligned along one of the grid axes).
     *
     * @param a first tile
     * @param b second tile
     * @return {@code true} if the tiles are aligned horizontally or vertically
     */
    protected boolean isAxisAligned(Tile a, Tile b) {
        return a.x == b.x || a.y == b.y;
    }

    protected int countTouchingOccupied(Tile center, Block block, ObjectSet<Tile> occupied) {
        int touching = 0;
        for (Tile tile : getAdjacentTiles(center, block)) {
            if (occupied.contains(tile)) {
                touching++;
            }
        }
        return touching;
    }

    protected int countAdjacentPairs(Seq<Tile> firstTiles, Seq<Tile> secondTiles) {
        int touching = 0;

        for (Tile first : firstTiles) {
            if (first == null)
                continue;
            for (Tile second : secondTiles) {
                if (second == null)
                    continue;
                if (Math.abs(first.x - second.x) + Math.abs(first.y - second.y) == 1) {
                    touching++;
                }
            }
        }

        return touching;
    }

    protected int compareInitialCandidates(Candidate a, Candidate b) {
        int c = Integer.compare(b.drill.size, a.drill.size);
        if (c != 0)
            return c;
        c = Integer.compare(b.score, a.score);
        if (c != 0)
            return c;
        c = Integer.compare(b.oreCount, a.oreCount);
        if (c != 0)
            return c;
        c = Float.compare(b.coverage, a.coverage);
        if (c != 0)
            return c;
        c = compareTilesForPreference(a.tile, b.tile);
        if (c != 0)
            return c;
        return Integer.compare(a.drill.id, b.drill.id);
    }

    protected boolean isBetterCandidate(Candidate candidate, Candidate bestCandidate, int gap, int bestGap,
            int compactness, int bestCompactness, double score, double bestScore) {
        if (candidate.drill.size != bestCandidate.drill.size)
            return candidate.drill.size > bestCandidate.drill.size;
        if (gap != bestGap)
            return gap < bestGap;
        if (compactness != bestCompactness)
            return compactness > bestCompactness;
        if (Double.compare(score, bestScore) != 0)
            return score > bestScore;
        return compareTilesForPreference(candidate.tile, bestCandidate.tile) < 0;
    }

    protected int compareTilesForPreference(Tile a, Tile b) {
        int c = Integer.compare(perpendicularDistanceToOutputAxis(a), perpendicularDistanceToOutputAxis(b));
        if (c != 0)
            return c;
        c = Integer.compare(distanceToOutputRoot(a), distanceToOutputRoot(b));
        if (c != 0)
            return c;
        return Integer.compare(a.pos(), b.pos());
    }

    protected int tilePos(int x, int y) {
        Tile tile = Vars.world.tile(x, y);
        return tile == null ? Integer.MAX_VALUE : tile.pos();
    }

    protected Tile findClusterCenter(Seq<Tile> clusterTiles) {
        if (clusterTiles.isEmpty()) {
            return selectedTile;
        }

        Tile best = clusterTiles.first();
        int bestDistance = Integer.MAX_VALUE;

        for (Tile candidate : clusterTiles) {
            int distance = 0;
            for (Tile other : clusterTiles) {
                distance += manhattan(candidate, other);
            }

            if (distance < bestDistance
                    || (distance == bestDistance && compareTilesByPosition(candidate, best) < 0)) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    protected Tile findOutputAnchorTile(Seq<Tile> clusterTiles, Tile clickedTile) {
        if (clusterTiles.isEmpty()) {
            return clickedTile;
        }

        Tile best = clusterTiles.first();
        int bestProjection = Integer.MIN_VALUE;
        int bestPerpendicular = Integer.MAX_VALUE;
        int bestDistance = Integer.MAX_VALUE;

        for (Tile candidate : clusterTiles) {
            int projection = candidate.x * outDirection.p.x + candidate.y * outDirection.p.y;
            int perpendicular = perpendicularDistanceToAxis(candidate, clickedTile);
            int distance = clickedTile == null ? 0 : manhattan(candidate, clickedTile);

            if (projection > bestProjection
                    || (projection == bestProjection && perpendicular < bestPerpendicular)
                    || (projection == bestProjection && perpendicular == bestPerpendicular && distance < bestDistance)
                    || (projection == bestProjection && perpendicular == bestPerpendicular && distance == bestDistance
                            && compareTilesByPosition(candidate, best) < 0)) {
                bestProjection = projection;
                bestPerpendicular = perpendicular;
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    protected int distanceToOutputRoot(Tile tile) {
        Tile root = outputAnchorTile != null ? outputAnchorTile : selectedTile;
        return root == null || tile == null ? Integer.MAX_VALUE : manhattan(tile, root);
    }

    protected int perpendicularDistanceToOutputAxis(Tile tile) {
        Tile root = outputAnchorTile != null ? outputAnchorTile : selectedTile;
        if (root == null || tile == null) {
            return Integer.MAX_VALUE;
        }

        return perpendicularDistanceToAxis(tile, root);
    }

    protected int perpendicularDistanceToAxis(Tile tile, Tile axisRoot) {
        if (axisRoot == null || tile == null) {
            return Integer.MAX_VALUE;
        }

        switch (outDirection) {
            case RIGHT:
            case LEFT:
                return Math.abs(tile.y - axisRoot.y);
            case UP:
            case DOWN:
            default:
                return Math.abs(tile.x - axisRoot.x);
        }
    }

    protected int compareTilesByPosition(Tile a, Tile b) {
        if (a == b)
            return 0;
        if (a == null)
            return 1;
        if (b == null)
            return -1;
        return Integer.compare(a.pos(), b.pos());
    }

    protected int extractorPlanCompactness(Seq<BuildPlan> extractorPlans, Tile drillCenter, Block drill, ObjectSet<Tile> occupied) {
        if (extractorPlans.isEmpty()) {
            return 0;
        }

        int compactness = 0;
        Seq<Tile> drillHitbox = getHitboxTiles(drillCenter, drill);
        Seq<Seq<Tile>> extractorHitboxes = new Seq<>();

        for (BuildPlan extractorPlan : extractorPlans) {
            Tile extractorTile = Vars.world.tile(extractorPlan.x, extractorPlan.y);
            if (extractorTile == null)
                continue;
            Seq<Tile> extractorHitbox = getHitboxTiles(extractorTile, WATER_EXTRACTOR);
            extractorHitboxes.add(extractorHitbox);
            compactness += countAdjacentPairs(drillHitbox, extractorHitbox);
            compactness += countTouchingOccupied(extractorTile, WATER_EXTRACTOR, occupied);
        }

        for (int i = 0; i < extractorHitboxes.size; i++) {
            for (int j = i + 1; j < extractorHitboxes.size; j++) {
                compactness += countAdjacentPairs(extractorHitboxes.get(i), extractorHitboxes.get(j));
            }
        }

        return compactness;
    }

    protected int routeCompactnessScore(Seq<BuildPlan> route, ObjectSet<Tile> occupied) {
        int compactness = 0;

        for (BuildPlan plan : route) {
            Tile tile = Vars.world.tile(plan.x, plan.y);
            if (tile == null)
                continue;
            compactness += countTouchingOccupied(tile, bridgeBlock, occupied);
        }

        return compactness;
    }

    protected int routeCenterDrift(Seq<BuildPlan> route) {
        int drift = 0;

        for (BuildPlan plan : route) {
            Tile tile = Vars.world.tile(plan.x, plan.y);
            if (tile == null)
                continue;
            drift += distanceToHeuristicCenter(tile);
            drift += perpendicularDistanceToOutputAxis(tile) * 2;
        }

        return drift;
    }

    protected boolean shouldRejectDedicatedRoute(Seq<BuildPlan> route, int gain, int compactness,
            int supportTouch, int routeCompactness, int gap, double marginalScore) {
        if (route.isEmpty()) {
            return false;
        }

        boolean isolatedDrill = compactness == 0 && supportTouch == 0 && gap > 0;
        boolean weakBranch = route.size >= gain + 1 || route.size * 2 > gain * 3;
        boolean looseRoute = routeCompactness <= route.size;

        if (isolatedDrill && weakBranch && looseRoute) {
            return true;
        }

        return marginalScore < 0;
    }

    protected Seq<Tile> getCoveredOreTiles(Tile center, Drill drill) {
        Seq<Tile> tiles = new Seq<>();

        for (Tile tile : center.getLinkedTilesAs(drill, new Seq<>())) {
            if (tile != null && tile.drop() == targetOre) {
                tiles.add(tile);
            }
        }

        tiles.sort(Tile::pos);
        return tiles;
    }

    protected Seq<Candidate> pruneDominatedCandidates(Seq<Candidate> candidates) {
        Seq<Candidate> filtered = new Seq<>();

        for (Candidate candidate : candidates) {
            if (!isDominatedBySmaller(candidate, candidates)) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    protected boolean isDominatedBySmaller(Candidate candidate, Seq<Candidate> candidates) {
        for (Candidate other : candidates) {
            if (other == candidate || other.drill.size >= candidate.drill.size) {
                continue;
            }
            if (other.oreCount < candidate.oreCount) {
                continue;
            }
            if (coversSameOreTiles(other.coveredTiles, candidate.coveredTiles)) {
                return true;
            }
        }

        return false;
    }

    protected boolean coversSameOreTiles(Seq<Tile> source, Seq<Tile> target) {
        if (target.isEmpty()) {
            return true;
        }
        if (source.size < target.size) {
            return false;
        }

        for (Tile tile : target) {
            if (!source.contains(tile)) {
                return false;
            }
        }

        return true;
    }

    protected int distanceFromHitboxToTiles(Tile center, Block block, Iterable<Tile> targets) {
        int best = Integer.MAX_VALUE;

        for (Tile hitboxTile : getHitboxTiles(center, block)) {
            if (hitboxTile == null)
                continue;
            for (Tile target : targets) {
                if (target == null)
                    continue;
                int gap = Math.max(0, manhattan(hitboxTile, target) - 1);
                if (gap < best) {
                    best = gap;
                }
            }
        }

        return best == Integer.MAX_VALUE ? 0 : best;
    }

    protected int boundingAreaGrowth(ObjectSet<Tile> occupied, Tile center, Block block) {
        if (occupied.isEmpty()) {
            return 0;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Tile tile : occupied) {
            if (tile == null)
                continue;
            minX = Math.min(minX, tile.x);
            minY = Math.min(minY, tile.y);
            maxX = Math.max(maxX, tile.x);
            maxY = Math.max(maxY, tile.y);
        }

        int currentArea = (maxX - minX + 1) * (maxY - minY + 1);

        for (Tile tile : getHitboxTiles(center, block)) {
            if (tile == null)
                continue;
            minX = Math.min(minX, tile.x);
            minY = Math.min(minY, tile.y);
            maxX = Math.max(maxX, tile.x);
            maxY = Math.max(maxY, tile.y);
        }

        int expandedArea = (maxX - minX + 1) * (maxY - minY + 1);
        return expandedArea - currentArea;
    }

    /**
     * Helpler to count how many ore are covered by given drill and tile
     * 
     * @param center drill center tile
     * @param drill given drill for counting
     * @param covered already covered array
     * @return amount of ore will get cover
     */
    protected int countUncoveredOre(Tile center, Block drill, ObjectSet<Tile> covered) {
        int count = 0;

        for (Tile t : center.getLinkedTilesAs((Drill) drill, new Seq<>())) {
            if (t != null && t.drop() == targetOre && !covered.contains(t)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Return the smallest distance between given tile and current network node
     * 
     * @param t given tile
     * @param networkNodes
     * @return
     */
    protected int distanceToNetwork(Tile t, ObjectSet<Tile> networkNodes) {
        int best = Integer.MAX_VALUE;

        for (Tile n : networkNodes) {
            int d = Math.abs(t.x - n.x) + Math.abs(t.y - n.y);
            if (d < best)
                best = d;
        }

        return best;
    }

    protected int routePlacementCost(Seq<BuildPlan> route) {
        int cost = 0;

        for (BuildPlan plan : route) {
            cost++;
            if (plan.config instanceof Point2 config) {
                cost += Math.abs(config.x) + Math.abs(config.y);
            }
        }

        return cost;
    }

    /**
     * (Unused) alternative path compression that greedily skips as many tiles
     * as possible while remaining within bridge range and axis-aligned. This
     * may produce shorter sequences but could also remove viable drill
     * positions.
     *
     * @param path original bridge path
     * @return trimmed version of the path
     */
    @SuppressWarnings("unused")
    protected Seq<Tile> aggressiveTrim(Seq<Tile> path) {
        if (path.size < 3)
            return path;

        Seq<Tile> result = new Seq<>();
        int i = 0;

        while (i < path.size) {
            result.add(path.get(i));

            int best = i + 1;

            for (int j = path.size - 1; j > i; j--) {
                if (manhattan(path.get(i), path.get(j)) <= bridgeRange
                        && isAxisAligned(path.get(i), path.get(j))) {
                    best = j;
                    break;
                }
            }

            i = best;
        }

        return result;
    }

    /**
     * Simple data holder representing a potential drill placement in the
     * heuristic search.
     */
    protected class Candidate {
        public Tile tile;
        public Block drill;
        public int score;
        public int oreCount;
        public float coverage;
        public Seq<Tile> coveredTiles;

        public Candidate(Tile t, Block d, int s, int oreCount, float coverage, Seq<Tile> coveredTiles) {
            tile = t;
            drill = d;
            score = s;
            this.oreCount = oreCount;
            this.coverage = coverage;
            this.coveredTiles = coveredTiles;
        }
    }

    /**
     * Simple data holder representing a potential water extractor placement in the
     * heuristic search.
     */
    protected class ExtractorCandidate {
        final int x, y, blockedOre, touching, drillTouch, totalTouch, distanceToDrill;

        ExtractorCandidate(int x, int y, int blockedOre, int touching, int drillTouch, int totalTouch,
                int distanceToDrill) {
            this.x = x;
            this.y = y;
            this.blockedOre = blockedOre;
            this.touching = touching;
            this.drillTouch = drillTouch;
            this.totalTouch = totalTouch;
            this.distanceToDrill = distanceToDrill;
        }
    }

    /**
     * Internal node class used by the A* pathfinding algorithm. Stores the
     * current tile, a link to the parent node, directional and cost values.
     * Implements {@link Comparable} so that nodes can be sorted by estimated
     * total cost.
     */
    protected class AStarNode implements Comparable<AStarNode> {
        public Tile tile;
        public AStarNode parent;
        @SuppressWarnings("unused")
        public int dir, gCost, hCost;

        public AStarNode(Tile t, AStarNode p, int d, int g, int h) {
            tile = t;
            parent = p;
            dir = d;
            gCost = g;
            hCost = h;
        }

        public int fCost() {
            return gCost + hCost;
        }

        @Override
        public int compareTo(AStarNode o) {
            int c = Integer.compare(this.fCost(), o.fCost());
            if (c == 0)
                c = Integer.compare(this.hCost, o.hCost);
            if (c == 0)
                c = Integer.compare(this.gCost, o.gCost);
            if (c == 0)
                c = Integer.compare(compareTilesForPreference(this.tile, o.tile), 0);
            return c;
        }
    }
}