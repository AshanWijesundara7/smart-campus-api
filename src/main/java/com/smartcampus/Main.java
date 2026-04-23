package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

public class Main {

    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        ResourceConfig config = new ResourceConfig()
                .packages(
                    "com.smartcampus.resource",
                    "com.smartcampus.exception",
                    "com.smartcampus.filter"
                );

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        System.out.println("==============================================");
        System.out.println("  Smart Campus API is running!");
        System.out.println("  Base URL : http://localhost:8080/api/v1");
        System.out.println("  Discovery: GET http://localhost:8080/api/v1/");
        System.out.println("  Rooms    : GET http://localhost:8080/api/v1/rooms");
        System.out.println("  Sensors  : GET http://localhost:8080/api/v1/sensors");
        System.out.println("==============================================");
        System.out.println("Press ENTER to stop...");
        System.in.read();
        server.shutdownNow();
    }
}
