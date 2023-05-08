package com.popugaevvn;

import com.popugaevvn.services.CacheServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;


public class App {

    private static final int PORT = 8000;

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(PORT)
                .addService(new CacheServiceImpl())
                .build();

        server.start();
        System.out.println("Server start on port " + PORT);

        server.awaitTermination();
    }
}
