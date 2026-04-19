package hao1337.addons.autodrill;

import arc.struct.Seq;
import arc.util.Nullable;
import hao1337.addons.autodrill.HeuristicGroundOrePF.Direction;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.units.BuildPlan;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.production.Drill;

/**
 * Abstract base class for pathfinding and placement of ground ore drills.
 * This class manages the logic for automatically discovering and planning drill placements
 * to extract ore from the ground, with support for bridge connections.
 *
 * @see GroundOrePathFinding
 * @author Hao-1337
 */
public abstract class GroundOrePathFinding {
    public final Block WATER_EXTRACTOR = Blocks.waterExtractor;

    /**
     * Cooperative build session that can be stepped over multiple frames.
     * Implementations must stay on the game thread because world queries are not
     * thread safe.
     */
    public interface BuildBatch {
        void step(int operationBudget);

        boolean isDone();

        float progress();

        Seq<BuildPlan> result();
    }

    /**
     * Generates a sequence of build plans for drill and bridge placement.
     * @return a sequence of BuildPlan objects representing the construction plan
     */
    public abstract Seq<BuildPlan> build();

    /**
     * Create an optional batched planner for cooperative main-thread execution.
     * The default implementation returns {@code null} and callers should fall back
     * to {@link #build()}.
     */
    public @Nullable BuildBatch createBuildBatch() {
        return null;
    }

    /**
     * The primary drill block selected for this pathfinding operation.
     */
    public final Block selectedDrill;
    /**
     * The maximum size of drills allowed in the pathfinding algorithm.
     */
    public final int maxDrillSize;
    /**
     * The output direction for ore conveyed from placed drills.
     */
    public final Direction outDirection;
    /**
     * If true, only drills of the selected size are allowed during pathfinding.
     * Default: false
     */
    public boolean forceSelectedSize = false;
    /**
     * Maximum heuristic search depth for the pathfinding algorithm.
     * Default: 120
     */
    public int maxDepth = 120;
    /**
     * Maximum number of ore tiles to consider per chunk.
     * Default: 200
     */
    public int maxTiles = 200;
    /**
     * Minimum coverage percentage required for a drill placement to be considered valid.
     * A drill placement is valid if it covers at least {n}% of ore tiles.
     * Default: 0.3f (approximately 30%)
     */
    public float minimumCoverage = 0.33f;
    /**
     * The range of the bridge block used for connections.
     */
    public final int bridgeRange;
    /**
     * The bridge block type used to connect drill outputs.
     */
    public boolean useWaterExtractor = true;
    /**
     * The bridge block type used to connect drill outputs.
     */
    public final Block bridgeBlock;
    /**
     * The given tile
     */
    Tile selectedTile;
    /**
     * The ore need to mine
     */
    public final Item targetOre;
    /**
     * Contains all possible drill for heuristic guessing
     */
    final Seq<Block> allowedDrill = new Seq<>();

    /**
     * Constructs a GroundOrePathFinding instance with the specified drill and configuration.
     *
     * @param drill the ground drill block to use (must be an instance of Drill)
     * @param startTile the starting tile containing the target ore
     * @param outputDirection the direction for ore output
     * @param bridgeType the type of item bridge to use, or null to use the default item bridge
     * @throws RuntimeException if the provided drill is not an instance of Drill
     */

    public GroundOrePathFinding(Block drill, Tile startTile, Direction outputDirection, @Nullable ItemBridge bridgeType) {
        if (!(drill instanceof Drill))
            throw new RuntimeException("Only ground drill allowed!");

        if (bridgeType == null) bridgeType = (ItemBridge) Blocks.itemBridge;
        bridgeBlock = bridgeType;
        bridgeRange = bridgeType.range;

        selectedDrill = drill;
        maxDrillSize = drill.size;
        selectedTile = startTile;
        outDirection = outputDirection;
        targetOre = startTile.drop();

        for (Block block : Vars.content.blocks()) {
            if (block instanceof Drill d && d.size <= maxDrillSize) {
                allowedDrill.add(block);
            }
        }

        if (drill != Blocks.mechanicalDrill) allowedDrill.remove(Blocks.mechanicalDrill);
    }
}