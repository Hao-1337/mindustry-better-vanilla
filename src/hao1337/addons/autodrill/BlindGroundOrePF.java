package hao1337.addons.autodrill;

import arc.math.geom.Point2;
import arc.struct.ObjectMap;
import arc.struct.ObjectIntMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.production.Drill;

import hao1337.addons.autodrill.HeuristicGroundOrePF.*;

/**
 * A simplified pathfinder that places drills and bridges in a fixed grid
 * pattern over a connected ore cluster, without performing an explicit
 * bridge-route search.  It is intended for "blind" deployments when the
 * player simply wants a regular spacing scheme rather than a shortest-path
 * network.
 *
 * <p>The algorithm picks tiles that match a precomputed 6×6 drill pattern and
 * ensures that each drill has an adjacent valid bridge tile.  Bridges are
 * additionally extended toward the outermost ore tile in the output direction
 * to provide a connection point.</p>
 * 
 * @see Ground
 */
public class BlindGroundOrePF extends GroundOrePathFinding {
    private static final int GRID_PERIOD = 6;
    private static final int BRIDGE_SPACING = 3;

    /**
     * Construct a new blind ore pathfinder.
     *
     * @param drill           drill block type to place
     * @param startTile       initial tile clicked by the user (seed for cluster)
     * @param outputDirection direction where the output bridge should point
     * @param bridgeType      optional specific bridge block (nullable for
     *                        default behaviour)
     */
    public BlindGroundOrePF(Block drill, Tile startTile, Direction outputDirection, @Nullable ItemBridge bridgeType) { super(drill, startTile, outputDirection, bridgeType); }

    /**
     * Generate build plans using the blind placement.
     * @return sequence of {@link BuildPlan} objects describing the structures to
     *         place; may be empty if no valid layout could be generated.
     */
    public Seq<BuildPlan> build() {
        Seq<BuildPlan> finalPlans = new Seq<>();
        Seq<Tile> oreTiles = Utils.getConnectedTiles(selectedTile, maxTiles);
        Utils.expandArea(oreTiles, 1);
        if (oreTiles.isEmpty())
            return finalPlans;

        Point2 direction = new Point2(outDirection.p.x * BRIDGE_SPACING, outDirection.p.y * BRIDGE_SPACING);
        LayoutCandidate bestLayout = null;

        for (int offsetX = 0; offsetX < GRID_PERIOD; offsetX++) {
            for (int offsetY = 0; offsetY < GRID_PERIOD; offsetY++) {
                LayoutCandidate layout = buildLayoutCandidate(oreTiles, direction, offsetX, offsetY);
                if (layout == null)
                    continue;
                if (bestLayout == null || isBetterLayout(layout, bestLayout)) {
                    bestLayout = layout;
                }
            }
        }

        if (bestLayout == null)
            return finalPlans;

        Seq<Tile> bridgeTiles = new Seq<>();
        Tile outputTile = bestLayout.outputTile;
        Tile outputAnchor = bestLayout.outputAnchor;
        bridgeTiles.addAll(bestLayout.bridgeTiles);
        ObjectMap<Tile, Tile> bridgeParents = buildBridgeParents(bridgeTiles, outputAnchor);
        bridgeTiles.sort((a, b) -> Integer.compare(distanceToOutputRoot(b, outputAnchor), distanceToOutputRoot(a, outputAnchor)));

        for (Tile drillTile : bestLayout.drillTiles) {
            finalPlans.add(new BuildPlan(drillTile.x, drillTile.y, 0, selectedDrill));
        }

        for (Tile bridgeTile : bridgeTiles) {
            Tile neighbor = bridgeTile == outputAnchor ? outputTile : bridgeParents.get(bridgeTile);

            Point2 config = new Point2();
            if (neighbor != null)
                config = new Point2(neighbor.x - bridgeTile.x, neighbor.y - bridgeTile.y);

            finalPlans.add(new BuildPlan(bridgeTile.x, bridgeTile.y, 0, bridgeBlock, config));
        }

        if (!bridgeTiles.contains(outputTile)) {
            finalPlans.add(new BuildPlan(outputTile.x, outputTile.y, 0, bridgeBlock));
        }

        return finalPlans;
    }

