package hao1337.addons.autodrill.ui;

import arc.Core;
import arc.func.Cons;
import arc.input.InputProcessor;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import hao1337.addons.autodrill.HeuristicGroundOrePF.Direction;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

/**
 * A UI table that displays directional buttons in a cross/plus pattern for selection.
 * 
 * <p>This table provides an interactive UI component for choosing a direction (UP, DOWN, LEFT, RIGHT)
 * or clearing the selection (null). The table positions itself relative to a tile on the game map
 * and automatically hides when the player clicks outside of it or when a selection is made.</p>
 * 
 * @see Direction
 * @see Table
 */
public class DirectionTable extends Table {
    public final float buttonSize;
    public float tileX = 0, tileY = 0;

    InputProcessor inputHandler = new InputProcessor() {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, KeyCode button) {
            if (!hasMouse())
                visible = false;

            return InputProcessor.super.touchDown(screenX, screenY, pointer, button);
        }
    };

    public DirectionTable() {
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

    public void build(Cons<Direction> onClick) {
        reset();
        background(Styles.black3);
        margin(2f);
        Core.input.addProcessor(inputHandler);

        update(() -> {
            if (Vars.state.isMenu()) {
                visible = false;
                return;
            }
            Vec2 v = Core.camera.project(tileX * Vars.tilesize, (tileY + 1) * Vars.tilesize);
            setPosition(v.x, v.y, Align.bottom);
        });


        table(t -> {
            t.label(() -> Core.bundle.format("hao1337.ui.addon.autodrill.direction"));
        }).growX().fillX();
        row();

        table(t -> {
            t.add().pad(2f);
            t.button(Icon.up, Styles.defaulti, () -> { onClick.get(Direction.UP); visible = false; }).pad(2f).get().resizeImage(buttonSize);
            t.add().pad(2f);
            t.row();
            t.button(Icon.left, Styles.defaulti, () -> { onClick.get(Direction.LEFT); visible = false; }).pad(2f).get().resizeImage(buttonSize);
            t.button(Icon.cancel, Styles.defaulti, () -> { onClick.get(null); visible = false; }).pad(2f).get().resizeImage(buttonSize);
            t.button(Icon.right, Styles.defaulti, () -> { onClick.get(Direction.RIGHT); visible = false; }).pad(2f).get().resizeImage(buttonSize);
            t.row();
            t.add().pad(2f);
            t.button(Icon.down, Styles.defaulti, () -> { onClick.get(Direction.DOWN); visible = false; }).pad(2f).get().resizeImage(buttonSize);
            t.add().pad(2f);
        });

        pack();
        act(0);
        visible = true;
    }
}
