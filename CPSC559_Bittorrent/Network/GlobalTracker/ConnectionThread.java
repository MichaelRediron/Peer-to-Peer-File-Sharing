package Network.GlobalTracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionThread extends Thread {

    private GlobalTracker gt;
    private boolean running;

    ConnectionThread(GlobalTracker gt) {
        this.gt = gt;
        this.running = false;
    }

    @Override
    public void run() {
        this.running = true;
        try {
            ServerSocket socket = new ServerSocket(1962, 5);
            while (this.running) {
                // wait for incoming connection
                Socket client = socket.accept();
                // set up printwriter for socket I/O
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                // parse incoming lessage
                String msg = in.readLine();
                // unless asking to connect, do nothing
                if (msg.equals("connect")) {
                    StringBuilder s = new StringBuilder();
                    String[] nodes = this.gt.getRandomNodes(3);
                    for (int i = 0; i < nodes.length; i++) {
                        s.append(nodes[i]);
                        if (i != nodes.length - 1) {
                            s.append(",");
                        }
                    }
                    // send list of connected nodes
                    out.println(s.toString());
                    String address = client.getInetAddress().getHostAddress();
                    // add new node to network list
                    this.gt.addNode(address);
                    System.out.println(">> Added " + address);
                }

                in.close();
                out.close();
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Terminate the connection thread.
     */
    void finish() {
        this.running = false;
    }
}
