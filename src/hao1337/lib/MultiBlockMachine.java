package hao1337.lib;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.math.geom.Geometry;
import arc.math.geom.Rect;
import arc.scene.ui.layout.Table;
import arc.struct.EnumSet;
import arc.util.Eachable;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Tmp;
import arc.struct.Seq;

import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.BuildVisibility;

public class MultiBlockMachine extends Block {
   // 2D array that contains build sechematic
   public int[][] buildPlans;
   // Hash map for sechematic array
   public String[] planIndex;
   @Nullable
   public Block[] blockIndex;
   // Build size
   public int areaSize = 32;
   protected int capacity = areaSize * areaSize;
   // Block flag
   protected BlockFlag flag = BlockFlag.factory;
   // frame offset (tile)
   protected float offsetX = 0f, offsetY = 0f;
   // Indicate that block should be a part of the machine
   public boolean selfIsMachine = true;
   // Indicate the block this machine transfrom to when build complete. Using null
   // for keep the machine
   @Nullable
   public String magreInto = null;
   private int correct = 0;

   MultiBlockMachine(String name) {
      super(name);
      update = true;
      solid = true;
      sync = true;
      flags = EnumSet.of(flag);
   }

   @Override
   public void init() {
      hasItems = false;
      hasLiquids = false;
      hasPower = true;
      outputsPower = false;
      outputsPayload = false;
      rotate = true;
      offset = 0f;
      buildVisibility = BuildVisibility.shown;
      consumePower(5f);

      if (selfIsMachine) {
         offsetX += size;
         offsetY += size;
      }

      offsetX *= 8;
      offsetY *= 8;
      capacity = areaSize * areaSize;
      blockIndex = new Block[planIndex.length];

      for (int i = 0; i < planIndex.length; i++) {
         if (blockIndex == null)
            blockIndex[i] = null;
         try {
            blockIndex[i] = Vars.content.block(planIndex[i]);
         } catch (Throwable err) {
            Log.err(err);
         }
      }
      super.init();
   }

   public boolean canPlaceOn(Tile tile, Team team, int rotation) {
      Rect rect = getRect(Tmp.r1, tile.worldx() + this.offset, tile.worldy() + this.offset, rotation).grow(0.1F);
      return !Vars.indexer.getFlagged(team, flag).contains((b) -> {
         return getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect);
      });
   }

   public Rect getRect(Rect rect, float x, float y, int rotation) {
      int rx = Geometry.d4x(rotation);
      int ry = Geometry.d4y(rotation);

      rect.setCentered(x, y, areaSize * 8);

      float len = (float) (8 * (areaSize + size)) / 2.0F;
      rect.x += (float) rx * len - ry * len - rx * offsetX + ry * offsetX;
      rect.y += (float) ry * len + rx * len - ry * offsetY - rx * offsetY;

      return rect;
   }

   @Override
   public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
      Draw.rect(region, plan.drawx(), plan.drawy());
   }

   public void drawPlace(int x, int y, int rotation, boolean valid) {
      super.drawPlace(x, y, rotation, valid);
      x *= 8;
      y *= 8;
      x = (int) ((float) x + offset);
      y = (int) ((float) y + offset);
      Rect rect = getRect(Tmp.r1, (float) x, (float) y, rotation);
      Drawf.dashRect(valid ? Pal.accent : Pal.remove, rect);
   }

   private class Bulding2D {
      public boolean isValid = false;

      Interval interval = new Interval();
      Seq<Block> maching = new Seq<Block>();
      int[][] buildPlansT;
      int lastRotate = -1;

      void updateMatching(int rotation) {
         if (rotation == lastRotate)
            return;

         lastRotate = rotation;
         maching.clear();

         int uh = 0;
         buildPlansT = Matrix.rotate(buildPlans, rotation);

         do {
            // Log.info(uh + "");
            int indexX = (int) Math.floor(uh / areaSize);
            int indexY = uh % areaSize;

            maching.add(blockIndex[buildPlansT[indexX][indexY]]);
         } while (uh++ < capacity - 1);
      }

      public void updateBounding(Rect rec, int rotation) {
         if (!interval.get(10))
            return;

         updateMatching(rotation);
         correct = 0;

         int lx = Math.round(rec.x / 8);
         int ly = Math.round(rec.y / 8);
         int hx = (int) Math.floor((rec.height + rec.x) / 8);
         int hy = (int) Math.floor((rec.width + rec.y) / 8);
         boolean v = false;

         for (int x = lx; x <= hx; x++) {
            for (int y = ly; y <= hy; y++) {
               int indexX = areaSize - x + lx - 1;
               int indexY = y - ly;
               int indexM = indexX * 16 + indexY;

               @Nullable
               Building build = Vars.world.tiles.get(x, y).build;
               Block target = maching.get(indexM);

               // Log.info(build == null ? "null" : "Current: " + build.block.name + " Match: "
               // + maching.get(indexM));
               if (build == null || build.block.name != target.name) {
                  v = true;
                  continue;
               }

               correct ++;
            }
         }
         isValid = !v;
      }
   }
   
   @Override
   public void setBars(){
       super.setBars();
   }

   public class MultiBlockMachineBuild extends Building {
      final Color doneColor = Color.rgb(0, 156, 23);
      Bulding2D build = new Bulding2D();

      public void updateTile() {
         build.updateBounding(getRect(Tmp.r1, x, y, rotation), rotation);
      }

      @Override
      public void draw() {
         super.draw();
         addBar("dai-vip-vai-lon", (e) -> (Bar)new ProgressBar(
            () -> Core.bundle.format("hao1337.block.correct-count", new Object[]{(float)correct / capacity * 100}),
            Color.orange,
            () -> correct / capacity
         ));
      }

      @Override
      public void displayBars(Table table) {
         super.displayBars(table);
      }

      @Override
      public void drawStatus() {
         BlockStatus status = status();
         if (status == BlockStatus.active) Drawf.dashRect(build.isValid ? doneColor : Pal.accent, getRect(Tmp.r1, x, y, rotation));
         super.drawStatus();
      }

      @Override
      public void drawSelect() {
         super.drawSelect();
         Drawf.dashRect(build.isValid ? doneColor : Color.white, getRect(Tmp.r1, x, y, rotation));
      }
   }
}
