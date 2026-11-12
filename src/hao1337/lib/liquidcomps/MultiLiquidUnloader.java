package hao1337.lib.liquidcomps;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import arc.util.*;
import arc.util.io.*;
import hao1337.addins.PlatformDependance;
import mindustry.Vars;
import mindustry.entities.units.*;
import mindustry.gen.Unit;
import mindustry.type.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.sandbox.LiquidSource;

import static mindustry.Vars.*;

public class MultiLiquidUnloader extends LiquidSource {
    @Nullable TextureRegion centerRegion;

    public MultiLiquidUnloader(String name) {
        super(name);
        configurations.clear();
        floating = false;

        config(Liquid.class, (MultiLiquidUnloaderBuild tile, Liquid liquid) -> tile.unloadLiquid = liquid);
        configClear((MultiLiquidUnloaderBuild tile) -> tile.unloadLiquid = null);
    }

    @Override
    public void init() {
        super.init();

        if (Vars.headless) return;
        centerRegion = Core.atlas.find(name + "-center");
    }

    @Override
    public void setBars() {
        super.setBars();

        removeBar("liquid");
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list) {
        drawPlanConfigCenter(plan, plan.config, name + "-center", false);
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[] { bottomRegion, region };
    }

    public class MultiLiquidUnloaderBuild extends LiquidSource.LiquidSourceBuild {
        public @Nullable Liquid unloadLiquid = null;
        Seq<MultiLiquidRouter.MultiLiquidRouterBuilding> routers = new Seq<>();

        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            
            for (var build : proximity) {
                if (
                    build instanceof MultiLiquidRouter.MultiLiquidRouterBuilding r &&
                    r.isValid() &&
                    !routers.contains(r1 -> ((MultiLiquidRouter) r1.block).groupId == ((MultiLiquidRouter) r.block).groupId)
                ) {
                    this.routers.add(r);
                }
            }
        }

        @Override
        public void updateTile() {
            if (unloadLiquid == null || routers.isEmpty()) {
                liquids = null;
                return;
            }

            for (var router : routers) {
                var graph = router.getGraph();
                if (graph == null) continue;
    
                float amount = graph.liquids.get(unloadLiquid);
    
                if (!graph.allowTransfer || amount < 0.1f) continue;
                liquids = graph.liquids;
                dumpLiquid(unloadLiquid);
                break;
            }
        }

        @Override
        public void draw() {
            super.draw();

            Draw.color(unloadLiquid == null ? Color.clear : unloadLiquid.color);
            Draw.rect(centerRegion, x, y);
            Draw.color();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            drawItemSelection(unloadLiquid);
        }

        @Override
        public void configured(Unit builder, Object value) {
            super.configured(builder, value);

            recacheT();
        }

        @PlatformDependance(version = "v159")
        void recacheT() {
            if (!headless) recache();
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(MultiLiquidUnloader.this, table, content.liquids(), () -> unloadLiquid, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Liquid config() {
            return unloadLiquid;
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(unloadLiquid == null ? -1 : unloadLiquid.id);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            int id = revision == 1 ? read.s() : read.b();
            unloadLiquid = id == -1 ? null : content.liquid(id);
        }
    }
}
