package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private int port;
    private String name;
    private Set<PeerInfo> connectedPeers;
    private static final int DISCOVERY_PORT = 8888;


    public Peer(String name,int port){
        this.name = name;
        this.port = port;
        this.connectedPeers = ConcurrentHashMap.newKeySet();
        try {
            serverSocket = new ServerSocket(port);
            discoverySocket = new DatagramSocket(DISCOVERY_PORT);
            discoverySocket.setBroadcast(true);
        } catch (IOException e) {
            System.err.println("Erreur lors de la création des sockets: " + e.getMessage());
        }
    }

    public void startPeer(){
        new Thread(this::listenForConnections).start();
        new Thread(this::startDiscoveryService).start();

        broadcastPresence();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Peer " + name + " démarré sur le port " + port);

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

    private void startDiscoveryService()
}
