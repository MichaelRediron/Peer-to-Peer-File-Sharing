package Network;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

///if cant send to any nodes in message then cant find file response
public class QueryNodes {
    //server will have to check for query command and if does not have file data, init constructor call fileQuery()
    //if does send back cmd(peerlist(44) or headdata(45)) | attach ip incase election | message either head data or peerlist
    private byte[] message;
    private ArrayList<String> nodelist;
    private ArrayList<byte[]> headInfo = new ArrayList<byte[]>();
    private ArrayList<byte[]> peerList = new ArrayList<byte[]>();
    private ArrayList<byte[]> notFound = new ArrayList<byte[]>();

    /**
     * Initialize a query to later call fileQuery() with.
     * @param message message to query with
     * @param nodelist list of nodes to send query to
     */
    public QueryNodes(byte[] message, ArrayList<String> nodelist) {
        this.message = message;
        this.nodelist = nodelist;
    }

    /**
     * Start a filequery with values defined in object constructor.
     * @return Response from query
     * @throws IOException Network I/O exceptions
     */
    public byte[] fileQuery() throws IOException {
//        System.out.println("querying nodes...");
        ArrayList<FileQuery> threadList = new ArrayList<>();
        ArrayList<String> notQueried = new ArrayList<>();
        int count = nodelist.size();

        // format node list into addresses and add to query list if they haven't been queried
        for (String s : nodelist) {
            InetAddress addr = InetAddress.getByName(s);
            if (!addr.isAnyLocalAddress()) {

            }
            byte[] bytes = addr.getAddress();
            for (byte b : bytes)
                b &= 0xFF;
            if (!isQueried(message, bytes)) {
                byte[] out = new byte[message.length + 4];
                out = addip(message, bytes);
                message = new byte[out.length];
                message = Arrays.copyOfRange(out, 0, out.length);
                notQueried.add(s);
            } else
                count--;
        }

        // send queries to all nodes not already queried
        for (int i = 0; i < notQueried.size(); i++) {
            InetAddress addr = InetAddress.getByName(notQueried.get(i));
            boolean tryagain = true;
            while (tryagain) {
                try {
                    FileQuery queryThread = new FileQuery(message, addr, 6080 + i, this);
                    queryThread.start();
                    threadList.add(queryThread);
                    tryagain = false;
                } catch (BindException e) {
                    i++;
                }
            }
        }

        // wait for all queries to return
        for (FileQuery fileQuery : threadList) {
            if (fileQuery.isAlive()) {
                try {
//                    System.out.println("joining queries...");
                    fileQuery.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//        System.out.println("all queries joined...");
        if (notFound.size() == count)
            return ByteBuffer.allocate(4).putInt(46).array();
        else if (!peerList.isEmpty())
            return peerList.get(0);
        else
            return headInfo.get(0);

        //**new if count=notfound array size send back not found
        //start threads, join, processQuery add to arraylist, check, return
    }

    /**
     * Process a returned query
     * @param b Node response from query
     */
    public synchronized void processQuery(byte[] b) {
        byte[] command = Arrays.copyOfRange(b, 0, 4);
        int cmd = ByteBuffer.wrap(command).getInt();
        if (cmd == 44)
            headInfo.add(b);
        else if (cmd == 45)
            peerList.add(b);
        else
            notFound.add(b);
    }

    /**
     * Check if a node has already been queried.
     * @param message message to send
     * @param node Ip address of node as a byte array
     * @return True if node has been queried already, False otherwise
     */
    public boolean isQueried(byte[] message, byte[] node) {
        for (int i = 4; i < message.length; i += 4) {
            byte[] bytes = new byte[4];
            bytes = Arrays.copyOfRange(message, i, i + 4);
            if (Arrays.equals(node, bytes))
                return true;
        }
        return false;
    }

    /**
     * Append the IP of the local machine to the message received so we do not get additional queries
     * @param message original message
     * @param bytes IP address of this node
     * @return new message to query with
     */
    public byte[] addip(byte[] message, byte[] bytes) {
        byte[] out = new byte[message.length + bytes.length];
        System.arraycopy(message, 0, out, 0, message.length);
        System.arraycopy(bytes, 0, out, message.length, bytes.length);
        return out;
    }
}
