package Network.Server;

import Controller.Node;
import Network.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class UDPServer extends Thread {

    private final boolean OMISSION_FAILURE_TEST = false;

    private CommandHandler handler;
    private int port;
    private DatagramSocket recvsocket;
    private DatagramSocket sendsocket;
    private DatagramPacket recvpacket;
    private DatagramPacket sendpacket;
    private byte[] buf;
    private Node node;
    private MD5hash hasher;
    private FileManager fm;
    private boolean running = false;
    private NodeList nlist;

    public UDPServer(FileManager man, Node node, int port) {
        this.port = port;
        this.hasher = new MD5hash();
        this.handler = new CommandHandler();
        this.node = node;
        this.nlist = new NodeList();
        try {
            this.buf = new byte[NetworkStatics.MAX_USEABLE_PACKET_SIZE];
            this.recvsocket = new DatagramSocket(this.port);
            this.sendsocket = new DatagramSocket();
            this.fm = man;
        } catch (SocketException e) {
//            System.err.println("Failure initializing datagram socket on port " + this.port + ".");
            e.printStackTrace();
        }
//        System.out.println("Created UDP server on port " + this.port + ".");
    }

    public void run() {
        this.running = true;
        while (this.running) {
//            System.out.println("waiting to receive packet...");
            this.recvpacket = new DatagramPacket(this.buf, this.buf.length);
            try {
                this.recvsocket.receive(this.recvpacket); // wait until we get some data
                this.buf = this.recvpacket.getData(); // put data into byte buffer
                byte[] trimmed = new byte[this.recvpacket.getLength()];
                System.arraycopy(this.buf, 0, trimmed, 0, trimmed.length);
                byte[][] parsed = this.handler.tokenizepacket(trimmed); // tokenize data
                int cmd = NetworkStatics.byteArrayToInt(parsed[0]); // parse first 4 bytes to integer
                // Debug print statements
//                System.out.println("Command number: " + cmd);
//                System.out.println("Command length: " + parsed[1].length); // print data length
//                NetworkStatics.printPacket(trimmed, "PACKET");

                // based off command number, do an action.
                switch (cmd) {
                    case 0: // heartbeat
//                        System.out.println("Request for heartbeat from " + this.recvpacket.getAddress() + ":" + this.recvpacket.getPort());
                        byte[] outCmd = this.handler.generatePacket(1, new byte[]{});
                        DatagramPacket outPacket = new DatagramPacket(outCmd, outCmd.length, this.recvpacket.getAddress(), this.recvpacket.getPort());
                        this.sendsocket.send(outPacket);
                        break;
                    case 5: // file query
                        // parsed[1] only contains filename, nothing else
                        String filename = new String(parsed[1], 0, 32).trim();
//                        System.out.println("5) Asking for '" + filename + "'");
                        int nodeMode = this.node.checkTrackers(filename);
//                        System.out.println("5) Case #" + nodeMode);
                        byte[] myIP = InetAddress.getByName(this.node.getIP()).getAddress();
                        // return different data dependent on if I am the head tracker, tracker, non-tracker
                        switch (nodeMode) {
                            case 0: // I am the head tracker
                                // send back filesize, hash, myIP, peerlist
                                ArrayList<String> peerlist = this.node.getPeerListFromTracker(filename);
                                byte[] peerlistbytes = new byte[peerlist.size() * 4];
                                for (int i = 0; i < peerlist.size(); i++) {
                                    byte[] addr = InetAddress.getByName(peerlist.get(i)).getAddress();
                                    System.arraycopy(addr, 0, peerlistbytes, i * 4, 4);
                                }
                                byte[] fileLength = NetworkStatics.intToByteArray((int) this.fm.getFilesize(filename));

                                RandomAccessFile raf = this.fm.getFile(filename.getBytes());
                                byte[] filedatatohash = new byte[(int) raf.length()];
                                raf.readFully(filedatatohash);
                                byte[] filehash = this.hasher.hashBytes(filedatatohash);
                                int outsize = fileLength.length + myIP.length + filehash.length + peerlistbytes.length + 4;
                                byte[] outData = new byte[outsize];

                                System.arraycopy(NetworkStatics.intToByteArray(45), 0, outData, 0, 4);
                                System.arraycopy(fileLength, 0, outData, 4, 4);
                                System.arraycopy(filehash, 0, outData, 8, 16);
//                                System.out.println("5) IP: " + InetAddress.getByAddress(myIP).getHostAddress());
                                System.arraycopy(myIP, 0, outData, 24, 4);
                                System.arraycopy(peerlistbytes, 0, outData, 28, peerlistbytes.length);

                                this.sendpacket = new DatagramPacket(outData, outData.length, this.recvpacket.getAddress(), this.recvpacket.getPort());
                                this.sendsocket.send(this.sendpacket);
                                break;
                            case 1: // I am a tracker
                                // send head tracker info if we know who the head tracker is, but we are not the head tracker
                                // current syntax: cmd:

                                byte[] headtrackerIP = InetAddress.getByName(this.node.getLeader(filename)).getAddress();
                                byte[] headtrackerOut = new byte[headtrackerIP.length + myIP.length];

                                System.arraycopy(headtrackerIP, 0, headtrackerOut, 0, headtrackerIP.length);
                                System.arraycopy(myIP, 0, headtrackerOut, headtrackerIP.length, myIP.length);
                                byte[] sendHeadTracker = this.handler.generatePacket(44, headtrackerOut);

                                this.sendpacket = new DatagramPacket(sendHeadTracker, sendHeadTracker.length, this.recvpacket.getAddress(), this.recvpacket.getPort());
                                this.sendsocket.send(this.sendpacket);
                                break;
                            case 2: // I am not a tracker
                                // call query "recursively"
                                // return whatever is returned to me
                                String[] nodes = this.nlist.getNodes();
                                ArrayList<String> nodesAlist = new ArrayList<>();
                                Collections.addAll(nodesAlist, nodes);
                                QueryNodes query = new QueryNodes(this.buf, nodesAlist);
                                byte[] returnedData = query.fileQuery();
                                this.sendpacket = new DatagramPacket(returnedData, returnedData.length, this.recvpacket.getAddress(), this.recvpacket.getPort()); // TODO TODO TODO TODO INCREMENT ME SOMEHOW
                                this.sendsocket.send(this.sendpacket);
                                break;
                            default:
//                                System.err.println("this should not happen!");
                                break;
                        }
                        break;
                    case 10: // file request
                        int startindex10 = NetworkStatics.byteArrayToInt(parsed[1], 0);
                        int endindex10 = NetworkStatics.byteArrayToInt(parsed[1], 4);
                        byte[] name10 = Arrays.copyOfRange(parsed[1], 8, parsed[1].length);
                        InetAddress requesterip = this.recvpacket.getAddress();
                        int length10 = endindex10 - startindex10 + 1;

//                        System.out.println("10) " + startindex10 + " to " + endindex10);

                        RandomAccessFile toget = this.fm.getFile(name10);
                        byte[] datatosend10 = new byte[length10];
                        toget.seek(startindex10);
                        int bytesread = toget.read(datatosend10, 0, length10);
                        toget.close();
//                        System.out.println("10) " + bytesread + " of " + length10);

                        if (bytesread != length10) {
//                            System.err.println("File bytes read is not equal to bytes requested to be read!\n" +
//                                    "Expected: " + length10 + "   Actual:" + bytesread + ".");
                        }

                        // split the datatosend array so it can fit into 64k udp packets
                        byte[][] splitdata = NetworkStatics.chunkBytes(datatosend10, NetworkStatics.FILECHUNK_SIZE);

                        for (int i = 0; i < splitdata.length; i++) {
//                            System.out.println("Sending chunk " + i);
                            if (!OMISSION_FAILURE_TEST || !(i == 0)) {
                                byte[] output = new byte[splitdata[i].length + 20];
                                System.arraycopy(NetworkStatics.intToByteArray(i), 0, output, 0, 4);
                                byte[] datahash = this.hasher.hashBytes(splitdata[i]);
                                System.arraycopy(datahash, 0, output, 4, 16);
                                System.arraycopy(splitdata[i], 0, output, 20, splitdata[i].length);
                                byte[] tosend = this.handler.generatePacket(11, output);
                                DatagramPacket sendPacket = new DatagramPacket(tosend, tosend.length, requesterip, this.recvpacket.getPort());
                                this.sendsocket.send(sendPacket);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } // else "drop" the packet
                        }
                        break;
                    case 12: // re-request file chunk
                        int startindex12 = NetworkStatics.byteArrayToInt(parsed[1], 0);
                        int endindex12 = NetworkStatics.byteArrayToInt(parsed[1], 4);
                        int chunknumber12 = NetworkStatics.byteArrayToInt(parsed[1], 8);
                        byte[] name12 = Arrays.copyOfRange(parsed[1], 12, parsed[1].length);
                        InetAddress requesterip12 = this.recvpacket.getAddress();
//                        System.out.println("Command 12 called: chunk # " + chunknumber12);

                        byte[] returnedData12 = this.generateFilePacket(name12, startindex12, endindex12, chunknumber12);
                        this.sendpacket = new DatagramPacket(returnedData12, returnedData12.length, requesterip12, this.recvpacket.getPort());
                        this.sendsocket.send(this.sendpacket);
                        break;
                    case 20: // node ready to seed
                        String fileName2 = new String(parsed[1]).trim();
//                        System.out.println("20) " + fileName2);
                        InetAddress newSeederIP = this.recvpacket.getAddress();

                        node.addPeerToTracker(fileName2, newSeederIP.getHostAddress());
                        ArrayList<String> pList = node.getPeerListFromTracker(fileName2);

                        String fIP20 = fileName2 + "," + newSeederIP.getHostAddress();
                        byte[] tosend20 = this.handler.generatePacket(25, fIP20.getBytes());

                        for (String p : pList) {
                            DatagramPacket data = new DatagramPacket(tosend20, tosend20.length, InetAddress.getByName(p), NetworkStatics.SERVER_CONTROL_RECEIVE);
                            this.sendsocket.send(data);
                        }
                        break;
                    case 23: // update head tracker for a file
                        String fileNameIP = new String(parsed[1]).trim();
                        String[] d = fileNameIP.split(",");
                        node.updateLeader(d[0], d[1]); //d[0] should be the file name and d[1] should be the ip of the leader
                        break;
                    case 24: // call election for a file
                        String fileName = new String(parsed[1]).trim();

                        node.deletePeerFromTracker(fileName, node.getLeader(fileName));
                        ArrayList<String> peerList = node.getPeerListFromTracker(fileName);

//                        System.out.println("PEER LIST FOR ELECTION");
//                        System.out.println("size: " + peerList.size());
//                        for (int i = 0; i < peerList.size(); i++) {
//                            System.out.println(peerList.get(i));
//                        }
//                        System.out.println("PEER LIST END");

                        String newLeader = Leadership.election(peerList);
                        node.updateLeader(fileName, newLeader);

                        String fIP = fileName + "," + newLeader;
//                        System.out.println("String to send: " + fIP);

                        byte[] tosend24 = this.handler.generatePacket(23, fIP.getBytes());
                        for (String peer : peerList) {
                            DatagramPacket data = new DatagramPacket(tosend24, tosend24.length, InetAddress.getByName(peer), NetworkStatics.SERVER_CONTROL_RECEIVE);
                            this.sendsocket.send(data);
                        }
                        InetAddress requesterIP24 = this.recvpacket.getAddress();
                        int requesterPort24 = this.recvpacket.getPort();
                        byte[] addr = InetAddress.getByName(newLeader).getAddress();
                        DatagramPacket data24 = new DatagramPacket(addr, addr.length, requesterIP24, requesterPort24);
                        this.sendsocket.send(data24);
                        break;
                    case 25: // add seeder for a file
//                        System.out.println("25) ");

                        String fileIP = new String(parsed[1]).trim();
                        String[] data = fileIP.split(",");

                        node.addPeerToTracker(data[0], data[1]);
                        break;
                    case 26: // delete seeder for a file
                        String fileIP1 = new String(parsed[1]).trim();
                        String[] data1 = fileIP1.split(",");

                        node.deletePeerFromTracker(data1[0], data1[1]);
                        break;
                    default:
                        // if an invalid command is sent, throw an exception.
                        throw new IllegalStateException("Unexpected value in " + getClass().getName() + " switch statement: " + cmd);
                }
            } catch (UnknownHostException e) {
//                System.out.println("Failed to connect to device...");
                e.printStackTrace();
            } catch (IOException e) {
//                System.err.println("Failure reading data on port " + this.port + " in UDPServer.");
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Generate the packet for command 12, which resends a file chunk.
     * @param name byte array containing filename
     * @param start original start index
     * @param end original end index
     * @param chunknum chunk # to resend
     * @return byte array to send
     * @throws IOException Failure reading file
     * @throws NoSuchAlgorithmException Failure hashing data
     */
    private byte[] generateFilePacket(byte[] name, int start, int end, int chunknum) throws IOException, NoSuchAlgorithmException {
        // calculate chunk start and end indices
        int chunkstartindex = start + (chunknum * (NetworkStatics.FILECHUNK_SIZE));
        int chunkendindex = chunkstartindex + NetworkStatics.FILECHUNK_SIZE;
        int maxchunklen = chunkendindex - chunkstartindex + 1;

        // access file & read selection
        RandomAccessFile toget = this.fm.getFile(name);
        toget.seek(chunkstartindex);
        byte[] filedata = new byte[maxchunklen];
        int bytesread = toget.read(filedata, 0, maxchunklen);
        toget.close();
        if (bytesread == 0) {
            throw new IOException("Failed to read any data during file packet generation...");
        }
        // create output array
        byte[] output = new byte[bytesread + 20];
        // copy over relevant data
        System.arraycopy(NetworkStatics.intToByteArray(chunknum), 0, output, 0, 4);
        byte[] datahash = this.hasher.hashBytes(filedata);
        System.arraycopy(datahash, 0, output, 4, 16);
        System.arraycopy(filedata, 0, output, 20, filedata.length);
        // add length and command number with function
        byte[] tosend = this.handler.generatePacket(11, output);
        return tosend;
    }

    /**
     * Terminate the UDP server.
     */
    public void terminate() {
        this.running = false;
        this.sendsocket.close();
    }

}