    protected LayoutCandidate buildLayoutCandidate(Seq<Tile> oreTiles, Point2 direction, int offsetX, int offsetY) {
        Seq<Tile> drillTiles = oreTiles.select(t -> isDrillTile(t, offsetX, offsetY));
        Seq<Tile> bridgeTiles = oreTiles.select(t -> isBridgeTile(t, offsetX, offsetY));
        Seq<Tile> validDrillTiles = new Seq<>();
        int area = selectedDrill.size * selectedDrill.size;

        for (Tile drillTile : drillTiles) {
            ObjectIntMap.Entry<Item> itemAndCount = Utils.countOre(drillTile, (Drill) selectedDrill);
            float coverage = itemAndCount == null ? 0f : itemAndCount.value / (float) area;

            if (itemAndCount == null || itemAndCount.key != selectedTile.drop() || coverage < minimumCoverage)
                continue;

            Seq<Tile> neighbors = Utils.getNearbyTiles(drillTile.x, drillTile.y, selectedDrill.size);
            neighbors.retainAll(tile -> isBridgeTile(tile, offsetX, offsetY));

            boolean hasBridgeNeighbor = false;
            for (Tile neighbor : neighbors) {
                if (bridgeTiles.contains(neighbor)) {
                    hasBridgeNeighbor = true;
                    break;
                }
            }

            if (!hasBridgeNeighbor) {
                Tile bridgeNeighbor = findPreferredBridgeTile(neighbors);
                if (bridgeNeighbor == null)
                    continue;
                if (!bridgeTiles.contains(bridgeNeighbor)) {
                    bridgeTiles.add(bridgeNeighbor);
                }
            }

            validDrillTiles.add(drillTile);
        }

        if (validDrillTiles.isEmpty() || bridgeTiles.isEmpty())
            return null;

        Tile outputTile = findOutputTile(bridgeTiles, direction);
        if (outputTile == null)
            return null;

        Tile outputAnchor = outputTile.nearby(-direction.x, -direction.y);
        Seq<Tile> routedBridgeTiles = trimUnsupportedBridgeBranches(validDrillTiles, bridgeTiles, outputAnchor);
        Seq<Tile> prunedBridgeTiles = pruneRedundantBridges(validDrillTiles, routedBridgeTiles, outputAnchor);
        prunedBridgeTiles = trimUnsupportedBridgeBranches(validDrillTiles, prunedBridgeTiles, outputAnchor);
        if (prunedBridgeTiles.isEmpty() || !prunedBridgeTiles.contains(outputAnchor))
            return null;

        int minedOre = countCoveredOre(validDrillTiles);
        int blockedOre = countBlockedOre(prunedBridgeTiles, outputTile);
        int finalAddedBridgeCount = countAddedBridgeTiles(prunedBridgeTiles, oreTiles);
        return new LayoutCandidate(validDrillTiles, prunedBridgeTiles, outputTile, minedOre, blockedOre,
            finalAddedBridgeCount, outputAnchor);
    }

    protected Seq<Tile> trimUnsupportedBridgeBranches(Seq<Tile> drillTiles, Seq<Tile> bridgeTiles, Tile outputAnchor) {
        Seq<Tile> trimmedBridgeTiles = new Seq<>();
        trimmedBridgeTiles.addAll(bridgeTiles);

        if (outputAnchor == null || !trimmedBridgeTiles.contains(outputAnchor)) {
            return trimmedBridgeTiles;
        }

        boolean removed;
        do {
            removed = false;
            ObjectMap<Tile, Tile> bridgeParents = buildBridgeParents(trimmedBridgeTiles, outputAnchor);
            ObjectSet<Tile> requiredBridges = new ObjectSet<>();
            requiredBridges.add(outputAnchor);

            for (Tile bridgeTile : trimmedBridgeTiles) {
                if (bridgeTile == null || bridgeTile == outputAnchor) {
                    continue;
                }
                if (!canBridgeReceiveFromAnyDrill(bridgeTile, bridgeParents.get(bridgeTile), drillTiles)) {
                    continue;
                }

                Tile current = bridgeTile;
                while (current != null && !requiredBridges.contains(current)) {
                    requiredBridges.add(current);
                    current = bridgeParents.get(current);
                }
            }

            if (requiredBridges.size < trimmedBridgeTiles.size) {
                trimmedBridgeTiles.retainAll(requiredBridges::contains);
                removed = true;
            }
        } while (removed);

        return trimmedBridgeTiles;
    }

