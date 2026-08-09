package hao1337.lib.liquidcomps;

import arc.struct.Seq;
import mindustry.gen.Building;
import mindustry.world.modules.LiquidModule;

public class LiquidGraph {
    /** Shared liquid inventory. */
    public final LiquidModule liquids;
    /** Every storage currently belonging to this graph. */
    public final Seq<Building> members = new Seq<>();
    /** Liquid capacity share across all instance */
    public float parentLimit = 10f;

    public boolean allowTransfer = false;
    public final LiquidModuleCaculator caculator = new LiquidModuleCaculator();

    public LiquidGraph(LiquidModule module, float parentLimit) {
        liquids = module;
        this.parentLimit = parentLimit;
    }

    public static interface InferLiquidGraph {
        LiquidGraph getGraph();
        void setGraph(LiquidGraph graph);
    }

    public <T extends Building & InferLiquidGraph> void add(T build) {
        var graph = build.getGraph();
        if (graph == this)
            return;

        if (graph != null) {
            graph.members.remove(build);
        }

        members.add(build);
        build.setGraph(this);
        build.liquids = liquids;
    }

    public <T extends Building & InferLiquidGraph> void remove(T build) {
        members.remove(build);

        if (build.getGraph() == this)
            build.setGraph(null);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LiquidGraph[members=" + members.size + "]");

        for (var b : members)
            builder.append(b);
        return builder.toString();
    }
}