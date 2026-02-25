package hao1337.addons.autodrill;

import arc.math.geom.Point2;
import arc.struct.ObjectIntMap;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Blocks;
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
        Seq<Tile> oreTiles = Utils.getConnectedTiles(selectedTile, 200);
        Utils.expandArea(oreTiles, 1);
        if (oreTiles.isEmpty())
            return finalPlans;

        Point2 direction = new Point2(outDirection.p.x * 3, outDirection.p.y * 3);
        Seq<Tile> drillTiles = oreTiles.select(BlindGroundOrePF::isDrillTile);
        Seq<Tile> bridgeTiles = oreTiles.select(BlindGroundOrePF::isBridgeTile);
        int area = selectedDrill.size * selectedDrill.size;

        drillTiles.retainAll(t -> {
            ObjectIntMap.Entry<Item> itemAndCount = Utils.countOre(t, (Drill)selectedDrill);

            if (itemAndCount == null || itemAndCount.key != selectedTile.drop() || itemAndCount.value / area < minimumCoverage)
                return false;

            Seq<Tile> neighbors = Utils.getNearbyTiles(t.x, t.y, selectedDrill.size);
            neighbors.retainAll(BlindGroundOrePF::isBridgeTile);

            for (Tile neighbor : neighbors)
                if (bridgeTiles.contains(neighbor)) return true;

            neighbors.retainAll(n -> {
                BuildPlan buildPlan = new BuildPlan(n.x, n.y, 0, Blocks.itemBridge);
                return buildPlan.placeable(Vars.player.team());
            });

            if (!neighbors.isEmpty()) {
                bridgeTiles.add(neighbors);
                return true;
            }

            return false;
        });

        Tile outerMost = bridgeTiles.max((t) -> outDirection.p.x == 0 ? t.y * outDirection.p.y : t.x * outDirection.p.x);
        if (outerMost == null) return new Seq<>();

        Tile outputTile = outerMost.nearby(direction);
        bridgeTiles.add(outputTile);

        bridgeTiles.sort(t -> t.dst2(outputTile.worldx(), outputTile.worldy()));

        for (Tile drillTile : drillTiles) finalPlans.add(new BuildPlan(drillTile.x, drillTile.y, 0, selectedDrill));

        for (Tile bridgeTile : bridgeTiles) {
            Tile neighbor = bridgeTiles.find(t -> Math.abs(t.x - bridgeTile.x) + Math.abs(t.y - bridgeTile.y) == 3);

            Point2 config = new Point2();
            if (bridgeTile != outputTile && neighbor != null)
                config = new Point2(neighbor.x - bridgeTile.x, neighbor.y - bridgeTile.y);

            finalPlans.add(new BuildPlan(bridgeTile.x, bridgeTile.y, 0, Blocks.itemBridge, config));
        }

        return finalPlans;
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
    private static boolean isDrillTile(Tile tile) {
        short x = tile.x;
        short y = tile.y;

        switch (x % 6) {
            case 0:
            case 2:
                if ((y - 1) % 6 == 0) return true;
                break;
            case 1:
                if ((y - 3) % 6 == 0 || (y - 3) % 6 == 2) return true;
                break;
            case 3:
            case 5:
                if ((y - 4) % 6 == 0) return true;
                break;
            case 4:
                if ((y) % 6 == 0 || (y) % 6 == 2) return true;
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
    private static boolean isBridgeTile(Tile tile) {
        short x = tile.x;
        short y = tile.y;

        return x % 3 == 0 && y % 3 == 0;
    }
}