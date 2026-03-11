package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

/**
 * Simple networking packet used during the custom handshake phase.  The packet
 * carries a mod or client {@code version} string
 * 
 * @author Hao-1337
 */
public class HandShakePacket extends Packet {
    /** human-readable version identifier. */
    public String version;

    /**
     * No-argument constructor required for reflective instantiation by the
     * packet router.
     */
    public HandShakePacket(){}

    /**
     * Convenience constructor used when sending the packet manually.
     *
     * @param name    name string to advertise
     * @param version version string associated with the name
     */
    public HandShakePacket(String version){
        this.version = version;
    }

    @Override
    public void write(Writes w){
        w.str(version);
    }

    @Override
    public void read(Reads r){
        version = r.str();
    }

    @Override
    public int getPriority(){
        return Packet.priorityHigh;
    }
} 