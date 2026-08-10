package hao1337.lib.liquidcomps;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.Unit;
import mindustry.type.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.sandbox.LiquidSource;
import mindustry.world.modules.LiquidModule;

import static mindustry.Vars.*;

public class MultiLiquidUnloader extends LiquidSource {
    TextureRegion centerRegion;

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
        MultiLiquidRouter.MultiLiquidRouterBuilding router;

        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            
            for (var build : proximity) {
                if (build instanceof MultiLiquidRouter.MultiLiquidRouterBuilding r && r.isValid()) {
                    this.router = r;
                    break;
                }
            }
        }

        @Override
        public void updateTile() {
            if (unloadLiquid == null) {
                liquids.clear();
                return;
            }

            if (router == null || !router.isValid()) {
                liquids = new LiquidModule();
                return;
            }

            var graph = router.getGraph();
            if (graph == null) return;

            liquids = graph.liquids;
            if (graph.allowTransfer) dumpLiquid(unloadLiquid);
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

            if (!headless)
                recache();
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
