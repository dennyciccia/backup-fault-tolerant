package backupsystem.datastructures;

import backupsystem.exceptions.DuplicateElementException;
import backupsystem.interfaces.PeerInterface;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class PeerList implements Serializable, Iterable<PeerInfo> {
    private final List<PeerInfo> list;

    /*
     * ----------- Constructors -----------
     */

    public PeerList() {
        list = new ArrayList<>();
    }

    // constructs object as a copy of passed list
    public PeerList(List<PeerInfo> list) {
        this.list = new ArrayList<>();
        for (PeerInfo peerInfo : list) {
            this.list.add(new PeerInfo(peerInfo.getName(), peerInfo.getIPAddress(), peerInfo.getStub()));
        }
    }

    /*
     * ----------- Getters -----------
     */

    public PeerInfo get(int index) throws IndexOutOfBoundsException {
        return list.get(index);
    }

    public PeerInfo getElementByName(String name) {
        for (PeerInfo p : list)
            if (p.getName().equals(name))
                return p;
        return null;
    }

    public PeerInfo getElementByStub(PeerInterface stub) {
        for (PeerInfo p : list)
            if (p.getStub().equals(stub))
                return p;
        return null;
    }

    /*
     * ----------- List methods -----------
     */

    @Override
    public Iterator<PeerInfo> iterator() {
        return list.iterator();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void add(PeerInfo peer) throws DuplicateElementException {
        if (nameInList(peer.getName()))
            throw new DuplicateElementException("Name already exists in list: " + peer.getName());
        if (stubInList(peer.getStub()))
            throw new DuplicateElementException("Stub already exists in list: " + peer.getStub());
        list.add(peer);
    }

    public void add(String name, String IPaddr, PeerInterface stub) throws DuplicateElementException {
        PeerInfo peer = new PeerInfo(name, IPaddr, stub);
        add(peer);
    }

    public void remove(PeerInfo peer) throws NoSuchElementException {
        if (!list.contains(peer))
            throw new NoSuchElementException("Element not in list: " + peer.toString());
        list.remove(peer);
    }

    public String remove(PeerInterface stub) throws NoSuchElementException {
        PeerInfo peer = getElementByStub(stub);
        if (peer != null) {
            remove(peer);
            return peer.getName();
        }
        else
            throw new NoSuchElementException("backupsystem.agents.Peer not in list: " + stub.toString());
    }

    /*
     * ----------- Utility methods -----------
     */

    public boolean nameInList(String name) {
        for (PeerInfo p : list) {
            if (p.getName().equals(name))
                return true;
        }
        return false;
    }

    public boolean stubInList(PeerInterface stub) {
        for (PeerInfo p : list) {
            if (p.getStub().equals(stub))
                return true;
        }
        return false;
    }

    public PeerList getListWithoutPeer(String name) {
        PeerList newList = new PeerList(list);
        PeerInfo peer = getElementByName(name);
        newList.remove(peer);
        return newList;
    }
}
