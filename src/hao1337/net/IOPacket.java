package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

/**
 * Generic packet that wraps arbitrary byte payloads sent over a
 * {@code short}-identified channel.  Used by the custom I/O router to carry
 * messages between clients and server.
 * 
 * @author Hao-1337
 */
public class IOPacket extends Packet {
    /** channel identifier for routing. */
    public short channel;
    /** raw byte payload sent on the channel. */
    public byte[] payload;

    /**
     * No-arg constructor required by the packet instantiation mechanism.
     */
    public IOPacket(){}

    /**
     * Simple initializer.
     *
     * @param channel channel id
     * @param payload message bytes
     */
    public IOPacket(short channel, byte[] payload){
        this.channel = channel;
        this.payload = payload;
    }

    @Override
    public void write(Writes w){
        w.s(channel);
        w.i(payload.length);
        w.b(payload);
    }

    @Override
    public void read(Reads r){
        channel = r.s();
        int len = r.i();

        if(len < 0 || len > 65536){
            throw new RuntimeException("Invalid packet size");
        }

        payload = r.b(len);
    }

    @Override
    public int getPriority(){
        return Packet.priorityNormal;
    }
}
