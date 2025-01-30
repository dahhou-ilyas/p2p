package org.example;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Peer <name> <port>");
            return;
        }

        String name = args[0];
        int port = Integer.parseInt(args[1]);

        Peer peer = new Peer(name, port);
        peer.startPeer();
    }
}