# p2p

Peers that find each other on a local network without any central registry,
then chat over TCP.

## How it works

- **Announce**: on start, and then every 5 s, a peer sends
  `HELLO|name|tcpPort|udpPort` to the multicast group `224.0.0.1:5555`.
- **Reply**: a peer that hears a newcomer answers it directly (UDP unicast) with
  its own `HELLO`, so the newcomer learns about everyone at once.
- **Registry**: each peer keeps its own list of peers with the time it last heard
  from them. A peer silent for 15 s is removed; a peer that quits sends
  `BYE|name|tcpPort` and is removed at once.
- **Chat**: option 2 of the menu opens a TCP connection to a peer and sends lines;
  the other peer prints them.
- **REST**: `GET /api/peers/count` and `GET /api/peers/list` expose the registry.

## Run

With Docker:

```bash
docker compose up --build
docker attach p2p-peer1-1      # the menu of peer1 (detach: Ctrl-P Ctrl-Q)
curl localhost:8081/api/peers/list
```

Locally (Java 17+, Maven), one terminal per peer:

```bash
mvn package
PEER_NAME=peer1 PEER_PORT=5001 UDP_PORT=8002 SPRING_PORT=8081 java -jar target/p2p-1.0-SNAPSHOT.jar
PEER_NAME=peer2 PEER_PORT=5002 UDP_PORT=8003 SPRING_PORT=8082 java -jar target/p2p-1.0-SNAPSHOT.jar
```
