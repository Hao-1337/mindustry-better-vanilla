package hao1337.net;

import static mindustry.Vars.*;

import java.util.HashMap;
import java.util.Map;

import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.net.NetConnection;

public class IORouter {
    public static interface ChannelHandler {
        void handleServer(NetConnection connection, byte[] payload);
        void handleClient(byte[] payload);
    }

    private final Map<Short, ChannelHandler> handlers = new HashMap<>();
    private final int maxPacketSize = 65536;

    public void register(short channel, ChannelHandler handler) {
        handlers.put(channel, handler);
    }

    public void dispatch(@Nullable NetConnection connection, IOPacket packet, boolean server) {
        Log.info("Got packet {server=@}", server);
        if (packet.payload.length > maxPacketSize)
            throw new RuntimeException("Packet too large");

        ChannelHandler handler = handlers.get(packet.channel);

        if (handler == null)
            return; // ignore unknown channel
        if (server && connection == null)
            return;

        if (server)
            handler.handleServer(connection, packet.payload);
        else
            handler.handleClient(packet.payload);
    }

    public void init() {
        net.handleServer(IOPacket.class, (con, pack) -> dispatch(con, pack, true));
        net.handleClient(IOPacket.class, (pack) -> dispatch(null, pack, false));
    }

    public void send(short channel, byte[] payload) {
        if (Net.protocol.isCompatible)
            Vars.net.send(new IOPacket(channel, payload), true);
    }

    public void sendTo(NetConnection connection, short channel, byte[] payload) {
        if (Net.protocol.isCompatible && Net.protocol.authPlayers.containsKey(connection))
            connection.send(new IOPacket(channel, payload), true);
    }

    public void broadcast(short channel, byte[] payload) {
        if (Net.protocol.isCompatible)
            for (NetConnection c : Vars.net.getConnections()) {
                if (Net.protocol.authPlayers.containsKey(c)) c.send(new IOPacket(channel, payload), true);
        }
    }

}
