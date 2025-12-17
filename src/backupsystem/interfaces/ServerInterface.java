package backupsystem.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Questa interfaccia contiene i metodi invocabili da un host remoto

public interface ServerInterface extends Remote {
    String subscribePeer(PeerInterface caller) throws RemoteException;
    void unsubscribePeer(PeerInterface caller) throws RemoteException;
}
