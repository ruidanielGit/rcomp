import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class implements a TCP server that listens to a port and accepts new client connections.
 */
public class TcpChatSrv {
    private static List<ClientConnection> cliList = new ArrayList<>(); // list of connected clients
    protected static List<List<String>> msgList = new ArrayList<>(); // list of messages posted by clients
    static int messageCounter = 0;
    static int m = 5;
    static int n = 5;

    public static void main(String args[]) throws Exception {

        ServerSocket srvSock; // socket used to listen to new client connection requests
        Socket connSock; // socket used to exchange data with a connected client
        initializeMessageList();

        try {
            srvSock = new ServerSocket(9999);
        } catch (IOException ex) {
            System.out.println("Local port number not available.");
            System.exit(1);
            return;
        }

        System.out.println("Server started. Waiting for connections...");

        while (true) {
            try {
                connSock = srvSock.accept();
                System.out.println("Client connected: " + connSock.getRemoteSocketAddress());
                ClientConnection clientConn = new ClientConnection(connSock);
                cliList.add(clientConn);
                Thread thread = new Thread(clientConn);
                thread.start();
            } catch (IOException ex) {
                System.out.println("Failed to accept client connection.");
            }
        }
    }

    /**
     * Initializes the list of messages posted by clients.
     */
    public static void initializeMessageList() {
        // initialize the list with empty strings
        for (int i = 0; i < m; i++) {
            List<String> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                row.add("");
            }
            msgList.add(row);
        }
    }

    /**
     * Adds a new message to the list of messages posted by clients.
     * @param text
     */
    public static synchronized void addText(String text) {
        if (messageCounter == m * n) {
            updateListSize();
            initializeMessageList(); // Reinitialize the list with the updated size
        }

        // add to the first available position
        for (int i = 0; i < msgList.size(); i++) {
            for (int j = 0; j < msgList.get(i).size(); j++) {
                if (msgList.get(i).get(j).equals("")) {
                    msgList.get(i).set(j, text);
                    messageCounter++;
                    return;
                }
            }
        }
    }

    /**
     * Updates the size of the list of messages posted by clients.
     */
    public static void updateListSize() {
        int currentSizeN = msgList.size();
        int currentSizeM = msgList.get(0).size();

        int rowsToAdd = 5 - currentSizeN;
        int columnsToAdd = 5 - currentSizeM;

        // update the size variables
        m += rowsToAdd;
        n += columnsToAdd;

        // Add new rows with empty strings
        for (int i = 0; i < rowsToAdd; i++) {
            List<String> newRow = new ArrayList<>(Collections.nCopies(currentSizeM, ""));
            msgList.add(newRow);
        }

        // Add new columns with empty strings to existing rows
        for (List<String> row : msgList) {
            for (int i = 0; i < columnsToAdd; i++) {
                row.add("");
            }
        }
    }

    /**
     * Prints the list of messages posted by clients in lines of 5 elements.
     */
    public static void printList() {
        for (List<String> row : msgList) {
            for (String text : row) {
                System.out.print(text + " ");
            }
            System.out.println();
        }
    }

    /**
     * Returns the list of messages posted by clients.
     *
     * @return
     */
    public synchronized List<List<String>> getMessages() {
        return msgList;
    }

    /**
     * Sends a message to all connected clients.
     * This is used when a client posts a message.
     *
     * @param text
     */
    public static void broadcastText(String text) {
        for (ClientConnection conn : cliList) {
            conn.sendToAll(5, text.getBytes()); // CODE 5 for POST_TEXT
        }
    }

    /**
     * Removes a client from the list of connected clients.
     *
     * @param connection the client connection to remove (the thread)
     */
    public static void removeConnection(ClientConnection connection) {
        cliList.remove(connection);
    }

}

// This class handles the communication with a client.
class ClientConnection implements Runnable {
    private Socket socket; // socket used to exchange data with the client
    private DataInputStream sIn; // input stream
    private DataOutputStream sOut;

    public ClientConnection(Socket sock) {
        socket = sock;
    }

    /**
     * Sends a message to the client, composed of a header and a data part.
     *
     * @param code
     * @param data
     */
    public void sendToAll(int code, byte[] data) {
        try {
            SBPMessage message = new SBPMessage(1, code, data.length, data);
            sOut.write(message.serialize());
        } catch (IOException ex) {
            System.out.println("Failed to send message to client.");
        }
    }

    /**
     * This method is executed when the thread is started.
     * It handles the communication with the client.
     */
    public void run() {
        try {
            sIn = new DataInputStream(socket.getInputStream());
            sOut = new DataOutputStream(socket.getOutputStream());

            // Send SESSION_ESTABLISHED message
            SBPMessage sessionMsg = new SBPMessage(1, 2, 0, null); // CODE 2 for SESSION_ESTABLISHED
            sOut.write(sessionMsg.serialize()); // passes the byte array to the output stream

            System.out.println("Sent SESSION_ESTABLISHED message to client.");

            while (true) {
                byte[] header = new byte[4]; // 4 bytes for the header (version, code, data length)
                sIn.read(header, 0, 4); // read the header from the input stream

                int code = header[1]; // get the code from the header
                int dataLength = (header[2] << 8) | header[3]; // get the data length from the header

                byte[] data = new byte[dataLength];
                sIn.read(data, 0, dataLength); // read the data from the input stream

                switch (code) {
                    case 6: // CODE 6 for GET_TEXT
                        System.out.println("SharedBoard requested.");
                        TcpChatSrv.printList();
                        sendToAll(6, TcpChatSrv.msgList.toString().getBytes()); // CODE 6 for GET_TEXT
                        break;
                    case 5: // CODE 5 for POST_TEXT
                        String text = new String(data);
                        System.out.println("Received text: " + text);
                        TcpChatSrv.broadcastText(text);
                        TcpChatSrv.addText(text); // add the text to the list of messages
                        break;
                    case 1: // CODE 1 for DISCONNECT
                        System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
                        TcpChatSrv.removeConnection(this);
                        return;
                    case 0: // CODE 0 for SESSION_ESTABLISHED
                        System.out.println("Received SESSION_ESTABLISHED message from client.");
                        break;
                    default:
                        System.out.println("Unknown message code: " + code);
                } // end switch
            } // end while
        } catch (IOException ex) {
            System.out.println("Failed to receive/send message to client.");

        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                System.out.println("Failed to close socket.");
            }
        }
    }
}

