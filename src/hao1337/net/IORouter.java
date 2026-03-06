package hao1337.net;

import static mindustry.Vars.*;

import java.util.HashMap;
import java.util.Map;

import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.net.NetConnection;

/**
 * Lightweight router for dispatching custom I/O packets on named channels.
 * Applications register {@link ChannelHandler}s keyed by a short channel id.
 * The router is responsible for basic validation, invoking handlers on the
 * correct side (client/server), and providing convenience methods to send,
 * broadcast or target clients.
 * 
 * @author Hao-1337
 */
public class IORouter {
    /**
     * Interface implemented by consumers of router messages.  Handlers may be
     * invoked on either the server or client depending on context.
     */
    public static interface ChannelHandler {
        /**
         * Called on the server when a packet arrives from a client.
         *
         * @param connection origin connection
         * @param payload    raw packet bytes
         */
        void handleServer(NetConnection connection, byte[] payload);
        /**
         * Called on the client when a packet arrives from the server.
         *
         * @param payload raw packet bytes
         */
        void handleClient(byte[] payload);
    }

    private final Map<Short, ChannelHandler> handlers = new HashMap<>();
    private final int maxPacketSize = 65536;

    /**
     * Register a handler for a particular channel identifier.  Any packets
     * arriving later on this channel will be routed to the provided handler.
     *
     * @param channel numeric channel id
     * @param handler handler implementation
     */
    public void register(short channel, ChannelHandler handler) {
        handlers.put(channel, handler);
    }

    /**
     * Internal receiver invoked by the underlying network layer.  Validates the
     * packet size, looks up the appropriate handler, and delegates to the
     * correct callback depending on whether this side is acting as server or
     * client.
     *
     * @param connection origin connection (null when on client)
     * @param packet     received IOPacket
     * @param server     {@code true} if dispatching on the server side
     */
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

    /**
     * Attach this router to the global {@code net} instance so that
     * {@link IOPacket} messages are forwarded into {@link #dispatch}.
     * Must be called during mod initialisation.
     */
    public void init() {
        net.handleServer(IOPacket.class, (con, pack) -> dispatch(con, pack, true));
        net.handleClient(IOPacket.class, (pack) -> dispatch(null, pack, false));
    }

    /**
     * Send a packet to the server on the given channel.
     *
     * @param channel channel id
     * @param payload raw bytes to send
     */
    public void send(short channel, byte[] payload) {
        if (Net.protocol.isCompatible)
            Vars.net.send(new IOPacket(channel, payload), true);
    }

    /**
     * Send a packet directly to a specific connection (server‑side use only).
     *
     * @param connection recipient connection
     * @param channel    channel id
     * @param payload    bytes to send
     */
    public void sendTo(NetConnection connection, short channel, byte[] payload) {
        if (Net.protocol.isCompatible && Net.protocol.authPlayers.containsKey(connection))
            connection.send(new IOPacket(channel, payload), true);
    }

    /**
     * Broadcast a packet to all authenticated clients currently connected to
     * the server.
     *
     * @param channel channel id
     * @param payload bytes to send
     */
    public void broadcast(short channel, byte[] payload) {
        if (Net.protocol.isCompatible)
            for (NetConnection c : Vars.net.getConnections()) {
                if (Net.protocol.authPlayers.containsKey(c)) c.send(new IOPacket(channel, payload), true);
        }
    }

}
