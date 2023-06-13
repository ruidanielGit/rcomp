import java.io.*;
import java.util.Arrays;

/**
 * This class implements the SBP protocol.
 */
public class SBPMessage {
    private int version;
    private int code;
    private int dLength1;
    private int dLength2;
    private byte[] data;

    public SBPMessage(int version, int code, int dLength, byte[] data) {
        this.version = version;
        this.code = code;
        this.dLength1 = dLength >> 8;
        this.dLength2 = dLength & 0xFF;
        this.data = data;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        dataOutputStream.writeByte(version);
        dataOutputStream.writeByte(code);
        dataOutputStream.writeByte(dLength1);
        dataOutputStream.writeByte(dLength2);
        if (data != null) {
            dataOutputStream.write(data);
        }

        return outputStream.toByteArray();
    }

    public int getVersion() {
        return version;
    }

    public int getCode() {
        return code;
    }

    public int getDataLength() {
        return (dLength1 + 256 * dLength2); //according to the protocol
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "SBPMessage{" +
                "version=" + version +
                ", code=" + code +
                ", dLength1=" + dLength1 +
                ", dLength2=" + dLength2 +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