    protected Seq<Tile> pruneRedundantBridges(Seq<Tile> drillTiles, Seq<Tile> bridgeTiles, Tile outputAnchor) {
        Seq<Tile> prunedBridgeTiles = new Seq<>();
        prunedBridgeTiles.addAll(bridgeTiles);

        if (outputAnchor == null || !prunedBridgeTiles.contains(outputAnchor)) {
            return prunedBridgeTiles;
        }

        boolean removed;
        do {
            removed = false;
            Seq<Tile> candidates = new Seq<>();
            candidates.addAll(prunedBridgeTiles);
            Seq<Tile> currentBridgeTiles = new Seq<>();
            currentBridgeTiles.addAll(prunedBridgeTiles);
            ObjectMap<Tile, Tile> bridgeParents = buildBridgeParents(currentBridgeTiles, outputAnchor);
            candidates.sort((a, b) -> compareBridgeRemovalPreference(a, b, currentBridgeTiles, bridgeParents, drillTiles));

            for (Tile bridgeTile : candidates) {
                if (!isBridgeRemovable(bridgeTile, drillTiles, prunedBridgeTiles, outputAnchor, bridgeParents))
                    continue;

                prunedBridgeTiles.remove(bridgeTile);
                removed = true;
                break;
            }
        } while (removed);

        return prunedBridgeTiles;
    }

    protected boolean isBridgeRemovable(Tile bridgeTile, Seq<Tile> drillTiles, Seq<Tile> bridgeTiles, Tile outputAnchor,
            ObjectMap<Tile, Tile> bridgeParents) {
        if (bridgeTile == null || bridgeTile == outputAnchor) {
            return false;
        }

        int connections = countBridgeConnections(bridgeTile, bridgeTiles);
        if (connections > 1) {
            return false;
        }

        Seq<Tile> remainingBridges = new Seq<>();
        remainingBridges.addAll(bridgeTiles);
        remainingBridges.remove(bridgeTile);
        if (!isBridgeNetworkConnected(remainingBridges, outputAnchor)) {
            return false;
        }

        ObjectMap<Tile, Tile> remainingBridgeParents = buildBridgeParents(remainingBridges, outputAnchor);
        Tile outgoingTarget = bridgeParents.get(bridgeTile);

        for (Tile drillTile : drillTiles) {
            if (!canBridgeReceiveFromDrill(bridgeTile, drillTile, outgoingTarget)) {
                continue;
            }

            boolean hasAlternateBridge = false;
            for (Tile otherBridge : remainingBridges) {
                if (canBridgeReceiveFromDrill(otherBridge, drillTile, remainingBridgeParents.get(otherBridge))) {
                    hasAlternateBridge = true;
                    break;
                }
            }

            if (!hasAlternateBridge) {
                return false;
            }
        }

        return true;
    }

    protected int compareBridgeRemovalPreference(Tile candidate, Tile best, Seq<Tile> bridgeTiles,
            ObjectMap<Tile, Tile> bridgeParents, Seq<Tile> drillTiles) {
        int c = Boolean.compare(canBridgeReceiveFromAnyDrill(best, bridgeParents.get(best), drillTiles),
                canBridgeReceiveFromAnyDrill(candidate, bridgeParents.get(candidate), drillTiles));
        if (c != 0)
            return c;

        c = Boolean.compare(best.drop() == targetOre, candidate.drop() == targetOre);
        if (c != 0)
            return c;

        int candidateConnections = countBridgeConnections(candidate, bridgeTiles);
        int bestConnections = countBridgeConnections(best, bridgeTiles);
        c = Integer.compare(candidateConnections, bestConnections);
        if (c != 0)
            return c;

        int candidateDistance = Math.abs(candidate.x - selectedTile.x) + Math.abs(candidate.y - selectedTile.y);
        int bestDistance = Math.abs(best.x - selectedTile.x) + Math.abs(best.y - selectedTile.y);
        c = Integer.compare(bestDistance, candidateDistance);
        if (c != 0)
            return c;

        return Integer.compare(best.pos(), candidate.pos());
    }

