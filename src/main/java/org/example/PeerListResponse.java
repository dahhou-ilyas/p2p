package org.example;

import java.util.List;

public class PeerListResponse {
    private List<PeerInfo> peers;

    public PeerListResponse(List<PeerInfo> peers) {
        this.peers = peers;
    }

    public List<PeerInfo> getPeers() {
        return peers;
    }

    public void setPeers(List<PeerInfo> peers) {
        this.peers = peers;
    }
}