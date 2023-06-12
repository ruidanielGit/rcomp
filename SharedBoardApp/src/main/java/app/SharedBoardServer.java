package app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * This class implements a simple TCP chat server.
 * The server listens to port 9999.
 * When a client connects, the server starts a thread to handle the connection.
 * The server also starts a thread to read messages from the console and send them to all clients.
 * The server terminates when the user types "exit".
 * The server uses a HashMap to store the list of connected clients.
 * The HashMap is shared by the server thread and the client threads.
 * The HashMap is accessed in mutual exclusion.
 */

class SharedBoardServer {

    private static HashMap<Socket, DataOutputStream> cliList = new HashMap<>(); // list of connected clients

    /**
     * Sends a message to all connected clients.
     *
     * @param len  number of characters in the message
     * @param data array with the message content (the byte array)
     * @throws Exception if an error occurs while sending the message
     */
    public static synchronized void sendToAll(int len, byte[] data) throws Exception {
        for (DataOutputStream cOut : cliList.values()) {
            cOut.write(len);
            cOut.write(data, 0, len);
        }
    }

    /**
     * Adds a new client to the list of connected clients.
     * The client is identified by the socket used to communicate with the client.
     *
     * @param s socket used to communicate with the client
     * @throws Exception if an error occurs while adding the client
     */
    public static synchronized void addCli(Socket s) throws Exception {
        cliList.put(s, new DataOutputStream(s.getOutputStream()));
    }

    /**
     * Removes a client from the list of connected clients.
     *
     * @param s socket used to communicate with the client
     * @throws Exception if an error occurs while removing the client
     */
    public static synchronized void remCli(Socket s) throws Exception {
        cliList.get(s).write(0);
        cliList.remove(s);
        s.close();
    }


    private static ServerSocket sock; // socket used to listen to new client connection requests

    public static void main(String args[]) throws Exception {
        int i;

        try {
            sock = new ServerSocket(9999); // create a socket to listen to port 9999
        } catch (IOException ex) {
            System.out.println("Local port number not available.");
            System.exit(1);
        }

        while (true) {
            Socket s = sock.accept(); // wait for a new client connection request
            addCli(s); // add the client to the list of connected clients
            Thread cli = new SharedBoardSrvClient(s); // create a thread to handle the client
            cli.start(); // start the thread
        }
    }
}

class SharedBoardSrvClient extends Thread {
    private Socket myS; // socket used to communicate with the client
    private DataInputStream sIn; // input stream

    public SharedBoardSrvClient(Socket s) {
        myS = s;
    }

    public void run() {
        int nChars; // number of characters in the message
        byte[] data = new byte[300]; // array with the message content (the byte array)

        try {
            sIn = new DataInputStream(myS.getInputStream()); // get the input stream corresponding to the socket myS, used to receive data from the client

            while (true) {
                nChars = sIn.read(); // read the number of characters in the message
                if (nChars == 0)
                    break; // empty line means client wants to exit
                sIn.read(data, 0, nChars);// offset 0 read nChars bytes from the input stream
                // PROBLEM: the message is also being sent to the client that sent the message
                SharedBoardServer.sendToAll(nChars, data); // send the message to all clients
            }
            // the client wants to exit
            SharedBoardServer.remCli(myS);
        } catch (Exception ex) {
            System.out.println("Error");
        }
    }
}


