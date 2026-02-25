package hao1337.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import hao1337.HVars;
import static hao1337.net.Net.*;
import hao1337.net.IORouter;
import hao1337.net.Protocol.PlayerAuthSuccess;
import mindustry.Vars;
import mindustry.game.EventType.ResetEvent;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.input.InputHandler;
import mindustry.net.NetConnection;
import mindustry.ui.Styles;

/**
 * UI widget that allows the player to control the game time speed.
 * Provides buttons for slowing down, resetting and speeding up the simulation,
 * and synchronizes its state across network connections in multiplayer.
 *
 * <p>The value of {@code time} may be negative to indicate reverse playback
 * at fractional speeds.  Changes are propagated using a custom packet router
 * defined in {@link hao1337.net.Net}.</p>
 */
public class TimeControl extends Table {
    /** unique identifier used in network packets to avoid processing our own changes. */
    private static final String uuid = UUID.randomUUID().toString();
    /** maximum allowed time multiplier. */
    private static final int MAX_SPEED = 8;
    /** minimum allowed time multiplier (negative for slow-reverse). */
    private static final int MIN_SPEED = -7;
    /** colour gradient used for the speed label when rendering. */
    private static final Color[] GRADIENT = { Pal.lancerLaser, Pal.accent, Color.valueOf("cc6eaf") };

    /**
     * When {@code true} clients in multiplayer are permitted to request a speed
     * increase.  Turning this off prevents out‑of‑sync issues at the cost of
     * disabling user control of time on clients.
     */
    public boolean enableSpeedUp = true;
    /**
     * If {@code false} the entire control widget is disabled (no buttons are
     * shown and speed cannot be changed).
     */
    public boolean useable = true;
    /** current multiplier setting. Positive values speed up, negative values
     * slow or reverse; 1 is normal real-time.
     */
    public int time = 1;

    /** Icon used by the slowdown button. */
    public Drawable left = new TextureRegionDrawable(Icon.left);
    /** Icon for the speedup button. */
    public Drawable right = new TextureRegionDrawable(Icon.right);
    /** Icon for the reset button. */
    public Drawable reset = new TextureRegionDrawable(Icon.refresh);

    private int displaytime = 1;
    private float gametime = 1;
    private Cell<Label> label;
    private boolean IUseable = true;
    private boolean IEnableState = true;
    private boolean needRebuild = false;

    /**
     * Construct a new TimeControl widget and register its network handlers.
     */
    public TimeControl() {
        super();
        netRegister();
    }
    
    /**
     * Helper to build a styled button with padding and tooltip.
     *
     * @param t        parent table where the button is added
     * @param action   runnable to execute when clicked
     * @param texture  icon to display on the button
     * @param toolTip  localization key for the tooltip text
     * @param padLeft  left padding in pixels
     * @param padRight right padding in pixels
     */
    void buildButton(Table t, Runnable action, Drawable texture, String toolTip, float padLeft, float padRight) {
        t.button(texture, 10f, action)
        .height(40f)
        .padLeft(padLeft)
        .padRight(padRight)
        .tooltip(ht -> ht.background(Styles.black6).add(Core.bundle.format(toolTip))
        .style(Styles.outlineLabel));
    }

    /**
     * Layout the UI elements and initialise labels/buttons based on the
     * current usability and admin state.
     */
    public void build() {
        name = "hao1337-timecontrol-ui";
        background(Htex.paneTopRight);
        top();

        table(null, table -> {
            table.label(() -> Core.bundle.format("hao1337.timecontrol.speedlabel"));
            label = table.label(() -> (time < 0 ? "×1/" : "×") + displaytime);
            marginBottom(10f);
        });
        row();

        table(null, table -> {
            margin(20f);
            if (IUseable && IEnableState) {
                buildButton(table, () -> update(-1), left, "hao1337.timecontrol.speeddown", 0f, 5f);
                buildButton(table, () -> update(), reset, "hao1337.timecontrol.speedreset", 0f, 5f);
                buildButton(table, () -> update(1), right, "hao1337.timecontrol.speedup", 0f, 0f);
                return;
            }
            if (IUseable && !IEnableState) {
                table.label(() -> Core.bundle.format("hao1337.timecontrol.notadmin.0"));
                table.row();
                table.label(() -> Core.bundle.format("hao1337.timecontrol.notadmin.1"));
                return;
            }
            table.label(() -> Core.bundle.format("hao1337.timecontrol.disable"));
        });
    }

    /**
     * Reconstruct the UI when global state changes (e.g. usability toggled or
     * admin rights lost).  Automatically schedules itself to run again if
     * further changes occur during the rebuild.
     */
    public void rebuild() {
        reset();
        build();

        update(() -> {
            boolean disable = shouldDisable();
            if (disable && !needRebuild) {
                reset();
                background(null);
                needRebuild = true;
                return;
            }

            if (!disable && needRebuild) {
                rebuild();
                needRebuild = false;
                return;
            }

            if (!(useable != IUseable || isEnable() != IEnableState) || disable) return;
            
            IUseable = useable;
            IEnableState = isEnable();

            rebuild();
        });
    }

