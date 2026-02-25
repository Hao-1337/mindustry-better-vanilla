package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

/**
 * Simple networking packet used during the custom handshake phase.  The packet
 * carries a mod or client {@code name} and its {@code version} string
 * 
 * @author Hao-1337
 */
public class HandShakePacket extends Packet {
    /** client or mod name being announced. */
    public String name;
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
    public HandShakePacket(String name, String version){
        this.name = name;
        this.version = version;
    }

    @Override
    public void write(Writes w){
        w.str(name);
        w.str(version);
    }

    @Override
    public void read(Reads r){
        name = r.str();
        version = r.str();
    }

    @Override
    public int getPriority(){
        return Packet.priorityHigh;
    }
} 