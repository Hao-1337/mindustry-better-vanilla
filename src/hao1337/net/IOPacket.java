package hao1337.net;

import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.net.Packet;

public class IOPacket extends Packet {
    public short channel;
    public byte[] payload;

    public IOPacket(){}

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