    protected boolean canBridgeReceiveFromAnyDrill(Tile bridgeTile, Tile outgoingTarget, Seq<Tile> drillTiles) {
        for (Tile drillTile : drillTiles) {
            if (canBridgeReceiveFromDrill(bridgeTile, drillTile, outgoingTarget)) {
                return true;
            }
        }
        return false;
    }

    protected int countAddedBridgeTiles(Seq<Tile> bridgeTiles, Seq<Tile> oreTiles) {
        int count = 0;

        for (Tile bridgeTile : bridgeTiles) {
            if (bridgeTile != null && !oreTiles.contains(bridgeTile)) {
                count++;
            }
        }

        return count;
    }

    protected boolean isBridgeNetworkConnected(Seq<Tile> bridgeTiles, Tile outputAnchor) {
        if (bridgeTiles.isEmpty()) {
            return false;
        }
        if (outputAnchor == null || !bridgeTiles.contains(outputAnchor)) {
            return false;
        }

        ObjectSet<Tile> visited = new ObjectSet<>();
        Queue<Tile> queue = new Queue<>();
        queue.addLast(outputAnchor);
        visited.add(outputAnchor);

        while (!queue.isEmpty()) {
            Tile current = queue.removeFirst();

            for (Tile bridgeTile : bridgeTiles) {
                if (bridgeTile == null || visited.contains(bridgeTile) || !isBridgeConnected(current, bridgeTile)) {
                    continue;
                }

                visited.add(bridgeTile);
                queue.addLast(bridgeTile);
            }
        }

        return visited.size == bridgeTiles.size;
    }

    protected ObjectMap<Tile, Tile> buildBridgeParents(Seq<Tile> bridgeTiles, Tile outputAnchor) {
        ObjectMap<Tile, Tile> parents = new ObjectMap<>();
        if (outputAnchor == null || !bridgeTiles.contains(outputAnchor)) {
            return parents;
        }

        ObjectSet<Tile> visited = new ObjectSet<>();
        Queue<Tile> queue = new Queue<>();
        queue.addLast(outputAnchor);
        visited.add(outputAnchor);

        while (!queue.isEmpty()) {
            Tile current = queue.removeFirst();

            for (Tile bridgeTile : bridgeTiles) {
                if (bridgeTile == null || bridgeTile == current || visited.contains(bridgeTile)
                        || !isBridgeConnected(current, bridgeTile)) {
                    continue;
                }

                parents.put(bridgeTile, current);
                visited.add(bridgeTile);
                queue.addLast(bridgeTile);
            }
        }

        return parents;
    }

    protected int distanceToOutputRoot(Tile tile, Tile outputAnchor) {
        if (tile == null || outputAnchor == null) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(tile.x - outputAnchor.x) + Math.abs(tile.y - outputAnchor.y);
    }

    protected boolean touchesDrill(Tile bridgeTile, Tile drillTile) {
        if (bridgeTile == null || drillTile == null) {
            return false;
        }

        return Utils.getNearbyTiles(drillTile.x, drillTile.y, selectedDrill.size).contains(bridgeTile);
    }

    protected int countBridgeConnections(Tile bridgeTile, Seq<Tile> bridgeTiles) {
        if (bridgeTiles == null) {
            return 0;
        }

        int count = 0;
        for (Tile otherBridge : bridgeTiles) {
            if (otherBridge == null || otherBridge == bridgeTile) {
                continue;
            }
            if (isBridgeConnected(bridgeTile, otherBridge)) {
                count++;
            }
        }
        return count;
    }

    protected boolean isBridgeConnected(Tile first, Tile second) {
        if (first == null || second == null) {
            return false;
        }

        int dx = Math.abs(first.x - second.x);
        int dy = Math.abs(first.y - second.y);
        return (dx == BRIDGE_SPACING && dy == 0) || (dy == BRIDGE_SPACING && dx == 0);
    }

