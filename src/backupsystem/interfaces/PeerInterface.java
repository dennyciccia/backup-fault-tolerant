package backupsystem.interfaces;

import backupsystem.datastructures.PeerList;
import backupsystem.exceptions.CallerNotSubscribedException;

import java.io.File;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.AbstractMap;

// Questa interfaccia contiene i metodi invocabili da un host remoto

public interface PeerInterface extends Remote {
    void updatePeerList(PeerList list) throws RemoteException;
    void backupFile(File file, byte[] fileData, PeerInterface owner) throws IOException, CallerNotSubscribedException;
    AbstractMap.SimpleImmutableEntry<File,byte[]> getBackedUpFile(String fileName, PeerInterface owner) throws IOException, CallerNotSubscribedException;
    String[] showBackedUpFiles(PeerInterface owner) throws IOException, CallerNotSubscribedException;
    void checkAlive() throws RemoteException;
}