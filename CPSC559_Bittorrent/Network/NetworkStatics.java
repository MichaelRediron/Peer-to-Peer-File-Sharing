package Network;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkStatics {

    public static int SERVER_CONTROL_RECEIVE = 6051;

    public final static int MAX_PACKET_SIZE = 65028;
    public final static int MAX_USEABLE_PACKET_SIZE = 65000;

    public final static int FILECHUNK_SIZE = 64000;

    /**
     * Formats an integer into a byte array of length 4. Returned array is little endian.
     *
     * @param n input integer
     * @return byte[] array of length 4
     */
    public static byte[] intToByteArray(int n) {
        return ByteBuffer.allocate(4).putInt(n).array();
    }

    /**
     * Get an integer from the first 4 indices of a byte array. Assumes little endian.
     *
     * @param b input byte array
     * @return integer n, stored in b[0..3]
     */
    public static int byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    /**
     * Get an integer from an index of a byte array. Assumes little endian.
     *
     * @param b          input byte array
     * @param startindex the start point of the integer to get
     * @return integer n, stored in b[startindex...startindex+3]
     */
    public static int byteArrayToInt(byte[] b, int startindex) {
        if (b.length < 4) {
            throw new IllegalArgumentException("Input byte array must be big enough to store an integer, in order to cast an integer out of it. (min 4 bytes as input).");
        }
        return ByteBuffer.wrap(Arrays.copyOfRange(b, startindex, startindex + 4)).getInt();
    }

    public static byte[] longToByteArray(long n) {
        return ByteBuffer.allocate(8).putLong(n).array();
    }

    public static long byteArrayToLong(byte[] b) {
        return byteArrayToLong(b, 0);
    }

    public static long byteArrayToLong(byte[] b, int startindex) {
        if (b.length < 8) {
            throw new IllegalArgumentException("Input byte array must be big enough to store a long, in order to cast a long out of it. (min 8 bytes as input).");
        }
        return ByteBuffer.wrap(Arrays.copyOfRange(b, startindex, startindex + 8)).getLong();
    }

    public static InetAddress byteArraytoInetAddress(byte[] b, int startindex) throws UnknownHostException {
        byte[] inetaddr = new byte[4];
        System.arraycopy(b, startindex, inetaddr, 0, 4);
        return InetAddress.getByAddress(inetaddr);
    }

    /**
     * Prints out a packet's data to help debug. Prints out as the hex code, character representing it, and then as a normal string.
     *
     * @param packetdata input byte array
     * @param header     header for print statement
     */
    public static void printPacket(byte[] packetdata, String header) {
        StringBuilder sb = new StringBuilder();
        String sb2 = "";
        String sb3 = "";
        for (byte b : packetdata) {
            sb.append(String.format("%02X  ", b));
            if ((b & 0xFF) == 0x0A) {
                sb2 += "\\n  ";
            } else if ((b & 0xFF) == 0x09) {
                sb2 += "\\t  ";
            } else if ((b & 0xFF) < 0x20) {
                sb2 += "    ";
            } else if ((b & 0xFF) == 0x7F) {
                sb2 += "    ";
            } else {
                sb2 += ((char) b) + "   ";
            }
            sb3 += String.format("%-4s", b & 0xFF);
        }
        System.out.println("=======" + header + "===============================");
        System.out.println(sb.toString());
        System.out.println(sb2);
        System.out.println(sb3);
        System.out.println("=================================================");
    }

    public static String getFilenameFromFilepath(String fp) {
        Pattern pat = Pattern.compile("[^/\\\\]*$");
        Matcher mat = pat.matcher(fp);
        mat.find();
        return mat.group(0);
    }

    /**
     * Splits 1d byte array to 2d byte array for file transfer.
     * Credit to https://stackoverflow.com/a/39788851
     *
     * @param arrayToSplit byte array to split
     * @param chunkSize    max size of each row returned
     * @return
     */
    public static byte[][] chunkBytes(byte[] arrayToSplit, int chunkSize) {
        if (chunkSize <= 0) {
            return null;  // just in case :)
        }
        // first we have to check if the array can be split in multiple
        // arrays of equal 'chunk' size
        int rest = arrayToSplit.length % chunkSize;  // if rest>0 then our last array will have less elements than the others
        // then we check in how many arrays we can split our input array
        int chunks = arrayToSplit.length / chunkSize + (rest > 0 ? 1 : 0); // we may have to add an additional array for the 'rest'
        // now we know how many arrays we need and create our result array
        byte[][] arrays = new byte[chunks][];
        // we create our resulting arrays by copying the corresponding
        // part from the input array. If we have a rest (rest>0), then
        // the last array will have less elements than the others. This
        // needs to be handled separately, so we iterate 1 times less.
        for (int i = 0; i < (rest > 0 ? chunks - 1 : chunks); i++) {
            // this copies 'chunk' times 'chunkSize' elements into a new array
            arrays[i] = Arrays.copyOfRange(arrayToSplit, i * chunkSize, i * chunkSize + chunkSize);
        }
        if (rest > 0) { // only when we have a rest
            // we copy the remaining elements into the last chunk
            arrays[chunks - 1] = Arrays.copyOfRange(arrayToSplit, (chunks - 1) * chunkSize, (chunks - 1) * chunkSize + rest);
        }
        return arrays; // that's it
    }

    /**
     * Return a socket available to bind to, within the range of application ports
     *
     * @return next port number available
     * @throws SocketException if no port is available (max 50 ports)
     */
    public static int getNextAvailablePort() throws SocketException {
        int port = 6051;
        int highBound = 6100;
        while (true) {
            try {
                DatagramSocket sock = new DatagramSocket(port);
                sock.close();
                break;
            } catch (SocketException e) {
                e.printStackTrace();
            }
            port++;
            if (port > highBound) {
                throw new SocketException("Could not find port.");
            }
        }
        return port;
    }
}
