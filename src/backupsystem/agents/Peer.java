package backupsystem.agents;

import backupsystem.datastructures.PeerInfo;
import backupsystem.datastructures.PeerList;
import backupsystem.interfaces.PeerInterface;
import backupsystem.interfaces.ServerInterface;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.regex.Pattern;

public class Peer extends UnicastRemoteObject implements PeerInterface {
    private static final String ROOT_FILE_DIR = "files";
    private static final String BACKUP_DIR = "backups";
    private static final String LOCAL_FILES_DIR = "local_files";

    private PeerList peerList;
    private String name;
    private boolean subscribed;
    private String serverIP;
    private ServerInterface serverStub;

    public Peer() throws RemoteException {
        super();
        peerList = null;
        name = null;
        subscribed = false;
        serverIP = null;
        serverStub = null;
    }

    /*
     * ----------- Getters -----------
     */

    public boolean isSubscribed() {
        return subscribed;
    }

    /*
     * ----------- Remote methods -----------
     */

    // invoked by server, update local peer list
    @Override
    public void updatePeerList(PeerList list) throws RemoteException {
        this.peerList = list;
    }

    // invoked by a peer, stores received file as owner's property
    @Override
    public void backupFile(File file, byte[] fileData, PeerInterface owner) throws IOException {
        String ownerName = peerList.getElementByStub(owner).getName();
        Path backupPath = Paths.get(ROOT_FILE_DIR + File.separator + this.name + File.separator + BACKUP_DIR + File.separator + ownerName + File.separator + file.getName());
        Files.createDirectories(backupPath.getParent());
        Files.write(backupPath, fileData);
    }

    // invoked by a peer, returns owner's specified file
    @Override
    public AbstractMap.SimpleImmutableEntry<File,byte[]> getBackedUpFile(String fileName, PeerInterface owner) throws IOException {
        String ownerName = peerList.getElementByStub(owner).getName();
        Path ownerFilePath = Paths.get(ROOT_FILE_DIR + File.separator + this.name + File.separator + BACKUP_DIR + File.separator + ownerName + File.separator + fileName);
        File fileDescriptor;
        byte[] fileData;

        if (Files.isRegularFile(ownerFilePath)) {
            // get file descriptor and read file bytes
            fileDescriptor = ownerFilePath.toFile();
            fileData = readFileFromDisk(fileDescriptor);
        } else {
            return null;
        }

        return new AbstractMap.SimpleImmutableEntry<>(fileDescriptor, fileData);
    }

    // invoked by a peer, returns the list of the owner's stored file names
    @Override
    public String[] showBackedUpFiles(PeerInterface owner) throws IOException {
        String ownerName = peerList.getElementByStub(owner).getName();
        List<String> fileNames;
        Path ownerBackupPath = Paths.get(ROOT_FILE_DIR + File.separator + this.name + File.separator + BACKUP_DIR + File.separator + ownerName);

        // take all the file names in the owner's directory
        if (Files.isDirectory(ownerBackupPath)) {
            try (var stream = Files.list(ownerBackupPath)) {
                fileNames = stream.filter(Files::isRegularFile).map(p -> p.getFileName().toString()).toList();
            }
        } else {
            fileNames = new ArrayList<>();
        }

        return fileNames.toArray(new String[0]);
    }

    /*
     * ----------- Local methods -----------
     */

    private void subscribeToServer(String serverIP) throws MalformedURLException, NotBoundException, RemoteException {
        ServerInterface stub = (ServerInterface) Naming.lookup("rmi://" + serverIP + "/Server");
        String obtainedName = stub.subscribePeer(this);
        // if everything goes right
        serverStub = stub;
        name = obtainedName;
        this.serverIP = serverIP;
        subscribed = true;
    }