    protected boolean canBridgeReceiveFromDrill(Tile bridgeTile, Tile drillTile, Tile outgoingTarget) {
        if (bridgeTile == null || drillTile == null || outgoingTarget == null) {
            return false;
        }

        Point2 blockedSide = new Point2(Integer.signum(outgoingTarget.x - bridgeTile.x), Integer.signum(outgoingTarget.y - bridgeTile.y));
        Tile blockedInputTile = bridgeTile.nearby(blockedSide.x, blockedSide.y);
        for (Tile hitboxTile : getHitboxTiles(drillTile, selectedDrill)) {
            if (hitboxTile == null || Math.abs(hitboxTile.x - bridgeTile.x) + Math.abs(hitboxTile.y - bridgeTile.y) != 1) {
                continue;
            }

            if (hitboxTile != blockedInputTile) {
                return true;
            }
        }

        return false;
    }

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

    protected Tile findPreferredBridgeTile(Seq<Tile> neighbors) {
        Tile best = null;

        for (Tile neighbor : neighbors) {
            if (neighbor == null)
                continue;

            BuildPlan buildPlan = new BuildPlan(neighbor.x, neighbor.y, 0, bridgeBlock);
            if (!buildPlan.placeable(Vars.player.team()))
                continue;

            if (best == null || compareBridgePreference(neighbor, best) < 0) {
                best = neighbor;
            }
        }

        return best;
    }

    protected int compareBridgePreference(Tile candidate, Tile best) {
        int c = Boolean.compare(candidate.drop() == targetOre, best.drop() == targetOre);
        if (c != 0)
            return c;

        int candidateAxis = perpendicularDistanceToOutputAxis(candidate);
        int bestAxis = perpendicularDistanceToOutputAxis(best);
        c = Integer.compare(candidateAxis, bestAxis);
        if (c != 0)
            return c;

        int candidateDistance = Math.abs(candidate.x - selectedTile.x) + Math.abs(candidate.y - selectedTile.y);
        int bestDistance = Math.abs(best.x - selectedTile.x) + Math.abs(best.y - selectedTile.y);
        c = Integer.compare(candidateDistance, bestDistance);
        if (c != 0)
            return c;

        return Integer.compare(candidate.pos(), best.pos());
    }

    protected Tile findOutputTile(Seq<Tile> bridgeTiles, Point2 direction) {
        Tile bestOutput = null;
        Tile bestAnchor = null;

        for (Tile bridgeTile : bridgeTiles) {
            Tile outputTile = bridgeTile.nearby(direction);
            if (outputTile == null)
                continue;

            if (!bridgeTiles.contains(outputTile)) {
                BuildPlan buildPlan = new BuildPlan(outputTile.x, outputTile.y, 0, bridgeBlock);
                if (!buildPlan.placeable(Vars.player.team()))
                    continue;
            }

            if (bestAnchor == null || compareOutputAnchor(bridgeTile, bestAnchor) < 0) {
                bestAnchor = bridgeTile;
                bestOutput = outputTile;
            }
        }

        return bestOutput;
    }

    protected int compareOutputAnchor(Tile candidate, Tile best) {
        int candidateProjection = candidate.x * outDirection.p.x + candidate.y * outDirection.p.y;
        int bestProjection = best.x * outDirection.p.x + best.y * outDirection.p.y;
        int c = Integer.compare(bestProjection, candidateProjection);
        if (c != 0)
            return c;

        int candidateAxis = perpendicularDistanceToOutputAxis(candidate);
        int bestAxis = perpendicularDistanceToOutputAxis(best);
        c = Integer.compare(candidateAxis, bestAxis);
        if (c != 0)
            return c;

        return Integer.compare(candidate.pos(), best.pos());
    }

    protected int countCoveredOre(Seq<Tile> drillTiles) {
        ObjectSet<Tile> covered = new ObjectSet<>();

        for (Tile drillTile : drillTiles) {
            for (Tile tile : drillTile.getLinkedTilesAs((Drill) selectedDrill, new Seq<>())) {
                if (tile != null && tile.drop() == targetOre) {
                    covered.add(tile);
                }
            }
        }

        return covered.size;
    }

    protected int countBlockedOre(Seq<Tile> bridgeTiles, Tile outputTile) {
        ObjectSet<Tile> allBridgeTiles = new ObjectSet<>();
        for (Tile bridgeTile : bridgeTiles) {
            if (bridgeTile != null) {
                allBridgeTiles.add(bridgeTile);
            }
        }
        if (outputTile != null) {
            allBridgeTiles.add(outputTile);
        }

        int blockedOre = 0;
        for (Tile bridgeTile : allBridgeTiles) {
            if (bridgeTile != null && bridgeTile.drop() == targetOre) {
                blockedOre++;
            }
        }

        return blockedOre;
    }

