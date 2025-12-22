# Fault tolerant backup system

The project consists of implementing a prototype of a distributed fault-tolerant system for backing up files.
Each peer, in addition to accessing its own files, keeps a copy of the files of the other peers, so if a peer stops working, the backup is not lost because there will be several copies on the other peers.

## Agents

### Central server

The central server's task is to keep track of the peers registered on the network.
It has a list of peers with their names, IP addresses, and other useful metadata.
The server provides the list of peers to new peers that want to register on the network.
When a peer is added or removed from the network, the server sends the updated list to all registered peers.

The central server also checks periodically if subscribed peers are alive, if one of them is not alive then it is unsubscribed from the network.

### Peer node

Each peer has a list of peers, which is provided by the server every time a new peer subscribes or unsubscribes from the network.
In this way, each peer knows not only the IP address of the server, but also the IP addresses of the other peers.
When the server sends the updated list of peers, the peer reacts to the event by updating its own copy of the list.

Each peer can perform the following actions (invoked by the user):

- Network registration: once the server's IP address is provided, the peer contacts it to be added to the list and to obtain a copy.
- Backing up a file: sends a copy of the file to one or more peers directly, using their IP address.
- Recovering lost files: the peer requests a copy of one of its files from all other peers (contacting them directly) and keeps the most recent one.
- Viewing backup status: shows which peers have copies of the requesting peer's files by making direct requests to the peers.
- Unsubscribing from the network: the peer deletes all backups of files from other peers that it has and notifies the server of the unsubscription.

Each time that a peer calls another peer, the callee checks if the caller is subscribed, if it is not then the callee notifies the caller.

## Network operation

After receiving the list of peers from the server, each peer knows the IP address of all other peers and can contact them directly without going through the server.
The resulting network topology is a complete graph with bidirectional edges.

## Run

To run the system, run the Server class once, then the Peer class for each peer that you want to have in the system. 
