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
    public Seq<BuildPlan> build() {
        Seq<BuildPlan> finalPlans = new Seq<>();
        // All fetched ore tile
        Seq<Tile> oreTiles = Utils.getConnectedTiles(selectedTile, 200);
        Utils.expandArea(oreTiles, maxDrillSize / 4);

        if (oreTiles.isEmpty()) return finalPlans;

        // Move selected tile to outer most ore tile for avoid graph grow into circular
        // path
        // Choose the outer most for the selected direction
        Tile outermostTile = oreTiles.get(0);
        int peekCur = Integer.MIN_VALUE;
        for (Tile t : oreTiles) {
            int distCur = Math.abs(t.x - selectedTile.x) + Math.abs(t.y - selectedTile.y);
            if (distCur > peekCur && Integer.signum(t.x - selectedTile.x) == outDirection.p.x
                    && Integer.signum(t.y - selectedTile.y) == outDirection.p.y) {
                peekCur = distCur;
                outermostTile = t;
            }
        }
        selectedTile = outermostTile;
        // Indicate all tiles that already have something here
        ObjectSet<Tile> globalOccupied = new ObjectSet<>();
        // anchor the network at the selected tile with an item bridge
        ObjectSet<Tile> networkNodes = new ObjectSet<>();
        // The output bridge direction
        Point2 outlet = outDirection.p.cpy();
        // All ore that already got cover
        ObjectSet<Tile> coveredOre = new ObjectSet<>();
        // Create bridge branch out
        int step = Math.min(4, bridgeRange - 2);
        // generate all candidate drill placements
        Seq<Candidate> candidates = new Seq<>();

        for (int i = 0; i < step; i++) outlet.add(outDirection.p);
        allowedDrill.sort((a, b) -> b.size - a.size);
        networkNodes.add(selectedTile);
        globalOccupied.add(selectedTile);
        finalPlans.add(new BuildPlan(selectedTile.x, selectedTile.y, outDirection.r, bridgeBlock, outlet));
        finalPlans.add(new BuildPlan(selectedTile.x + outlet.x, selectedTile.y + outlet.y, outDirection.r, bridgeBlock));

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

        for (Tile t : oreTiles) {
            for (Block drillMode : allowedDrill) {
                if (forceSelectedSize && drillMode != selectedDrill) continue;
                if (!(drillMode instanceof Drill drill)) continue;
                if (!Build.validPlace(drill, Vars.player.team(), t.x, t.y, 0)) continue;

                ObjectIntMap.Entry<Item> ores = Utils.countOre(t, drill);
                Seq<Tile> hitbox = getHitboxTiles(t, drillMode);

                boolean fits = true;
                for (Tile ht : hitbox) {
                    if (ht == null || ht.drop() != targetOre) {
                        fits = false;
                        break;
                    }
                }

                if (!fits) continue;

                if (ores != null && ores.value > 0) {
                    if (ores.key != targetOre) {
                        // Not a request ore at all
                        // Not add to graph at all (even heuristic score is 0, it still can be pick if
                        // this is the only option exist)
                        // candidates.add(new Candidate(t, drill, Integer.MIN_VALUE));
                        continue;
                    }
                    if (ores.value * 1f / (drillMode.size * drillMode.size) < minimumCoverage) {
                        // It not really worth if you place laser drill (cover 16 tiles) just for mine 1
                        // tiles
                        continue;
                    }
                    int distToOut = Math.abs(t.x - selectedTile.x) + Math.abs(t.y - selectedTile.y);
                    // Weighted scoring: ore count, drill efficiency, proximity to outlet
                    int oreWeight = ores.value * 20;
                    int sizeWeight = drill.size * 12;
                    int distanceWeight = Math.max(0, 100 - distToOut * 5);
                    int score = oreWeight + sizeWeight + distanceWeight;
                    candidates.add(new Candidate(t, drill, score));
                }
            }
        }
        candidates.sort((a, b) -> b.score - a.score);

        while (true) {
            Candidate best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Candidate c : candidates) {
                if (isHitboxOccupied(c.tile, c.drill, globalOccupied)) continue;
            
                int gain = countUncoveredOre(c.tile, c.drill, coveredOre);
                if (gain <= 0) continue;
            
                int dist = distanceToNetwork(c.tile, networkNodes);
                double score = gain * 15.0 - dist * 2.0;

                if (score > bestScore) {
                    bestScore = score;
                    best = c;
                }
            }
        
            if (best == null) break;
            Seq<BuildPlan> route = aStarBridgeRoute(best.tile, best.drill, networkNodes, globalOccupied);
        
            if (route == null) {
                candidates.remove(best);
                continue;
            }
        
            finalPlans.add(new BuildPlan(best.tile.x, best.tile.y, 0, best.drill));

            if (best.drill.size > 2 && useWaterExtractor) {
                Seq<BuildPlan> extractorPlans = findBestWaterExtractor(best.tile, best.drill, globalOccupied);
                if (extractorPlans == null) continue;
                finalPlans.addAll(extractorPlans);
            }
    
            for (Tile t : best.tile.getLinkedTilesAs((Drill) best.drill, new Seq<>())) {
                if (t != null && t.drop() == targetOre) {
                    coveredOre.add(t);
                }
            }
        
            // Lock hitbox
            lockHitbox(best.tile, best.drill, globalOccupied);
            finalPlans.addAll(route);
        
            for (BuildPlan bp : route) {
                Tile rTile = Vars.world.tile(bp.x, bp.y);
                if (rTile != null) {
                    globalOccupied.add(rTile);
                    networkNodes.add(rTile);
                }
            }
        
            // Add drill tile to network so growth expands outward
            networkNodes.add(best.tile);        
            candidates.remove(best);
        }

        return finalPlans;
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
        for (Tile dt : drillTiles) {
            for (Tile et : exTiles) {
                if (Math.abs(dt.x - et.x) + Math.abs(dt.y - et.y) == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Search the neighbourhood around a large drill for a valid water
     * extractor placement that maximises nearby target ore coverage and does
     * not overlap occupied tiles.
     *
     * @param drillCenter centre tile of the drill
     * @param drill       drill block being placed (used for size and range)
     * @param occupied    set of tiles already reserved by other structures
     * @return a sequence containing a single {@link BuildPlan} for the chosen
     *         extractor, or {@code null} if no suitable location exists. The
     *         returned extractor will be locked into the occupied set by the
     *         caller.
     */
    protected Seq<BuildPlan> findBestWaterExtractor(Tile drillCenter, Block drill, ObjectSet<Tile> occupied) {
        if (drill.size <= 2)
            return null;

        Seq<ExtractorCandidate> options = new Seq<>();
        int range = drill.size + 4;

        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                int ex = drillCenter.x + dx;
                int ey = drillCenter.y + dy;
                Tile exCenter = Vars.world.tile(ex, ey);
                if (exCenter == null)
                    continue;

                Seq<Tile> drillHitbox = getHitboxTiles(drillCenter, drill);
                Seq<Tile> exHitbox = getHitboxTiles(exCenter, WATER_EXTRACTOR);

                boolean overlap = false;
                for (Tile t : exHitbox) {
                    if (t == null || occupied.contains(t) || drillHitbox.contains(t)) {
                        overlap = true;
                        break;
                    }
                }
                if (overlap)
                    continue;

                if (!Build.validPlace(WATER_EXTRACTOR, Vars.player.team(), ex, ey, 0))
                    continue;

                if (isAdjacentToDrill(drillHitbox, exHitbox)) {
                    int ores = countOreInArea(ex, ey, 2);
                    options.add(new ExtractorCandidate(ex, ey, ores));
                }
            }
        }

        if (options.isEmpty())
            return null;

        options.sort((a, b) -> {
            int c = Integer.compare(a.oreCovered, b.oreCovered);
            if (c == 0) {
                int distA = Math.abs(a.x - drillCenter.x) + Math.abs(a.y - drillCenter.y);
                int distB = Math.abs(b.x - drillCenter.x) + Math.abs(b.y - drillCenter.y);
                c = Integer.compare(distA, distB);
            }
            return c;
        });

        ExtractorCandidate best = options.first();
        lockHitbox(Vars.world.tile(best.x, best.y), WATER_EXTRACTOR, occupied);

        return Seq.with(new BuildPlan(best.x, best.y, 0, WATER_EXTRACTOR));
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

        // Already touching network?
        for (Tile adj : startEdges) {
            if (networkNodes.contains(adj))
                return new Seq<>();
        }

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        ObjectSet<Tile> closedSet = new ObjectSet<>();

        // Seed from drill edges
        for (Tile adj : startEdges) {
            if (!isValidBridge(adj, occupied))
                continue;

            int h = nearestNetworkDistance(adj, networkNodes);
            openSet.add(new AStarNode(adj, null, -1, 0, h));
        }

        AStarNode goal = null;

        while (!openSet.isEmpty()) {
            AStarNode curr = openSet.poll();

            if (closedSet.contains(curr.tile))
                continue;

            closedSet.add(curr.tile);

            // Stop when reaching ANY network node
            if (networkNodes.contains(curr.tile)) {
                goal = curr;
                break;
            }

            for (Direction d : Direction.values()) {
                for (int len = bridgeRange; len >= 1; len--) {
                    Tile next = curr.tile.nearby(d.p.x * len, d.p.y * len);
                    if (next == null || closedSet.contains(next))
                        continue;

                    if (!isLineClear(curr.tile, next, occupied))
                        continue;

                    if (!networkNodes.contains(next) && !isValidBridge(next, occupied))
                        continue;

                    // Distance-based cost
                    int turnPenalty = 0;
                    if (curr.parent != null && curr.dir != -1 && curr.dir != d.ordinal()) {
                        turnPenalty = 2; // tune if needed
                    }

                    int newG = curr.gCost + len + turnPenalty;
                    int newH = nearestNetworkDistance(next, networkNodes);

                    openSet.add(new AStarNode(next, curr, d.ordinal(), newG, newH));

                    if (networkNodes.contains(next))
                        break;
                }
            }
        }

        if (goal == null)
            return null;

        // Reconstruct path
        Seq<Tile> path = new Seq<>();
        for (AStarNode n = goal; n != null; n = n.parent) {
            path.add(n.tile);
        }
        path.reverse();

        path = compressPath(path);

        Seq<BuildPlan> plans = new Seq<>();
        for (int i = 0; i < path.size - 1; i++) {
            Tile from = path.get(i);
            Tile to = path.get(i + 1);
            Point2 config = new Point2(to.x - from.x, to.y - from.y);
            plans.add(new BuildPlan(from.x, from.y, 0, bridgeBlock, config));
        }

        return plans;
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

        public Candidate(Tile t, Block d, int s) {
            tile = t;
            drill = d;
            score = s;
        }
    }

    /**
     * Simple data holder representing a potential water extractor placement in the
     * heuristic search.
     */
    protected class ExtractorCandidate {
        final int x, y, oreCovered;

        ExtractorCandidate(int x, int y, int oreCovered) {
            this.x = x;
            this.y = y;
            this.oreCovered = oreCovered;
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
            return c;
        }
    }
}