    /**
     * Decide whether the time control UI should be hidden based on various
     * conditions (settings, HUD visibility, current input state).
     *
     * @return {@code true} if the widget should be disabled and hidden
     */
    boolean shouldDisable() {
        InputHandler input = Vars.control.input;
        return !Core.settings.getBool("hao1337.ui.timecontrol.enable") || !Vars.ui.hudfrag.shown || Vars.ui.minimapfrag.shown() || input.lastSchematic != null || !input.selectPlans.isEmpty();
    }

    /**
     * Recalculate derived values after the {@link #time} field changes.  This
     * method updates the display label, adjusts the game time scaling via the
     * {@code Time} API, and colours the label according to the speed.
     */
    void timeUpdate() {
        gametime = Math.abs(time);
        displaytime = (int) (time < 0 ? gametime + 1 : gametime);
        if (time < 0) gametime = 1 / (gametime + 1);

        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60 * gametime, 3 * gametime));
        if (label != null) label.color(Tmp.c1.lerp(GRADIENT, (time + MAX_SPEED) / (2 * MIN_SPEED)));
    }

    /**
     * Change the current speed by a relative step, enforce bounds, and send
     * updates if the value actually changed.
     *
     * @param step positive to speed up, negative to slow/reverse
     */
    void update(int step) {
        if (step > 0 && !enableSpeedUp) {
            Vars.ui.showInfoToast(Core.bundle.format("hao1337.ui.speedup.error"), 5f);
            return;
        }

        time += step;
        if (time > MAX_SPEED) {
            Vars.ui.showInfoToast(Core.bundle.format("hao1337.ui.speedlimit.up"), 2f);
            time = MAX_SPEED;
            return;
        }
        if (time < MIN_SPEED) {
            Vars.ui.showInfoToast(Core.bundle.format("hao1337.ui.speedlimit.down"), 2f);
            time = MIN_SPEED;
            return;
        }

        timeUpdate();
        updateSnapshot();
    }

    /**
     * Reset speed to 1 (normal) and optionally send a network snapshot.
     *
     * @param useSnapshot whether to broadcast the new speed to other clients
     */
    public void update(boolean useSnapshot) {
        time = 1;
        timeUpdate();
        if (useSnapshot) updateSnapshot();
    }

    /**
     * Convenience variant of {@link #update(boolean)} that always sends a
     * snapshot.
     */
    public void update() {
        time = 1;
        timeUpdate();
        updateSnapshot();
    }

    /**
     * Apply a received speed value from the network without rebroadcasting.
     *
     * @param speed new time multiplier to adopt
     */
    void setSnapshot(int speed) {
        time = speed;
        timeUpdate();
    }

    /**
     * Check whether the current player is allowed to use the control (either
     * in single-player mode or as an admin on a server).
     *
     * @return {@code true} if the control should be interactive for this user
     */
    boolean isEnable() {
        return HVars.net.isSinglePlayer() || Vars.player.admin;
    }

    /**
     * Register network handlers for time control packets and attach relevant
     * game event listeners.  The client handler applies incoming snapshots,
     * while the server handler rebroadcasts them to all connected clients.
     */
    void netRegister() {
        router.register(HVars.tcNetChannel, new IORouter.ChannelHandler() {
            public void handleClient(byte[] payload) {
                var i = new DataInputStream(new ByteArrayInputStream(payload));
                Reads r = new Reads(i);

                try  {
                    String Fuuid = r.str();
                    if (uuid.equals(Fuuid)) return;
                    useable = r.bool();

                    String mes = r.str();
                    if (mes.length() > 0) Vars.player.sendUnformatted(mes);
                    setSnapshot(r.i());
                } finally {
                    r.close();
                }
            }
            public void handleServer(NetConnection connection, byte[] payload) {
                var i = new DataInputStream(new ByteArrayInputStream(payload));
                Reads r = new Reads(i);

                try  {
                    // Server config is highest
                    String Fuuid = r.str();
                    if (uuid.equals(Fuuid)) return;
                    /** useable = */r.bool();
                    String mes = r.str();
                    if (mes.length() > 0) Vars.player.sendUnformatted(mes);
                    setSnapshot(r.i());

                    router.broadcast(HVars.tcNetChannel, payload);
                } finally {
                    r.close();
                }
            }
        });
        
        Events.on(PlayerAuthSuccess.class, e -> router.sendTo(e.connection, HVars.tcNetChannel, exportConfigPacket("")));
        Events.on(ResetEvent.class, e -> update(false));
    }

    /**
     * Build a packet containing the current configuration and an optional
     * message.  The packet is later sent via the custom router.
     *
     * @param mes text message to include (displayed on receipt)
     * @return serialized packet payload
     */
    byte[] exportConfigPacket(String mes) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(bos);
        Writes w = new Writes(s);

        try {
            w.str(uuid);
            w.bool(Core.settings.getBool("hao1337.ui.timecontrol.enable"));
            w.str(mes);
            w.i(time);

            return bos.toByteArray();
        } finally {
            w.close();
        }
    }

    /**
     * Broadcast the current speed setting to all peers, including a chat message
     * identifying the player who made the change.
     */
    void updateSnapshot() {
        try {
            byte[] payload = exportConfigPacket( "[orange][" + Vars.player.name + "][] Set time control to [accent]" + (time < 0 ? "×1/" : "×") + displaytime);
            router.send(HVars.tcNetChannel, payload);
        } catch (Throwable e) {}
    }
}
