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

public class TimeControl extends Table {
    private static final String uuid = UUID.randomUUID().toString();
    private static final int MAX_SPEED = 8;
    private static final int MIN_SPEED = -7;
    private static final Color[] GRADIENT = { Pal.lancerLaser, Pal.accent, Color.valueOf("cc6eaf") };

    // If player in multiplayer game. When speedup client side game, it make game out of sync
    public boolean enableSpeedUp = true;
    // If false, disable speed control
    public boolean useable = true;
    // Current speed
    public int time = 1;

    // Icon for speeddown button
    public Drawable left = new TextureRegionDrawable(Icon.left);
    // Icon for speedup button
    public Drawable right = new TextureRegionDrawable(Icon.right);
    // Icon for reset speed button
    public Drawable reset = new TextureRegionDrawable(Icon.refresh);

    private int displaytime = 1;
    private float gametime = 1;
    private Cell<Label> label;
    private boolean IUseable = true;
    private boolean IEnableState = true;
    private boolean needRebuild = false;

    public TimeControl() {
        super();
        netRegister();
    }
    
    void buildButton(Table t, Runnable action, Drawable texture, String toolTip, float padLeft, float padRight) {
        t.button(texture, 10f, action)
        .height(40f)
        .padLeft(padLeft)
        .padRight(padRight)
        .tooltip(ht -> ht.background(Styles.black6).add(Core.bundle.format(toolTip))
        .style(Styles.outlineLabel));
    }

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

    boolean shouldDisable() {
        InputHandler input = Vars.control.input;
        return !Core.settings.getBool("hao1337.ui.timecontrol.enable") || !Vars.ui.hudfrag.shown || Vars.ui.minimapfrag.shown() || input.lastSchematic != null || !input.selectPlans.isEmpty();
    }

    void timeUpdate() {
        gametime = Math.abs(time);
        displaytime = (int) (time < 0 ? gametime + 1 : gametime);
        if (time < 0) gametime = 1 / (gametime + 1);

        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60 * gametime, 3 * gametime));
        if (label != null) label.color(Tmp.c1.lerp(GRADIENT, (time + MAX_SPEED) / (2 * MIN_SPEED)));
    }

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

    public void update(boolean useSnapshot) {
        time = 1;
        timeUpdate();
        if (useSnapshot) updateSnapshot();
    }

    public void update() {
        time = 1;
        timeUpdate();
        updateSnapshot();
    }

    void setSnapshot(int speed) {
        time = speed;
        timeUpdate();
    }

    boolean isEnable() {
        return HVars.net.isSinglePlayer() || Vars.player.admin;
    }

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

    void updateSnapshot() {
        try {
            byte[] payload = exportConfigPacket( "[orange][" + Vars.player.name + "][] Set time control to [accent]" + (time < 0 ? "×1/" : "×") + displaytime);
            router.send(HVars.tcNetChannel, payload);
        } catch (Throwable e) {}
    }
}
