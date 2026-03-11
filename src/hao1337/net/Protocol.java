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

/**
 * Implements custom network protocol for mod communication in Mindustry.
 * Handles server/client detection, version handshake and player authentication.
 * 
 * @author Hao-1337
 */
public class Protocol {
    /** Unique protocol version identifier used for compatibility checks. */
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

    /**
     * Registers all event listeners and packet handlers required by the protocol.
     * Should be called once during mod initialization.
     */
    public void init() {
        Events.on(ClientLoadEvent.class, e -> protocolChangeFire(false));
        Events.on(HostEvent.class, e -> protocolChangeFire(false));
        Events.on(ResetEvent.class, e -> protocolChangeFire(false));
        Events.on(WorldLoadEndEvent.class, e -> protocolChangeFire(true));
        Events.on(ConnectPacketEvent.class, e -> {
            playerConnections.put(e.connection, 0);
            Log.info("[green][Server][][blue] Auth started for '[accent]@[]'", e.connection);
        });

        Events.on(PlayerLeave.class, e -> {
            authPlayers.remove(e.player.con);
            playerConnections.remove(e.player.con);
        });

        Events.on(PlayerConnectionConfirmed.class, e -> {
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
                Log.info("[green][Server][] Auth success for '@'", con.uuid);
                playerConnections.remove(con);
                authPlayers.put(con, 0);
                Timer.schedule(() -> Events.fire(new PlayerAuthSuccess(con)), 0.15f);
            }
        });

        protocolChangeFire(false);
    }

    /**
     * Checks if the current server is compatible with this mod's protocol.
     * @return true if the server supports this mod version
     */
    public boolean serverIsCompatible() {
        return isCompatible;
    }

    /**
     * Kicks a connection with a "requires Better Vanilla" message.
     * Only works when running as server.
     * @param con the connection to kick
     */
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