    private void unsubscribeFromServer() throws IOException {
        serverStub.unsubscribePeer(this);

        // delete all backed up files of other peers
        Path localBackupPath = Paths.get(ROOT_FILE_DIR + File.separator + this.name + File.separator + BACKUP_DIR);
        if (Files.exists(localBackupPath)) {
            try (var stream = Files.walk(localBackupPath)) {
                //.sorted(Comparator.reverseOrder())
                stream.filter(Files::isRegularFile).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        subscribed = false;
    }

    private void backup(File file, int[] nodes, PeerList peerListWithoutSelf) throws IOException {
        // read file
        byte[] fileData = readFileFromDisk(file);

        // backup file
        for (int node : nodes) {
            PeerInfo peer = peerListWithoutSelf.get(node);
            peer.getStub().backupFile(file, fileData, this);
        }
    }

    private void recoverFile(String fileName) throws IOException {
        PeerList peerListWithoutSelf = getPeerListWithoutSelf();
        List<AbstractMap.SimpleImmutableEntry<File,byte[]>> filesFormPeers = new ArrayList<>();

        // get all backup copies of that file
        for (PeerInfo peerInfo : peerListWithoutSelf) {
            AbstractMap.SimpleImmutableEntry<File,byte[]> file = peerInfo.getStub().getBackedUpFile(fileName, this);
            if (file != null) {
                filesFormPeers.add(file);
            }
        }

        // find the most recent file
        AbstractMap.SimpleImmutableEntry<File,byte[]> latestFile = null;
        for (var file : filesFormPeers) {
            if (latestFile == null || file.getKey().lastModified() > latestFile.getKey().lastModified()) {
                latestFile = file;
            }
        }

        // write the most recent file
        if (latestFile != null) {
            Path localFilesPath = Paths.get(ROOT_FILE_DIR + File.separator + this.name + File.separator + LOCAL_FILES_DIR + File.separator + latestFile.getKey().getName());
            Files.createDirectories(localFilesPath.getParent());
            Files.write(localFilesPath, latestFile.getValue());
        } else {
            throw new FileNotFoundException("No peer has that file");
        }
    }

    private Map<String,String[]> retrieveBackedUpFilesList() throws IOException {
        PeerList peerListWithoutSelf = getPeerListWithoutSelf();
        Map<String,String[]> peerWithFiles = new HashMap<>();

        // associate each other peer to its files
        for (PeerInfo peerInfo : peerListWithoutSelf) {
            String[] files = peerInfo.getStub().showBackedUpFiles(this);
            peerWithFiles.put(peerInfo.getName(), files);
        }

        return peerWithFiles;
    }

    /*
     * ----------- Utility methods -----------
     */

    public static boolean validateIPaddress(final String ip) {
        Pattern pattern = Pattern.compile("^((([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5]))|(localhost)$");
        return pattern.matcher(ip).matches();
    }

    private int showMenuSubscribed(Scanner scanner) {
        int choice;

        System.out.println("----- Fault Tolerant Backup System -----\n");

        System.out.println("Subscribed to server " + serverIP + " with name " + this.name + "\n");
        System.out.println("1. Backup a file");
        System.out.println("2. Recovery lost file");
        System.out.println("3. Show backed up files");
        System.out.println("4. Unsubscribe from server");

        System.out.print("Enter your choice: ");
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            choice = -1;
        }
        System.out.print("\n");

        return choice;
    }

    private int showMenuNotSubscribed(Scanner scanner) {
        int choice;

        System.out.println("----- Fault Tolerant Backup System -----\n");

        System.out.println("Not subscribed to any server");
        System.out.println("1. Subscribe to server");
        System.out.println("0. Close");

        System.out.print("Enter your choice: ");
        try {
            choice = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            choice = -1;
        }
        System.out.print("\n");

        return choice;
    }

    public PeerList getPeerListWithoutSelf() {
        return peerList.getListWithoutPeer(this.name);
    }

    private void showPeerList(PeerList peerListWithoutSelf) {
        System.out.println("Peer list:");
        for (int i=0; i<peerListWithoutSelf.size(); i++) {
            System.out.println(i + ": " + peerListWithoutSelf.get(i).getName() + " (" + peerListWithoutSelf.get(i).getIPAddress() + ")");
        }
        System.out.println();
    }

    private byte[] readFileFromDisk(File file) throws IOException {
        byte[] fileData;

        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int n;

            while ((n = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }

            fileData = baos.toByteArray();
        }

        return fileData;
    }

    /*
     * ----------- Main -----------
     */

    public static void main() {
        Peer thisPeer;
        try {
            thisPeer = new Peer();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // Interface
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            if (thisPeer.isSubscribed()) {
                choice = thisPeer.showMenuSubscribed(scanner);

                switch(choice) {
                    // backup a file
                    case 1: {
                        // check if there are peers
                        PeerList peerListWithoutSelf = thisPeer.getPeerListWithoutSelf();
                        if (!peerListWithoutSelf.isEmpty()) {
                            try {
                                // ask which file
                                System.out.print("Enter file name: ");
                                String fileName = scanner.nextLine();
                                System.out.print("\n");
                                File file = new File(fileName);
                                if (!file.exists())
                                    throw new FileNotFoundException(fileName);

                                // ask which peer(s)
                                thisPeer.showPeerList(peerListWithoutSelf);
                                System.out.print("Enter peer IDs on which backup the selected file: ");
                                int[] nodes = Arrays.stream(scanner.nextLine().split("(,|;| +)")).mapToInt(Integer::parseInt).toArray();
                                System.out.print("\n");
                                for (int node : nodes)
                                    if (node < 0 || node >= peerListWithoutSelf.size())
                                        throw new IndexOutOfBoundsException("Peer IDs must be between 0 and " + (peerListWithoutSelf.size() - 1));

                                // backup
                                thisPeer.backup(file, nodes, peerListWithoutSelf);
                                System.out.print("File backed up on nodes: ");
                                for (int i = 0; i < nodes.length; i++) {
                                    System.out.print(nodes[i]);
                                    if (i != nodes.length - 1) System.out.print(", ");
                                }
                                System.out.println();

                            } catch (FileNotFoundException e) {
                                System.err.println("File not found: " + e.getMessage());
                            } catch (IndexOutOfBoundsException e) {
                                System.err.println("Error: " + e.getMessage());
                            } catch (IOException e) {
                                System.err.println("Error in backup: " + e.getMessage());
                            } catch (NumberFormatException e) {
                                System.err.println("Peer IDs must be integers: " + e.getMessage());
                            }
                        } else {
                            System.out.println("There are no peers on which backup");
                        }
                    }
                    break;

                    // recovery backed up files
                    case 2: {
                        // ask which file
                        System.out.print("Enter file name to recover: ");
                        String fileName = scanner.nextLine();
                        System.out.print("\n");

                        // try to recover file
                        try {
                            thisPeer.recoverFile(fileName);
                            System.out.println("File recovered");
                        } catch (IOException e) {
                            System.err.println("Error in recovering file: " + e.getMessage());
                        }
                    }
                    break;

                    // show backed up files
                    case 3: {
                        Map<String,String[]> peerWithFiles;
                        try {
                            peerWithFiles = thisPeer.retrieveBackedUpFilesList();

                            System.out.println("Files backed up on other peers:");

                            // for each peer print its files
                            for (String key : peerWithFiles.keySet()) {
                                System.out.println("- " + key + ":");
                                for (String value : peerWithFiles.get(key)) {
                                    System.out.println("\t- " + value);
                                }
                            }
                        } catch (IOException e) {
                            System.err.println("Error in retrieving list of backed up files: " + e.getMessage());
                        }
                    }
                    break;

                    // unsubscribe from server
                    case 4: {
                        try {
                            thisPeer.unsubscribeFromServer();
                            System.out.println("Unsubscribed from server");
                        } catch (IOException e) {
                            System.err.println("Error in unsubscribing from server: " + e.getMessage());
                        }
                    }
                    break;

                    default: {
                        System.err.println("Invalid choice");
                    }
                }
            } else {
                choice = thisPeer.showMenuNotSubscribed(scanner);

                if (choice == 1) {
                    // subscribe to server
                    System.out.print("Type server IP address: ");
                    String serverIP = scanner.nextLine();
                    System.out.print("\n");
                    if (validateIPaddress(serverIP)) {
                        try {
                            thisPeer.subscribeToServer(serverIP);
                            System.out.println("Correctly subscribed to server");
                        } catch (Exception e) {
                            System.err.println("Error in subscribing to server: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Invalid IP address");
                    }
                } else if (choice == 0) {
                    System.out.println("Goodbye");
                } else {
                    System.err.println("Invalid choice");
                }
            }
            System.out.print("\n\n");
        } while (choice != 0);

        scanner.close();

        System.exit(0);
    }
}