import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class implements a TCP client that connects to a server.
 */
class TcpChatCli {
    static InetAddress serverIP;
    static Socket sock;

    public static void main(String args[]) throws Exception {
        byte[] data = new byte[300];

        if (args.length != 1) {
            System.out.println("Server IPv4/IPv6 address or DNS name is required as argument");
            System.exit(1);
        }

        try {
            serverIP = InetAddress.getByName(args[0]);
        } catch (UnknownHostException ex) {
            System.out.println("Invalid server: " + args[0]);
            System.exit(1);
        }

        try {
            sock = new Socket(serverIP, 9999);
        } catch (IOException ex) {
            System.out.println("Failed to connect.");
            System.exit(1);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream sOut = new DataOutputStream(sock.getOutputStream());

        // Start a thread to read incoming messages from the server
        Thread serverConn = new Thread(new SBPClientConnection(sock, sOut));
        serverConn.start();

        // Send COMMTEST request to start a session
        SBPMessage commTestMessage = new SBPMessage(1, 0, 0, null); // CODE 0 for COMMTEST
        sOut.write(commTestMessage.serialize()); // send the message to the server (SBP protocol)

        System.out.println("Enter your nickname: ");
        String nickName = in.readLine();

        while (true) {
            String input = in.readLine();

            if (input.equals("exit")) {
                // Send DISCONN request to end the session
                SBPMessage disconnMessage = new SBPMessage(1, 1, 0, null); // CODE 1 for DISCONN
                sOut.write(disconnMessage.serialize());
                break; // break the loop
            } else if (input.equals("SharedBoard")) {
                SBPMessage getSharedBoard = new SBPMessage(1, 6, 0, null); // CODE 6 for the SharedBoard
                sOut.write(getSharedBoard.serialize());
                sOut.flush();
            } else {
                System.out.print("Sending message to server ");
                String text = "(" + nickName + ")" + " " + input;
                // Send POST_TEXT request
                byte textLength = (byte) text.length();
                SBPMessage postTextMessage = new SBPMessage(1, 5, textLength, text.getBytes()); // CODE 5 for POST_TEXT
                sOut.write(postTextMessage.serialize());
            }
        }
        serverConn.join(); // wait for the server connection thread to finish
        sock.close(); // close the socket
    }
}

/**
 * This class implements the thread that handles the communication with the server.
 */
class SBPClientConnection implements Runnable {
    private Socket socket;
    private DataInputStream sIn;
    private DataOutputStream sOut;

    public SBPClientConnection(Socket tcp_s, DataOutputStream sOut) {
        socket = tcp_s; // initialize socket
        this.sOut = sOut;
    }

    public void run() {
        try {
            sIn = new DataInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(sIn));

            while (true) {
                byte[] header = new byte[4]; // header means the first 4 bytes of a message
                sIn.read(header, 0, 4); // read the header from the input stream that comes from the server when a message is sent


                int code = header[1]; // get the code
                int dataLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);// get the data length

                byte[] data = new byte[dataLength]; // create a byte array for the data
                sIn.read(data, 0, dataLength); // read the data

                SBPMessage message = new SBPMessage(header[0], code, dataLength, data);

                if (message.getCode() == 1) { // CODE 1 for DISCONN from SBP protocol
                    System.out.println("DISCONNECT: Disconnected from the server...");

                } else if (message.getCode() == 2) { // CODE 2 for ACK (acknowledgement) from SBP protocol
                    System.out.println("COMMTEST: Successfully connected to server...");

                } else if (message.getCode() == 3) { // CODE 3 for ERROR from SBP protocol
                    String errorMessage = new String(message.getData());
                    System.out.println("ERR: " + errorMessage);

                } else if (message.getCode() == 5) { // CODE 5 for POST_TEXT from SBP protocol
                    String text = new String(message.getData());
                    System.out.println("DATA: " + text);
                } else if (message.getCode() == 6) {
                    String text = new String(message.getData());
                    System.out.println("DATA: " + text);
                }
            }
        } catch (IOException ex) {
            System.out.println("Client disconnected.");
        }
    }
}
