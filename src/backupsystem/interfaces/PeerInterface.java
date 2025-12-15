package backupsystem.interfaces;

import backupsystem.datastructures.PeerList;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.AbstractMap;

// Questa interfaccia contiene i metodi invocabili da un host remoto

public interface PeerInterface extends Remote {
    void updatePeerList(PeerList list) throws RemoteException;
    void backupFile(File file, byte[] fileData, PeerInterface owner) throws IOException;
    AbstractMap.SimpleImmutableEntry<File,byte[]> getBackedUpFile(String fileName, PeerInterface owner) throws IOException;
    String[] showBackedUpFiles(PeerInterface owner) throws IOException;
}
