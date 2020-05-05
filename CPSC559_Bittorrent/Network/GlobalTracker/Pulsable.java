package Network.GlobalTracker;

public interface Pulsable {

    // Both below should be synchronized
    String[] getConnectedNodes();

    void deleteNode(String address);
}
