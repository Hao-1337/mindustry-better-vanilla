package hao1337.net;

import arc.Events;
import arc.struct.ObjectIntMap;
import arc.util.Log;
import arc.util.Timer;
import arc.util.Nullable;
import mindustry.Vars;
import hao1337.HVars;
import mindustry.game.EventType.*;
import mindustry.net.NetConnection;

/** Implement network protocol for mod comunicate */
public class Protocol {
    public static final String protocolVersion = "43abc1b2-8adb-4527-ab27-e4a370095089";

    public static class ProtocolChange {
        public final ProtocolType type;

        public ProtocolChange(@Nullable ProtocolType t) {
            if (t != null)
                this.type = t;
            else
                this.type = ProtocolType.fetch();
        }
    }

    public static class PlayerAuthSuccess {
        public final NetConnection connection;
        public PlayerAuthSuccess(NetConnection con) {
            connection = con;
        }
    }

    public void init() {
        Events.on(ClientLoadEvent.class, e -> protocolChangeFire(false));
        Events.on(HostEvent.class, e -> protocolChangeFire(false));
        Events.on(ResetEvent.class, e -> protocolChangeFire(false));
        Events.on(WorldLoadEndEvent.class, e -> protocolChangeFire(true));
        Events.on(ConnectPacketEvent.class, e -> playerConnections.put(e.connection, 0));

        Events.on(PlayerLeave.class, e -> {
            authPlayers.remove(e.player.con);
            playerConnections.remove(e.player.con);
        });

        Events.on(PlayerConnectionConfirmed.class, e -> {
            Log.info("[green][Server][][blue] Auth started for uuid '[accent]@[]'", e.player.con.uuid);
            Timer.schedule(() -> {
                if (e.player.con.hasDisconnected) {
                    playerConnections.remove(e.player.con);
                    return;
                }
                if (playerConnections.containsKey(e.player.con)) {
                    Log.warn("[scarlet][Server][] Auth failed (timeout) for '@'", e.player.con.uuid);
                    kick(e.player.con);
                    playerConnections.remove(e.player.con);
                }
            }, 2.5f);
        });

        Vars.net.handleServer(HandShakePacket.class, (con, p) -> {
            if (!p.name.equals(HVars.name) || !p.version.equals(HVars.version)) {
                Log.warn("[scarlet][Server][] Auth failed due to version mismatch for '@'", con.uuid);
                kick(con);
            }
            else {
                Events.fire(new PlayerAuthSuccess(con));
                Log.info("[green][Server][] Auth success for '@'", con.uuid);
                playerConnections.remove(con);
                authPlayers.put(con, 0);
            }
        });

        protocolChangeFire(false);
    }

    public boolean serverIsCompatible() {
        return isCompatible;
    }

    public void kick(NetConnection con) {
        if (lastProtocolType.isServer()) {
            playerConnections.remove(con);
            con.kick("[scarlet]Connection couldn't be authenticated.[]\n\nThis server requires [accent][Better Vanilla] - ("
                    + HVars.version + ")[] to play!", 10);
        }
    }

    final String validProtocolString = "_hMod_internal_" + protocolVersion;
    final ObjectIntMap<NetConnection> playerConnections = new ObjectIntMap<>();
    final ObjectIntMap<NetConnection> authPlayers = new ObjectIntMap<>();
    ProtocolType lastProtocolType;
    boolean isCompatible = false;

    void protocolChangeFire(boolean doHandShake) {
        var state = ProtocolType.fetch();
        if (lastProtocolType == state && !doHandShake)
            return;
        if (lastProtocolType != state)
            Log.info("[Protocol Listener] Detect protocol state change: [green]@", state.toString());

        lastProtocolType = state;

        if (state.isServer()) {
            Vars.state.rules.tags.put(validProtocolString, HVars.name + ":" + HVars.version);
            isCompatible = true;
        }

        else if (state == ProtocolType.LOCAL) {
            isCompatible = true;
        }

        else {
            if (Vars.state.rules.tags.containsKey(validProtocolString)) {
                isCompatible = true;

                var value = Vars.state.rules.tags.get(validProtocolString);
                String[] kvs = value.split(":");

                if (kvs.length == 2 && kvs[0].equals(HVars.name) && kvs[1].equals(HVars.version)) {
                    var packet = new HandShakePacket();
                    packet.name = HVars.name;
                    packet.version = HVars.version;
                    Vars.net.send(packet, true);
                }
            }
            else isCompatible = false;
        }
        Events.fire(new ProtocolChange(state));
    }
}
