package backupsystem.datastructures;

import backupsystem.interfaces.PeerInterface;

import java.io.Serializable;
import java.util.Objects;

public class PeerInfo implements Serializable {
    private final String name;
    private final String IPAddress;
    private final PeerInterface stub;

    public PeerInfo(String name, String IPAddress, PeerInterface stub) {
        this.name = name;
        this.IPAddress = IPAddress;
        this.stub = stub;
    }

    public String getName() {
        return name;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public PeerInterface getStub() {
        return stub;
    }

    @Override
    public String toString() {
        return "backupsystem.datastructures.PeerInfo{" +
                "name='" + name + '\'' +
                ", IPAddress='" + IPAddress + '\'' +
                ", stub=" + stub +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return Objects.equals(name, peerInfo.name) && Objects.equals(IPAddress, peerInfo.IPAddress) && Objects.equals(stub, peerInfo.stub);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, IPAddress, stub);
    }
}
