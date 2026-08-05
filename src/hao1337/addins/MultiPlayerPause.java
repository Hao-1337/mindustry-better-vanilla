package hao1337.addins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import arc.Core;
import arc.util.Interval;
import arc.util.io.Reads;
import arc.util.io.Writes;
import hao1337.HVars;
import static hao1337.net.Net.*;
import hao1337.net.IORouter;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.input.Binding;
import mindustry.net.NetConnection;

public class MultiPlayerPause {
    public MultiPlayerPause() { networking(); }
    private final Interval interval = new Interval();
    private boolean debounce = false;

    @PlatformDependance(version = "v159")
    public boolean disable() {
        return Vars.state.rules.pauseDisabled;
    }

    public void pool() {
        if (debounce) {
            if (interval.get(40)) {
                debounce = false;
            }
            return;
        }

        if (Core.input.keyTap(Binding.pause) && !HVars.net.isSinglePlayer() && !disable()) {
            debounce = true;
            var doPause = Vars.state.isPaused();
            var packet = exportConfigPacket(doPause ? false : true, "[orange][" + Vars.player.name + "][] " + (doPause ? "Resume the game" : "Stop the game"));
            router.send(HVars.pauseNetChannel, packet);
        }
    }

    public void networking() {
        router.register(HVars.pauseNetChannel, new IORouter.ChannelHandler() {
            public void handleClient(byte[] payload) {
                // Game acutally do it for you!
                var i = new DataInputStream(new ByteArrayInputStream(payload));
                Reads r = new Reads(i);

                try  {
                    String Fuuid = r.str();
                    if (HVars.uuid.equals(Fuuid)) return;

                    r.bool();
                    r.bool();
                    String mess = r.str();
                    Vars.player.sendUnformatted(mess);
                } finally {
                    r.close();
                }
            }

            public void handleServer(NetConnection connection, byte[] payload) {
                var i = new DataInputStream(new ByteArrayInputStream(payload));
                Reads r = new Reads(i);

                try  {
                    String Fuuid = r.str();
                    if (HVars.uuid.equals(Fuuid)) return;

                    boolean requestPause = r.bool(),
                    isAdmin = r.bool();
                    if (!isAdmin) return /* Only admin can be pause */;

                    String mes = r.str();
                    if (mes.length() > 0) Vars.player.sendUnformatted(mes);
                    Vars.state.set(requestPause ? GameState.State.paused : GameState.State.playing);
                    router.broadcast(HVars.pauseNetChannel, payload);
                } finally {
                    r.close();
                }
            }
        });
    }

    byte[] exportConfigPacket(boolean doPause, String mes) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream s = new DataOutputStream(bos);
        Writes w = new Writes(s);

        try {
            w.str(HVars.uuid);
            w.bool(doPause);
            w.bool(Vars.player.admin);
            w.str(mes);

            return bos.toByteArray();
        } finally {
            w.close();
        }
    }
}
