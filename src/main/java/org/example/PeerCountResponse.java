package org.example;

public class PeerCountResponse {
    private int count;

    public PeerCountResponse(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}