package to_base_on;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class implements a simple TCP chat client.
 * The client connects to a server whose IP address (or DNS name) is given as
 * argument in the command line.
 * The client reads messages from the console and sends them to the server.
 * The client also reads messages from the server and prints them in the console window.
 * The client terminates when the user types "exit".
 */

class TcpChatCli {
    static InetAddress serverIP; // server's IP address
    static Socket sock; // socket used to communicate with the server

    public static void main(String args[]) throws Exception {
        String nick, frase;
        byte[] data = new byte[300];

        if (args.length != 1) {
            System.out.println(
                    "Server IPv4/IPv6 address or DNS name is required as argument");
            System.exit(1);
        }

        /*
         * The InetAddress class represents an Internet Protocol (IP) address.
         * The static method getByName() of the InetAddress class returns an InetAddress object
         * encapsulating the IP address of the host identified by the argument.
         */
        try {
            serverIP = InetAddress.getByName(args[0]); // get server's IP address
        } catch (UnknownHostException ex) { // if ip address is not valid
            System.out.println("Invalid server: " + args[0]);
            System.exit(1);
        }

        /*
         * The Socket class represents a socket, i.e. an endpoint for communication between two machines.
         * The constructor of the Socket class creates a socket connected to the specified server.
         */
        try {
            sock = new Socket(serverIP, 9999);
        } catch (IOException ex) { // if connection fails
            System.out.println("Failed to connect.");
            System.exit(1);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream sOut = new DataOutputStream(sock.getOutputStream());

        System.out.println("Connected to server");
        System.out.print("Enter your nickname: ");
        nick = in.readLine();

        // start a thread to read incoming messages from the server
        Thread serverConn = new Thread(new TcpChatCliConn(sock));
        serverConn.start(); // the thread will terminate when the server closes the connection


        while (true) { // read messages from the console and send them to the server
            frase = in.readLine(); // read a line from the console
            if (frase.compareTo("exit") == 0) {
                sOut.write(0); // send a message to the server to close the connection
                break;
            }
            frase = "(" + nick + ") " + frase;
            data = frase.getBytes(); // convert the string to an array of bytes
            sOut.write((byte) frase.length()); // send the message length
            sOut.write(data, 0, (byte) frase.length()); // send the message content (the byte array) to the server
        }

        serverConn.join(); // wait for the thread to terminate
        sock.close(); // close the socket
    }
}

/**
 * This class implements a thread that reads messages from the server and prints them in the console window.
 * The thread terminates when the server closes the connection.
 * The thread uses a shared DataInputStream to read messages from the server.
 * The DataInputStream is accessed in mutual exclusion.
 * The thread uses a shared Socket to communicate with the server.
 */
class TcpChatCliConn implements Runnable {
    private Socket s; // shared Socket
    private DataInputStream sIn; // shared DataInputStream

    public TcpChatCliConn(Socket tcp_s) {
        s = tcp_s;
    }

    public void run() {
        int nChars; // number of characters in a message
        byte[] data = new byte[300]; // array to store the message content (the byte array)
        String frase; // message content (the string)

        try {
            sIn = new DataInputStream(s.getInputStream()); // get the input stream from the socket
            while (true) {
                nChars = sIn.read(); // read the message length
                if (nChars == 0) // if the message length is 0, the server closed the connection
                    break;
                sIn.read(data, 0, nChars); // read the message content (the byte array)
                frase = new String(data, 0, nChars); // convert the byte array to a string
                System.out.println(frase); // print the message in the console window
            }
        } catch (IOException ex) { // if an error occurs while reading the message
            System.out.println("Client disconnected.");
        }
    }
}




