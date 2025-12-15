package backupsystem.agents;

import backupsystem.datastructures.PeerInfo;
import backupsystem.datastructures.PeerList;
import backupsystem.exceptions.DuplicateElementException;
import backupsystem.interfaces.PeerInterface;
import backupsystem.interfaces.ServerInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.NoSuchElementException;
import java.util.UUID;

public class Server extends UnicastRemoteObject implements ServerInterface {
    private final PeerList peerList;

    public Server() throws RemoteException {
        super();
        this.peerList = new PeerList();
    }

    @Override
    public synchronized String subscribePeer(PeerInterface peerStub) throws RemoteException {
        try {
            // Obtain peer IP address and generate unique name
            String IPAddress = RemoteServer.getClientHost();
            String name = "backupsystem.agents.Peer-" + UUID.randomUUID().toString().substring(0, 11);
            // Add peer to the list
            peerList.add(name, IPAddress, peerStub);
            // Send updated list to each subscribed peer
            sendUpdatedList();
            System.out.println("New peer subscribed: " + name);
            printPeerList();
            // Return peer name to invoker
            return name;
        } catch (ServerNotActiveException e) {
            throw new RuntimeException("backupsystem.agents.Server cannot determine peer IP address");
        } catch (DuplicateElementException e) {
            throw new RuntimeException("backupsystem.agents.Peer already subscribed");
        }
    }

    @Override
    public void unsubscribePeer(PeerInterface peerStub) throws RemoteException {
        try {
            String name = peerList.remove(peerStub);
            sendUpdatedList();
            System.out.println("backupsystem.agents.Peer unsubscribed: " + name);
            printPeerList();
        } catch (NoSuchElementException e) {
            throw new RuntimeException("backupsystem.agents.Peer not subscribed");
        }
    }

    private void sendUpdatedList() {
        for (PeerInfo p : peerList){
            try{
                p.getStub().updatePeerList(peerList);
            } catch (RemoteException e) {
                System.err.println("Cannot update peer list for: " + p);
                System.err.println(e.getMessage());
            }
        }
    }

    private void printPeerList() {
        System.out.println("\nbackupsystem.agents.Peer list:");
        for (PeerInfo p : peerList) {
            System.out.println(p.toString());
        }
        System.out.print("\n");
    }

    public static void main() {
        try {
            Registry rmiRegistry = LocateRegistry.createRegistry(1099);
            System.out.println("RMI Registry created on port 1099");

            Server server = new Server();
            rmiRegistry.rebind("backupsystem.agents.Server", server);
            System.out.println("backupsystem.agents.Server bound to registry as 'backupsystem.agents.Server'");
        } catch (RemoteException e) {
            System.err.println("Error in creating registry or backupsystem.agents.Server binding");
            throw new RuntimeException(e);
        }

        System.out.println("backupsystem.agents.Server is ready ...\n");


        synchronized (Server.class) {
            try {
                Server.class.wait();
            } catch (InterruptedException e) {
                System.out.println("backupsystem.agents.Server interrupted. Goodbye.");
            }
        }
    }
}
