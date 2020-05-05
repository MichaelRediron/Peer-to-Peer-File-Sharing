package Network.GlobalTracker;

import Network.NetworkStatics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HeartbeatThread<T extends Pulsable> extends Thread {

    private static DatagramSocket socket;
    public static boolean debug = false;

    private T gt;
    public boolean running;
    private String ownIP;

    public static void init() {
        init(0);
    }

    public static void init(int offset) {
        try {
            socket = new DatagramSocket(NetworkStatics.SERVER_CONTROL_RECEIVE + 48 + offset);
            socket.setSoTimeout(1500);
        } catch (SocketException se) {
            se.printStackTrace();
        }
    }

    public HeartbeatThread(T gt, String ownIP) {
        this.gt = gt;
        this.ownIP = ownIP;
        this.running = false;
    }

    @Override
    public void run() {
        this.running = true;
        while (this.running) {
            String[] nodes = this.gt.getConnectedNodes();
            for (String node : nodes) {
                if (node.equals(this.ownIP)) {
                    continue;
                }
                boolean alive = pulseNode(node);
                if (!alive) {
                    if (debug) System.out.println(">> Removed node " + node);
                    this.gt.deleteNode(node);
                }
            }
            try {
                if (debug) System.out.println(">> Heartbeat sleeping");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void finish() {
        this.running = false;
    }

    private synchronized static boolean pulseNode(String node) {
        if (debug) System.out.println(">> Checking " + node);
        for (int i = 1; i <= 5; i++) {
            try {
                byte[] cmd = ByteBuffer.allocate(4).putInt(0).array();
                DatagramPacket outPacket = new DatagramPacket(cmd, cmd.length, InetAddress.getByName(node), NetworkStatics.SERVER_CONTROL_RECEIVE);
                socket.send(outPacket);

                byte[] inMsg = new byte[NetworkStatics.MAX_PACKET_SIZE];
                DatagramPacket inPacket = new DatagramPacket(inMsg, inMsg.length);
                socket.receive(inPacket);
                int inCmd = NetworkStatics.byteArrayToInt(Arrays.copyOfRange(inMsg, 0, 4));

                if (inCmd == 1) {
                    if (debug) System.out.println("<< Alive " + node);
                    return true;
                }
            } catch (IOException ioe) {
                if (debug) System.out.println(String.format("!! Timeout %d %s", i, node));
            }
        }
        return false;
    }
}
