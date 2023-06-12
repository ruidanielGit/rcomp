package app;

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

class SharedBoardClient {
    static InetAddress serverIP; // server's IP address
    static Socket sock; // socket used to communicate with the server

    public static void main(String args[]) throws Exception {
        String nick, frase;

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
        Thread serverConn = new SharedBoardClientConn(sock);
        serverConn.start(); // the thread will terminate when the server closes the connection

        while (true) { // read messages from the console and send them to the server
            frase = in.readLine(); // read a line from the console

            if (frase.equalsIgnoreCase("exit")) {
                sOut.write(new SBPMessage((byte) 0, (byte) 1, (byte) 0, (byte) 0, new byte[0]).toBytes());
                break;
            }

            byte[] data = frase.getBytes(); // convert the string to an array of bytes
            SBPMessage message = new SBPMessage((byte) 0, (byte) 1, (byte) (data.length & 0xFF), (byte) ((data.length >> 8) & 0xFF), data);
            sOut.write(message.toBytes()); // send the SBPMessage to the server
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
class SharedBoardClientConn extends Thread {
    private Socket s; // shared Socket

    public SharedBoardClientConn(Socket tcp_s) {
        s = tcp_s;
    }

    public void run() {
        try {
            SBPMessage message;
            while (true) {
                message = SBPMessage.readFromStream(s.getInputStream()); // read a message from the server
                if (message == null) // server closed the connection
                    break;

                String frase = new String(message.getData()); // convert the byte array to a string
                System.out.println(frase); // print the message
            }
        } catch (IOException ex) { // if an error occurs while reading the message
            System.out.println("Client disconnected.");
        }
    }
}

class SBPMessage {

    private static final int VERSION_OFFSET = 0;
    private static final int CODE_OFFSET = 1;
    private static final int D_LENGTH_1_OFFSET = 2;
    private static final int D_LENGTH_2_OFFSET = 3;
    private static final int DATA_OFFSET = 4; //offset of the data in the bytes array

    private byte version;
    private byte code;
    private byte dLength1;
    private byte dLength2;
    private byte[] data;

    //constructor for received message (from bytes)
    public SBPMessage(byte version, byte code, byte dLength1, byte dLength2, byte[] data) {
        this.version = version;
        this.code = code;
        this.dLength1 = dLength1;
        this.dLength2 = dLength2;
        this.data = data;
    }

    public SBPMessage() {
    }

    public byte getVersion() {
        return version;
    }

    public byte getCode() {
        return code;
    }

    public byte getdLength1() {
        return dLength1;
    }

    public byte getdLength2() {
        return dLength2;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return (dLength1 + 256 * dLength2); //according to the protocol
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public void setdLength1(byte dLength1) {
        this.dLength1 = dLength1;
    }

    public void setdLength2(byte dLength2) {
        this.dLength2 = dLength2;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

//    /**
//     * Converts a byte array to an SBP message object
//     * Used when the server receives a request from the shared board app as a byte array
//     * The server will convert the byte array to an SBP message object in order to process the request
//     *
//     * @param bytes the byte array to be converted
//     * @return the SBP message converted to a byte array
//     */
//    public static SBPMessage fromBytes(byte[] bytes) {
//        if (bytes.length < 4) {
//            throw new IllegalArgumentException("Invalid SBP message format");
//        }
//
//        byte version = bytes[VERSION_OFFSET];
//        byte code = bytes[CODE_OFFSET];
//        byte dLength1 = bytes[D_LENGTH_1_OFFSET];
//        byte dLength2 = bytes[D_LENGTH_2_OFFSET];
//        byte[] data = new byte[bytes.length - DATA_OFFSET];
//        System.arraycopy(bytes, DATA_OFFSET, data, 0, data.length);//copy the data from the bytes array to the data array
//
//        return new SBPMessage(version, code, dLength1, dLength2, data);
//    }

    /**
     * Converts an SBP message object to a byte array
     * Used when the shared board app sends a request to the server
     * The server will receive the request as a byte array and will convert it to an SBP message object
     *
     * @return the SBP message converted to a byte array
     */
    public byte[] toBytes() {
        int messageLength = getDataLength() + DATA_OFFSET;
        byte[] bytes = new byte[messageLength];
        bytes[VERSION_OFFSET] = version;
        bytes[CODE_OFFSET] = code;
        bytes[D_LENGTH_1_OFFSET] = dLength1;
        bytes[D_LENGTH_2_OFFSET] = dLength2;
        System.arraycopy(data, 0, bytes, DATA_OFFSET, data.length);//copy the data from the data array to the bytes array

        return bytes;
    }

    /**
     * Reads an SBP message from an input stream
     * Used when the server receives a request from the shared board app
     * @param inputStream the input stream to read from
     * @return the SBP message read from the stream
     * @throws IOException if an I/O error occurs while reading from the stream
     */
    public static SBPMessage readFromStream(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        byte version = dataInputStream.readByte();
        byte code = dataInputStream.readByte();
        byte dLength1 = dataInputStream.readByte();
        byte dLength2 = dataInputStream.readByte();
        int dataLength = (dLength1 & 0xFF) + ((dLength2 & 0xFF) << 8);
        byte[] data = new byte[dataLength];
        dataInputStream.readFully(data);

        return new SBPMessage(version, code, dLength1, dLength2, data);
    }
}



