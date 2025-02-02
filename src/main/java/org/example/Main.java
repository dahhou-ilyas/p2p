package org.example;

public class Main {
    public static void main(String[] args) {
        String name = System.getenv("PEER_NAME");   // Récupérer le nom du peer
        if (name == null || name.isEmpty()) {
            System.out.println("Nom du peer manquant dans l'environnement.");
            return;
        }
        String peerPortStr = System.getenv("PEER_PORT");  // Récupérer le port TCP
        if (peerPortStr == null || peerPortStr.isEmpty()) {
            System.out.println("Port du peer manquant dans l'environnement.");
            return;
        }
        int tcpPort = Integer.parseInt(peerPortStr);  // Convertir le port en entier

        String udpPortStr = System.getenv("UDP_PORT");  // Récupérer le port UDP
        if (udpPortStr == null || udpPortStr.isEmpty()) {
            System.out.println("Port UDP manquant dans l'environnement.");
            return;
        }
        int udpPort = Integer.parseInt(udpPortStr);

        Peer peer = new Peer(name, tcpPort,udpPort);
        peer.startPeer();
    }
}