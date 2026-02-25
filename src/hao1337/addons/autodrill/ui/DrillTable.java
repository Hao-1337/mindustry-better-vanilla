package hao1337.addons.autodrill.ui;

import arc.Core;
import arc.func.Cons;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;

/**
 * A UI table for displaying drill block options based on the selected tile.
 * 
 * <p>The table positions itself above the selected tile and follows the camera,
 * automatically hiding when the game enters menu state or when the user clicks away.</p>
 * 
 * @author Hao-1337
 */
public class DrillTable extends Table {
    public final float buttonSize;

    private final InputProcessor inputHandler = new InputProcessor() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
            if (!hasMouse()) visible = false;
            return false;
        }
    };

    public DrillTable() {
        super(Styles.none);
        buttonSize = AutoDrill.buttonSize;
    }

    @Override
    public void reset() {
        super.reset();
        Core.input.removeProcessor(inputHandler);
        visible = false;
        clearChildren();
    }

    public void build(Tile tile, Cons<Block> onClick) {
        reset();
        Core.input.addProcessor(inputHandler);

        update(() -> {
            if (Vars.state.isMenu()) {
                visible = false;
                return;
            }
            Vec2 v = Core.camera.project(tile.worldx() * Vars.tilesize, (tile.worldy() + 1) * Vars.tilesize);
            setPosition(v.x, v.y, Align.bottom);
        });

        boolean selectedTileIsWallOre = tile.wallDrop() != null;
        for (Block block : Vars.content.blocks()) {
            if (!isAllowed(block)) continue;
            if (
                (!selectedTileIsWallOre && block instanceof Drill ground && ground.canMine(tile)) ||
                (selectedTileIsWallOre && block instanceof BeamDrill wall && wall.blockedItems.contains(tile.wallDrop()))
            ) {
                var icon = drawIcon(block);
                ImageButton button = new ImageButton(icon, Styles.defaulti);

                button.clicked(() -> {
                    if (onClick != null) onClick.get(block);
                    visible = false;
                });

                table(null, t -> t.add(button).get().resizeImage(buttonSize)).margin(2f);
                if (getChildren().size % 6 == 0) row();
            }
        }

        pack();
        act(0);
        visible = true;
    }

    boolean isAllowed(Block block) {
        return !block.isHidden() && !Vars.state.rules.bannedBlocks.contains(block) && block.unlockedNow() && block.environmentBuildable();
    }

    private TextureRegionDrawable drawIcon(Block block) {
        return new TextureRegionDrawable(block.uiIcon);
    }
}