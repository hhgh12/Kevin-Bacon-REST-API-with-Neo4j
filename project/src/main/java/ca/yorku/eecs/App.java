package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Scanner;

import com.sun.net.httpserver.HttpServer;

public class App 
{
    static int PORT = 8080;
    public static void main(String[] args) throws IOException
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        // TODO: two lines of code are expected to be added here
        // please refer to the HTML server example 
        Utils u = new Utils();
	    server.createContext("/api/v1", u::handle);

        server.start();
        

        System.out.printf("Server started on port %d...\n", PORT);
        Neo4j neo = new Neo4j();
        

        
        
    }
}
