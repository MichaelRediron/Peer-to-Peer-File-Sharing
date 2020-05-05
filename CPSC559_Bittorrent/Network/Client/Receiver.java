package Network.Client;

import Network.NetworkStatics;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;


public class Receiver extends Thread {

    private DatagramSocket socket;
    private volatile boolean shutdown;
    private Slave slave;

    public Receiver(Slave slave, DatagramSocket socket) {
        //init passed in slave object and socket
        this.socket = socket;
        this.shutdown = false;
        this.slave = slave;
    }

    public void run() {
        try {
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while (!this.shutdown) //shuts down when shutdown called from slave
        {
            byte[] bytes = new byte[NetworkStatics.MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            try {
                // wait for packet, or 1 second. If a packet is received, process it via Slave.processPacket().
                socket.receive(packet);
                byte[] nout = new byte[packet.getLength()];
                System.arraycopy(bytes, 0, nout, 0, nout.length);
//				System.out.println("RECEIVED PACKET OF LENGTH " + bytes.length);
                slave.processPacket(nout);
            } catch (SocketTimeoutException e) {
//				System.err.println("socket timeout waiting for packet in receiver...");
                break;
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Shut down this thread, rather than wait for the timeout
     */
    public void shutdown() {
        this.shutdown = true;
    }

}
