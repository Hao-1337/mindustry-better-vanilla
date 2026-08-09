package hao1337.lib.liquidcomps;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectFloatMap;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.liquid.LiquidRouter;
import mindustry.world.modules.LiquidModule;

/**
 * Multi-liquid router using a shared LiquidGraph.
 *
 * Notes:
 * - Routers only ACCEPT liquids.
 * - They DO NOT dump liquids (traditional piping cannot choose which liquid).
 * - Actual liquid storage is shared through LiquidGraph.
 */
public class MultiLiquidRouter extends LiquidRouter {
    public MultiLiquidRouter(String name) {
        super(name);

        update = true;
        solid = true;
        canOverdrive = false;
        floating = false;
    }
    
    @Override
    public boolean consumesLiquid(Liquid liq){
        return true;
    }

    @Override
    public void setBars() {
        super.setBars();

        // remove vanilla single-liquid bar
        barMap.remove("liquid");
    }

    
    public class MultiLiquidRouterBuilding extends LiquidBuild implements LiquidGraph.InferLiquidGraph {
        public MultiLiquidCoreStorage.MultiLiquidCoreStorageBuilding storageBuilding;
        LiquidGraph graph;
        public LiquidGraph getGraph() { return graph; }
        public void setGraph(LiquidGraph graph) { this.graph = graph; }
        
        @Override
        public void display(Table table) {
            super.display(table);
            table.row();
            table.table(t -> {
                if (storageBuilding != null) t.image(storageBuilding.block.uiIcon).size(32f).padRight(5f);
                t.label(() -> {
                    if (graph == null) return Core.bundle.format("hao1337.block.multi-liquid.no-core");
                    return Core.bundle.format("hao1337.block.multi-liquid.linked", tileX(), tileY());
                });
            }).left().padTop(5f);
        }
        
        @Override
        public void draw() {
            Draw.rect(this.block.region, this.x, this.y, this.drawrot());
            this.drawTeamTop();
        }

        @Override
        public void updateTile() {
            // Dynamically add bars for every stored liquid.
            if (storageBuilding == null || !storageBuilding.isValid()) {
                var storage = MultiLiquidCoreStorage.get(this.team);
                if (storage == null) {
                    storageBuilding = null;
                    graph = null;
                    liquids = new LiquidModule();
                    return;
                }

                storageBuilding = storage;
                graph = storageBuilding.getGraph();
                liquids = graph.liquids;
            }
            
            for (var liq : Vars.content.liquids()) {
                if (liquids.get(liq) > 0.05f) {
                    String name = "liquid-" + liq.name;

                    if (!barMap.containsKey(name)) {
                        addLiquidBar(liq);
                    }
                }
            }

            // Intentionally DO NOT dump liquids.
            // We'll make a dedicated extractor/unloader later.
        }

        ObjectFloatMap<Liquid> visualLiquids = new ObjectFloatMap<>();

        @Override    
        public void drawLight() {
            if (liquids == null) {
                return;
            }

            for (var liq : Vars.content.liquids()) {
                if (liquids.get(liq) < 0.01f) continue;

                if (this.block.hasLiquids && this.block.drawLiquidLight && liq.lightColor.a > 0.001F) {
                    var liqStep = this.visualLiquids.get(liq, 0f);
                    this.visualLiquids.put(liq, Mathf.lerpDelta(liqStep, this.liquids.get(liq) >= 0.01f ? 1.0f : 0.0f, 0.06f));
                    this.drawLiquidLight(liq, liqStep);
                }
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return graph == null
                    ? false
                    : graph.allowTransfer && liquids.get(liquid) < graph.parentLimit;
        }

        @Override
        public boolean canDumpLiquid(Building to, Liquid liquid) {
            return false;
        }

        @Override
        public boolean canPickup() {
            return false;
        }

        @Override
        public void dumpLiquid(Liquid liquid) {}

        @Override
        public byte version() {
            return 1;
        }
    }
}