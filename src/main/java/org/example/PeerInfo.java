package org.example;

import java.util.Objects;

public class PeerInfo {
    String ip;
    int port;
    String name;

    public PeerInfo(String ip, int port, String name) {
        this.ip = ip;
        this.port = port;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return port == peerInfo.port && ip.equals(peerInfo.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
