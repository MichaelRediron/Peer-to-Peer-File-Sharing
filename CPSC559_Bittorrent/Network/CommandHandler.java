package Network;

import java.util.Arrays;

/**
 * COMMAND STRUCTURE
 * 00: Heartbeat
 * 01: Heartbeat reply
 * 05: Request seeder
 * 06: Return seeder
 * 10: Request file
 * 11: Send file chunk
 * 12: Resend file chunk
 * 20: Ready to seed
 * 22: New file
 * 23: New leader found
 * 24: Call election
 */
public class CommandHandler {
    private int[] cmdlen;

    public CommandHandler() {
        initcmdlen();
    }

    /**
     * Given a command number, get the length of data that should follow the command. -2 means invalid command,
     * -1 means variable length command
     *
     * @param cmd command
     * @return expected length of command
     */
    public int getCmdLen(int cmd) {
        return this.cmdlen[cmd];
    }


    /**
     * Init the command length array, used in tokenizing and generating packets below.
     * -1 denotes variable length,
     * -3 denotes do not include length,
     * any positive number indicates a static length.
     */
    private void initcmdlen() {
        this.cmdlen = new int[256];
        Arrays.fill(this.cmdlen, -2);
        this.cmdlen[0] = 0;
        this.cmdlen[1] = 0;
        this.cmdlen[3] = -1;
        this.cmdlen[5] = -3;
        this.cmdlen[6] = -1;
        this.cmdlen[10] = -1;
        this.cmdlen[11] = -1;
        this.cmdlen[12] = -1;
        this.cmdlen[20] = -1;
        this.cmdlen[24] = -1;
        this.cmdlen[23] = -1;
        this.cmdlen[25] = -1;
        this.cmdlen[26] = -1;
        this.cmdlen[44] = -1;
        this.cmdlen[45] = -1;
        this.cmdlen[46] = -1;
    }

    /**
     * Parse incoming data into a command and its data. Throws the "UnsupportedOperationException" if the command
     * is invalid.
     *
     * @param data
     * @return
     */
    public byte[][] tokenizepacket(byte[] data) {
        byte[][] parsed = new byte[2][]; // create 2d array containing command number then data
        parsed[0] = Arrays.copyOfRange(data, 0, 4); // get command from data
        int cmd = NetworkStatics.byteArrayToInt(data);
        int lencmd = this.getCmdLen(cmd); // check command length
        if (lencmd == -3) {
            parsed[1] = Arrays.copyOfRange(data, 4, data.length);
        } else if (lencmd == -2) { // if invalid, throw exception
            throw new UnsupportedOperationException("Command not available.");
        } else if (lencmd == -1) { // if variable length, read next 4 bytes as length integer
            lencmd = NetworkStatics.byteArrayToInt(data, 4);
            parsed[1] = Arrays.copyOfRange(data, 8, lencmd + 16);
        } else { // otherwise use static length
            parsed[1] = Arrays.copyOfRange(data, 4, lencmd + 4);
        }
        return parsed;
    }

    /**
     * Generate the header for a command
     * @param cmd command number for packet
     * @param data data to put in packet
     * @return packet to send
     */
    public byte[] generatePacket(int cmd, byte[] data) {
        int len = this.getCmdLen(cmd);
        byte[] cmdbytes = NetworkStatics.intToByteArray(cmd);
        byte[] output;
        if (len == -2) {
            throw new UnsupportedOperationException("Command not available.");
        } else if (len == -1) {
            len = data.length; // get variable data length (data length + cmd bytes + len bytes)
            output = new byte[data.length + 8]; // create array of appropriate size
            System.arraycopy(cmdbytes, 0, output, 0, 4); // copy cmd bytes to output
            byte[] lenbytes = NetworkStatics.intToByteArray(len); // get byte array of length integer
            System.arraycopy(lenbytes, 0, output, 4, 4); // copy length bytes to output
            System.arraycopy(data, 0, output, 8, len); // copy data to output
        } else {
            if (data.length != len) {
                throw new IllegalArgumentException("data length does not match cmd length requirement!");
            } else {
                output = new byte[data.length + 4]; // get variable data length (data length + cmd bytes)
                System.arraycopy(cmdbytes, 0, output, 0, 4); // copy cmd bytes to output
                System.arraycopy(data, 0, output, 4, len); // copy data to output
            }
        }
        return output;
    }

    /**
     * Append data to the end of an already generated packet.
     * @param original previous packet
     * @param append data to append
     * @return new packet with data appended & length recalculated
     */
    public byte[] appendToGeneratedPacket(byte[] original, byte[] append) {
        int len = NetworkStatics.byteArrayToInt(original, 4);
        int newlen = len + append.length;
        if (newlen > NetworkStatics.MAX_PACKET_SIZE) {
            throw new UnsupportedOperationException("Cannot append data to create a packet larger than DatagramSocket can send");
        }
        byte[] newlenbytes = NetworkStatics.intToByteArray(newlen);
        System.arraycopy(newlenbytes, 0, original, 4, 4);
        byte[] newpacket = new byte[newlen];
        System.arraycopy(original, 0, newpacket, 0, original.length);
        System.arraycopy(append, 0, newpacket, original.length, append.length);
        return newpacket;
    }
}
