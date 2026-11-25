package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private static final String GROUP = "224.0.0.1";
    private static final int PORT_GROUP = 5555;
    // Every peer re-announces itself periodically; a peer that stays silent
    // for PEER_TIMEOUT_MS (three missed announcements) is considered gone.
    private static final long HEARTBEAT_MS = 5_000;
    private static final long PEER_TIMEOUT_MS = 15_000;

    private final ServerSocket serverSocket;
    private final DatagramSocket discoverySocket;
    private final MulticastSocket multicast;
    private final InetAddress group;
    private final int port;
    private final String name;
    private final int discoveryUDPport;
    // The local registry: every known peer and the last time we heard from it.
    private final Map<PeerInfo, Long> connectedPeers = new ConcurrentHashMap<>();
    private volatile boolean running = true;


    public Peer(String name, int port, int udpPort) throws IOException {
        this.name = name;
        this.port = port;
        this.discoveryUDPport = udpPort;

        serverSocket = new ServerSocket(port);
        discoverySocket = new DatagramSocket(discoveryUDPport);

        group = InetAddress.getByName(GROUP);
        multicast = new MulticastSocket(PORT_GROUP);
        multicast.setTimeToLive(1);
        multicast.joinGroup(group);
    }


    public void startPeer(){
        startBackground(this::listenForConnections);
        startBackground(this::startDiscoveryService);
        startBackground(this::multicastReceiver);
        startBackground(this::heartbeat);

        broadcastPresence();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Peer " + name + " démarré sur le port " + port + "   my UDP Port "+ discoveryUDPport);

        while (true){
            System.out.println("\nOptions:");
            System.out.println("1. Voir les pairs connectés");
            System.out.println("2. Envoyer un message à un pair");
            System.out.println("3. Quitter");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice){
                case 1:
                    showConnectedPeers();
                    break;
                case 2:
                    if (connectedPeers.isEmpty()) {
                        System.out.println("Aucun pair connecté.");
                        break;
                    }
                    selectAndConnectToPeer(scanner);
                    break;
                case 3:
                    stop();
                    return;
            }
        }
    }

    // Daemon threads: they must not keep the JVM alive once the peer has quit.
    private void startBackground(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        if (!running) return;
        broadcastDisconnection();
        running = false;
        multicast.close();
        discoverySocket.close();
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    // Unicast replies (HELLO sent straight to our UDP port by a peer that just heard us).
    private void startDiscoveryService(){
        receiveLoop(discoverySocket, false);
    }

    // Multicast announcements (HELLO / BYE sent to the whole group).
    private void multicastReceiver(){
        receiveLoop(multicast, true);
    }

    private void receiveLoop(DatagramSocket socket, boolean replyToNewPeers) {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                handleMessage(message, packet.getAddress().getHostAddress(), replyToNewPeers);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur dans le service de découverte: " + e.getMessage());
                }
            } catch (RuntimeException e) {
                // One malformed packet must never stop discovery for good.
                System.err.println("Paquet ignoré: " + e.getMessage());
            }
        }
    }

    private void sendUnicastPresenceUDP(String targetIp, int peerUDPPort) {
        String message = "HELLO|" + name + "|" + port + "|" + discoveryUDPport;
        try {
            byte[] buffer = message.getBytes();
            InetAddress target = InetAddress.getByName(targetIp);
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, target, peerUDPPort);

            System.out.println("envoyé wa 3alikom salam");
            discoverySocket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi unicast: " + e.getMessage());
        }
    }

    // HELLO|name|tcpPort|udpPort  or  BYE|name|tcpPort
    private void handleMessage(String message, String sourceIp, boolean replyToNewPeers) {
        String[] parts = message.split("\\|");

        if (parts[0].equals("HELLO") && parts.length >= 4) {
            PeerInfo peer = new PeerInfo(sourceIp, Integer.parseInt(parts[2]), parts[1], Integer.parseInt(parts[3]));
            if (isSelf(peer)) return;

            boolean isNew = connectedPeers.put(peer, System.currentTimeMillis()) == null;
            if (isNew) {
                System.out.println("\nNouveau pair découvert: " + peer);
                // Answer a newcomer directly, so it learns about us without
                // waiting for our next periodic announcement.
                if (replyToNewPeers) {
                    sendUnicastPresenceUDP(sourceIp, peer.peerPortUDP);
                }
            }
        } else if (parts[0].equals("BYE") && parts.length >= 3) {
            PeerInfo peer = new PeerInfo(sourceIp, Integer.parseInt(parts[2]), parts[1], 0);
            if (connectedPeers.remove(peer) != null) {
                System.out.println("\nPair déconnecté: " + peer.name);
            }
        }
    }

    private boolean isSelf(PeerInfo peer) {
        return peer.name.equals(name) && peer.port == port;
    }

    private void heartbeat() {
        while (running) {
            try {
                Thread.sleep(HEARTBEAT_MS);
            } catch (InterruptedException e) {
                return;
            }
            broadcastPresence();
            // Peers that crashed never send BYE: forget them once they go silent.
            long now = System.currentTimeMillis();
            connectedPeers.entrySet().removeIf(entry -> {
                boolean expired = now - entry.getValue() > PEER_TIMEOUT_MS;
                if (expired) {
                    System.out.println("\nPair expiré (plus de nouvelles): " + entry.getKey().name);
                }
                return expired;
            });
        }
    }

    private void broadcastPresence() {
        sendToGroup("HELLO|" + name + "|" + port + "|" + discoveryUDPport);
    }

    // BYE goes to the multicast group, where every peer is listening.
    private void broadcastDisconnection() {
        sendToGroup("BYE|" + name + "|" + port);
    }

    private void sendToGroup(String message) {
        try {
            byte[] buffer = message.getBytes();
            multicast.send(new DatagramPacket(buffer, buffer.length, group, PORT_GROUP));
        } catch (IOException e) {
            if (running) {
                System.err.println("Erreur lors de l'envoi au groupe: " + e.getMessage());
            }
        }
    }

    private void showConnectedPeers() {
        if (connectedPeers.isEmpty()) {
            System.out.println("Aucun pair connecté.");
            return;
        }
        System.out.println("\nPairs connectés:");
        int i = 1;
        for (PeerInfo peer : connectedPeers.keySet()) {
            System.out.println(i++ + ". " + peer);
        }
    }

    private void selectAndConnectToPeer(Scanner scanner) {
        showConnectedPeers();
        System.out.print("Sélectionnez le numéro du pair: ");
        int selection = scanner.nextInt();
        scanner.nextLine();

        PeerInfo[] peers = connectedPeers.keySet().toArray(new PeerInfo[0]);
        if (selection > 0 && selection <= peers.length) {
            PeerInfo selectedPeer = peers[selection - 1];
            connectToPeer(selectedPeer.ip, selectedPeer.port);
        } else {
            System.out.println("Sélection invalide.");
        }
    }

    private void listenForConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                startBackground(() -> handleConnection(clientSocket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Erreur lors de l'acceptation de la connexion: " + e.getMessage());
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Connecté au pair: " + name);

            Scanner scanner = new Scanner(System.in);
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                System.out.println("Message reçu: " + inputLine);
                System.out.print("Répondre (ou 'quit' pour quitter): ");
                String response = scanner.nextLine();

                if ("quit".equalsIgnoreCase(response)) {
                    break;
                }
                out.println(response);
            }

            socket.close();
        } catch (IOException e) {
            System.err.println("Erreur lors de la gestion de la connexion: " + e.getMessage());
        }
    }

    private void connectToPeer(String ip, int peerPort) {
        try {
            Socket socket = new Socket(ip, peerPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String welcome = in.readLine();
            System.out.println(welcome);

            Scanner scanner = new Scanner(System.in);
            String userInput;

            while (true) {
                System.out.print("Entrez votre message (ou 'quit' pour quitter): ");
                userInput = scanner.nextLine();

                if ("quit".equalsIgnoreCase(userInput)) {
                    break;
                }

                out.println(userInput);
                String response = in.readLine();
                System.out.println("Réponse reçue: " + response);
            }

            socket.close();
        } catch (IOException e) {
            System.err.println("Erreur lors de la connexion au pair: " + e.getMessage());
        }
    }

    public int getConnectedPeersCount() {
        return connectedPeers.size();
    }

    public List<PeerInfo> getConnectedPeersList() {
        return new ArrayList<>(connectedPeers.keySet());
    }
}