    protected boolean isBetterLayout(LayoutCandidate candidate, LayoutCandidate best) {
        int c = Integer.compare(candidate.netOre(), best.netOre());
        if (c != 0)
            return c > 0;

        c = Integer.compare(best.blockedOre, candidate.blockedOre);
        if (c != 0)
            return c > 0;

        c = Integer.compare(candidate.minedOre, best.minedOre);
        if (c != 0)
            return c > 0;

        c = Integer.compare(candidate.drillTiles.size, best.drillTiles.size);
        if (c != 0)
            return c > 0;

        c = Integer.compare(best.addedBridgeCount, candidate.addedBridgeCount);
        if (c != 0)
            return c > 0;

        c = Integer.compare(best.totalBridgeCount(), candidate.totalBridgeCount());
        if (c != 0)
            return c > 0;

        return compareOutputAnchor(candidate.outputTile, best.outputTile) < 0;
    }

    protected int perpendicularDistanceToOutputAxis(Tile tile) {
        if (tile == null || selectedTile == null) {
            return Integer.MAX_VALUE;
        }

        switch (outDirection) {
            case RIGHT:
            case LEFT:
                return Math.abs(tile.y - selectedTile.y);
            case UP:
            case DOWN:
            default:
                return Math.abs(tile.x - selectedTile.x);
        }
    }

    protected void addUnique(Seq<Tile> tiles, Tile tile) {
        if (tile != null && !tiles.contains(tile)) {
            tiles.add(tile);
        }
    }

    /**
     * Predicate used by the blind algorithm to test whether a tile fits the
     * hard-coded 6×6 pattern for drill placement.  The origin of the pattern is
     * implicitly at (0,0) in world coordinates.
     *
     * @param tile tile to test
     * @return {@code true} if a drill should be placed on this tile according to
     *         the pattern
     */
    private static boolean isDrillTile(Tile tile, int offsetX, int offsetY) {
        int x = Math.floorMod(tile.x - offsetX, GRID_PERIOD);
        int y = Math.floorMod(tile.y - offsetY, GRID_PERIOD);

        switch (x) {
            case 0:
            case 2:
                if (y == 1) return true;
                break;
            case 1:
                if (y == 3 || y == 5) return true;
                break;
            case 3:
            case 5:
                if (y == 4) return true;
                break;
            case 4:
                if (y == 0 || y == 2) return true;
                break;
        }

        return false;
    }

    /**
     * Simple 3×3 grid test for candidate bridge placement.  Blind mode only
     * considers tiles whose coordinates are multiples of three.
     *
     * @param tile tile to test
     * @return {@code true} if a bridge may be placed here
     */
    private static boolean isBridgeTile(Tile tile, int offsetX, int offsetY) {
        int x = Math.floorMod(tile.x - offsetX, BRIDGE_SPACING);
        int y = Math.floorMod(tile.y - offsetY, BRIDGE_SPACING);

        return x == 0 && y == 0;
    }

    protected static class LayoutCandidate {
        final Seq<Tile> drillTiles;
        final Seq<Tile> bridgeTiles;
        final Tile outputTile;
        final Tile outputAnchor;
        final int minedOre;
        final int blockedOre;
        final int addedBridgeCount;

        LayoutCandidate(Seq<Tile> drillTiles, Seq<Tile> bridgeTiles, Tile outputTile, int minedOre, int blockedOre,
                int addedBridgeCount, Tile outputAnchor) {
            this.drillTiles = drillTiles;
            this.bridgeTiles = bridgeTiles;
            this.outputTile = outputTile;
            this.minedOre = minedOre;
            this.blockedOre = blockedOre;
            this.addedBridgeCount = addedBridgeCount;
            this.outputAnchor = outputAnchor;
        }

        int netOre() {
            return minedOre - blockedOre;
        }

        int totalBridgeCount() {
            return bridgeTiles.size + (bridgeTiles.contains(outputTile) ? 0 : 1);
        }
    }
}