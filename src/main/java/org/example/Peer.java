package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private MulticastSocket multicast;
    private int port;
    private String name;
    private Set<PeerInfo> connectedPeers;
    private static final int DISCOVERY_PORT = 8888;
    private int discoveryUDPport ;
    private String GROUP = "224.0.0.1";
    private int PORT_GROUP = 5555;


    public Peer(String name,int port,int udpPort){
        this.name = name;
        this.port = port;
        this.connectedPeers = ConcurrentHashMap.newKeySet();
        this.discoveryUDPport=udpPort;
        try {
            serverSocket = new ServerSocket(port);


            discoverySocket = new DatagramSocket(discoveryUDPport);


            InetAddress group = InetAddress.getByName(GROUP);
            multicast = new MulticastSocket(PORT_GROUP);

            multicast.setTimeToLive(1);
            multicast.joinGroup(group);

        } catch (IOException e) {
            System.err.println("Erreur lors de la création des sockets: " + e.getMessage());
        }
    }


    public void startPeer(){
        new Thread(this::listenForConnections).start();
        new Thread(this::startDiscoveryService).start();
        new Thread(this::multicastReceiver).start();

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
                    broadcastDisconnection();
                    return;
            }
        }
    }

    private void startDiscoveryService(){
        byte[] buffer = new byte[1024];

        while (true){
            try{
                DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
                discoverySocket.receive(packet);

                String message =new String(packet.getData(),0,packet.getLength());

                handleDiscoveryMessage(message, packet.getAddress().getHostAddress());
            }catch (IOException e) {
                System.err.println("Erreur dans le service de découverte: " + e.getMessage());
            }
        }
    }
    public void multicastReceiver(){

        System.out.println("hhhhhheeeeeeeelllllo");
        byte[] buffer = new byte[1024];

        while (true){
            try{
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicast.receive(packet);

                String message =new String(packet.getData(),0,packet.getLength());

                System.out.println("________________________");
                System.out.println(message);
                System.out.println("________________________");

                handleMulticastRecevierMessage(message, packet.getAddress().getHostAddress());
            }catch (IOException e) {
                System.err.println("Erreur dans le service de découverte: " + e.getMessage());
            }
        }

    }

    private void sendUnicastPresenceUDP(String targetIp,int peerUDPPort, int discoveryUDPport) {
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

    public void handleMulticastRecevierMessage(String message, String sourceIp){
        String[] parts = message.split("\\|");


        if (parts.length >= 4){
            String type = parts[0];
            String peerName = parts[1];
            int peerPort = Integer.parseInt(parts[2]);
            int peerPortUDP = Integer.parseInt(parts[3]);

            if (type.equals("HELLO")){
                PeerInfo newPeer = new PeerInfo(sourceIp, peerPort, peerName,peerPortUDP);
                if (!newPeer.equals(new PeerInfo(getLocalIp(), port, name,discoveryUDPport))){
                    connectedPeers.add(newPeer);
                    System.out.println("\nNouveau pair découvert: " + newPeer);
                    sendUnicastPresenceUDP(sourceIp,peerPortUDP,discoveryUDPport);
                }else if (type.equals("BYE")) {
                    connectedPeers.removeIf(peer ->
                            peer.ip.equals(sourceIp) && peer.port == peerPort);
                    System.out.println("\nPair déconnecté: " + peerName);
                }
            }

        }
    }

    private void handleDiscoveryMessage(String message, String sourceIp){
        String[] parts = message.split("\\|");

        if (parts.length >= 4){
            String type = parts[0];
            String peerName = parts[1];
            int peerPort = Integer.parseInt(parts[2]);
            int reWolcomePortUDP = Integer.parseInt(parts[3]);
            if (type.equals("HELLO")){
                PeerInfo newPeer = new PeerInfo(sourceIp, peerPort, peerName,reWolcomePortUDP);
                if (!newPeer.equals(new PeerInfo(getLocalIp(), port, name,reWolcomePortUDP))){
                    connectedPeers.add(newPeer);
                    System.out.println("\nNouveau pair découvert xxxxx: " + newPeer);
                }else if (type.equals("BYE")) {
                    connectedPeers.removeIf(peer ->
                            peer.ip.equals(sourceIp) && peer.port == peerPort);
                    System.out.println("\nPair déconnecté: " + peerName);
                }
            }
        }
    }

    private void broadcastPresence() {
        String message = "HELLO|" + name + "|" + port + "|" + discoveryUDPport;
        System.out.println(message);
        try {
            InetAddress group = InetAddress.getByName(GROUP);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT_GROUP);

            multicast.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur lors du broadcast: " + e.getMessage());
            e.printStackTrace();
        }
    }


//    private void sendUnicastPresence(String targetIp) {
//        String message = "HELLO|" + name + "|" + port;
//        try {
//            byte[] buffer = message.getBytes();
//            InetAddress target = InetAddress.getByName(targetIp);
//            DatagramPacket packet = new DatagramPacket(
//                    buffer, buffer.length, target, DISCOVERY_PORT);
//            discoverySocket.send(packet);
//        } catch (IOException e) {
//            System.err.println("Erreur lors de l'envoi unicast: " + e.getMessage());
//        }
//    }

    private void broadcastDisconnection() {
        String message = "BYE|" + name + "|" + port;
        try {
            byte[] buffer = message.getBytes();
            InetAddress broadcast = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, broadcast, DISCOVERY_PORT);
            discoverySocket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur lors du broadcast de déconnexion: " + e.getMessage());
        }
    }

    private void showConnectedPeers() {
        if (connectedPeers.isEmpty()) {
            System.out.println("Aucun pair connecté.");
            return;
        }
        System.out.println("\nPairs connectés:");
        int i = 1;
        for (PeerInfo peer : connectedPeers) {
            System.out.println(i++ + ". " + peer);
        }
    }

    private void selectAndConnectToPeer(Scanner scanner) {
        showConnectedPeers();
        System.out.print("Sélectionnez le numéro du pair: ");
        int selection = scanner.nextInt();
        scanner.nextLine();

        PeerInfo[] peers = connectedPeers.toArray(new PeerInfo[0]);
        if (selection > 0 && selection <= peers.length) {
            PeerInfo selectedPeer = peers[selection - 1];
            connectToPeer(selectedPeer.ip, selectedPeer.port);
        } else {
            System.out.println("Sélection invalide.");
        }
    }

    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    private void listenForConnections() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleConnection(clientSocket)).start();
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
}
