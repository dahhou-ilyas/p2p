package org.example;

import java.util.Objects;

public class PeerInfo {
    String ip;
    int port;
    String name;
    int peerPortUDP;

    public PeerInfo(String ip, int port, String name,int peerPortUDP) {
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.peerPortUDP=peerPortUDP;
    }

    @Override
    public String toString() {
        return name + " (" + ip + ":" + port + ") "+"peerPortUDP : "+peerPortUDP ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo peerInfo = (PeerInfo) o;
        return port == peerInfo.port && Objects.equals(name, peerInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPeerPortUDP() {
        return peerPortUDP;
    }

    public void setPeerPortUDP(int peerPortUDP) {
        this.peerPortUDP = peerPortUDP;
    }
}
