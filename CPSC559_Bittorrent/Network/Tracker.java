package Network;

import Controller.Node;
import Network.GlobalTracker.HeartbeatThread;
import Network.GlobalTracker.Pulsable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public class Tracker implements Pulsable {

    private ArrayList<String> peerList;
    private String fileName;
    private String leader;
    private Node node;
    private boolean isLeader;
    private HeartbeatThread<Tracker> beat;

    public Tracker(ArrayList<String> peerList, String fileName, String leader, Node node) {
        this(peerList, fileName, leader, node, false);
    }

    public Tracker(ArrayList<String> peerList, String fileName, String leader, Node node, boolean isLeader) {
        this.peerList = peerList;
        this.fileName = fileName;
        this.leader = leader;
        this.node = node;
        this.isLeader = isLeader;
        this.beat = new HeartbeatThread<>(this, node.getIP());
        if (this.isLeader) {
            this.beat.start();
        }
    }

    /**
     * A method to add a new peer to current peerList
     *
     * @param newPeer ip address
     */
    public void addPeerData(String newPeer) {
        this.peerList.add(newPeer);
    }

    /**
     * A method to delete a peer from current peerList
     *
     * @param deletePeer ip address
     */
    public void deletePeerData(String deletePeer) {
        this.peerList.remove(deletePeer);
    }

    /**
     * A method to update leader's ip address
     *
     * @param newLeader ip address
     */
    public void updateLeader(String newLeader) {
        deletePeerData(this.leader);
        this.leader = newLeader;

        this.isLeader = this.leader.equals(this.node.getIP());
        if (this.isLeader) {
            if (!this.beat.running) {
                this.beat.start();
            }
        } else {
            this.beat.finish();
        }
    }

    public ArrayList<String> getPeerList() {
        return this.peerList;
    }

    /**
     * A method to get the current leader's ip address
     *
     * @return ip address
     */
    public String getLeader() {
        return this.leader;
    }

    public String getFileName() {
        return this.fileName;
    }

    @Override
    public String[] getConnectedNodes() {
        return this.peerList.toArray(new String[]{});
    }

    @Override
    public void deleteNode(String address) {
        this.deletePeerData(address);

        String data = this.fileName + "," + address;
        byte[] msg = new CommandHandler().generatePacket(26, data.getBytes());
        try {
            DatagramSocket socket = new DatagramSocket();
            for (String peer : this.peerList) {
                DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName(peer), NetworkStatics.SERVER_CONTROL_RECEIVE);
                socket.send(packet);
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
