package hao1337.addins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import hao1337.HVars;
import hao1337.net.IORouter;
import hao1337.net.Protocol.PlayerAuthSuccess;
import static hao1337.net.Net.*;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.net.NetConnection;

public class TimeControlBackend {
    /** current multiplier setting. Positive values speed up, negative values
     * slow or reverse; 1 is normal real-time.
     */
    public int time = 1;
    
    int displaytime = 1;
    float gametime = 1;
    boolean useable = false;

    public TimeControlBackend() { netWorking(); }
    public boolean isUsable() { return useable; }
    public int getTimeState() { return time; }
    public void setTimeState(int t) {
        time = t;
        timeUpdate();
        updateSnapshot();
    }
    public String getDisplayTime() { return (time < 0 ? "×1/" : "×") + displaytime; }

    /**
     * Recalculate derived values after the {@link #time} field changes.  Thiswwssasasas
     * method updates the display label, adjusts the game time scaling via the
     * {@code Time} API, and colours the label according to the speed.
     */
    void timeUpdate() {
        Log.info("[MBV] Time control state has been changed to: @", time);

        gametime = Math.abs(time);
        displaytime = (int) (time < 0 ? gametime + 1 : gametime);
        if (time < 0) gametime = 1 / (gametime + 1);

        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60 * gametime, 3 * gametime));
        // if (label != null) label.color(Tmp.c1.lerp(GRADIENT, (time + MAX_SPEED) / (2 * MIN_SPEED)));
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
     * Apply a received speed value from the network without rebroadcasting.
     *
     * @param speed new time multiplier to adopt
     */
    void setSnapshot(int speed) {
        time = speed;
        timeUpdate();
    }

    /**
     * Register network handlers for time control packets and attach relevant
     * game event listeners.  The client handler applies incoming snapshots,
     * while the server handler rebroadcasts them to all connected clients.
     */
    void netWorking() {
        router.register(HVars.tcNetChannel, new IORouter.ChannelHandler() {
            public void handleClient(byte[] payload) {
                var i = new DataInputStream(new ByteArrayInputStream(payload));
                Reads r = new Reads(i);

                try  {
                    String Fuuid = r.str();
                    if (HVars.uuid.equals(Fuuid)) return;
                    useable = r.bool();

                    String mes = r.str();
                    if (mes.length() > 0 && Vars.player != null) Vars.player.sendUnformatted(mes);
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
                    if (HVars.uuid.equals(Fuuid)) return;
                    /** useable = */r.bool();
                    String mes = r.str();
                    if (mes.length() > 0 && Vars.player != null) Vars.player.sendUnformatted(mes);
                    setSnapshot(r.i());

                    router.broadcast(HVars.tcNetChannel, payload);
                } finally {
                    r.close();
                }
            }
        });

        Events.on(PlayerAuthSuccess.class, e -> router.sendTo(e.connection, HVars.tcNetChannel, exportConfigPacket("")));
        Events.on(ResetEvent.class, e -> update(false));
        Events.on(StateChangeEvent.class, e -> {
            if (e.to == GameState.State.menu) update(false);
        });
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
            w.str(HVars.uuid);
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
            byte[] payload = exportConfigPacket("[orange][" + Vars.player.name + "][] Set time control to [accent]" + (time < 0 ? "×1/" : "×") + displaytime);
            router.send(HVars.tcNetChannel, payload);
        } catch (Throwable e) {}
    }
}
