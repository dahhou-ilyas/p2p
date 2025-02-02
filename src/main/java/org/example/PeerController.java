package org.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/peers")
public class PeerController {
    private final Peer peer;

    public PeerController(Peer peer) {
        this.peer = peer;
    }

    @GetMapping("/count")
    public PeerCountResponse getConnectedPeersCount() {
        return new PeerCountResponse(peer.getConnectedPeersCount());
    }

    @GetMapping("/list")
    public PeerListResponse getConnectedPeers() {
        return new PeerListResponse(peer.getConnectedPeersList());
    }
}
