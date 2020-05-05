package Network;

import java.io.IOException;
import java.net.*;

public class FileQuery extends Thread {

    private InetAddress ip;
    private DatagramSocket udpSocket;
    private byte[] message;
    private QueryNodes query;

    public FileQuery(byte[] message, InetAddress ip, int port, QueryNodes query) throws SocketException {
        this.message = message;
        this.ip = ip;
//        System.out.println("Attempting to create filequery object on port " + port);
        this.udpSocket = new DatagramSocket(port);
//        this.udpSocket.setSoTimeout(2000);
        this.query = query;
    }

    public void run() {
        boolean tryagain = false;
        byte[] bytes;
        DatagramPacket packet;
        do {
            bytes = new byte[NetworkStatics.MAX_PACKET_SIZE];
            packet = new DatagramPacket(message, message.length, ip, NetworkStatics.SERVER_CONTROL_RECEIVE);
            try {
//                System.out.println("Sending packet in filequery");
                udpSocket.send(packet);
                packet = new DatagramPacket(bytes, bytes.length);
//                System.out.println("Waiting to receive packet...");
                udpSocket.receive(packet);
//                System.out.println("packet received.");
                tryagain = false;
            } catch (SocketTimeoutException e) {
                tryagain = true;
            } catch (IOException e) {
                e.printStackTrace();
                tryagain = false;
            }
        } while (tryagain);
//        System.out.println("file successfully queried");
        byte[] nout = new byte[packet.getLength()];
        System.arraycopy(bytes, 0, nout, 0, nout.length);
        query.processQuery(nout);
        udpSocket.close();
    }
}
