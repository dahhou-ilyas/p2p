package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        Peer peer = context.getBean(Peer.class);

        // The menu runs on the main thread; the REST API (/api/peers) keeps
        // working in the background. Without a console, the peer and its API
        // simply keep running.
        if (peer.runConsole()) {
            System.exit(SpringApplication.exit(context));
        }
    }

    // The Peer is a Spring bean so PeerController can read its registry.
    @Bean(destroyMethod = "stop")
    public Peer peer() throws IOException {
        Peer peer = new Peer(requireEnv("PEER_NAME"), Integer.parseInt(requireEnv("PEER_PORT")),
                Integer.parseInt(requireEnv("UDP_PORT")));
        peer.start();
        return peer;
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Variable d'environnement manquante: " + key);
        }
        return value;
    }
}
