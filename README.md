# Fault tolerant backup system

The project consists of implementing a prototype of a distributed fault-tolerant system for backing up files.
Each node, in addition to accessing its own files, keeps a copy of the files of the other nodes, so if a node stops working, the backup is not lost because there will be several copies on the other nodes.

## Agents

### Central server

The central server's task is to keep track of the nodes registered on the network.
It has a list of nodes with their names, IP addresses, and other useful metadata.
The server provides the list of nodes to new nodes that want to register on the network.
When a node is added or removed from the network, the server sends the updated list to all registered nodes.

### Peer node

Each node has a list of nodes, which is provided by the server every time a new node subscribes or unsubscribes from the network.
In this way, each node knows not only the IP address of the server, but also the IP addresses of the other nodes.
When the server sends the updated list of nodes, the node reacts to the event by updating its own copy of the list.

Each node can perform the following actions (invoked by the user):

- Network registration: once the server's IP address is provided, the node contacts it to be added to the list and to obtain a copy.
- Backing up a file: sends a copy of the file to one or more nodes directly, using their IP address.
- Recovering lost files: the node requests a copy of its files from all other nodes (contacting them directly) and keeps the most recent one.
- Viewing backup status: shows which nodes have copies of the requesting node's files by making direct requests to the nodes.
- Unsubscribing from the network: the node deletes all backups of files from other nodes that it has and notifies the server of the unsubscription.

## Network operation

After receiving the list of nodes from the server, each node knows the IP address of all other nodes and can contact them directly without going through the server.
The resulting network topology is a complete graph with bidirectional edges.

## Run

To run the system, run the Server class once, then the Peer class for each peer that you want to have in the system. 
