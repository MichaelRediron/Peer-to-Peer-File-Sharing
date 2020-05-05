package Network.Client;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.BlockingQueue;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Arrays;

public class FileThread implements Runnable {

    protected BlockingQueue<byte[]> queue = null;
    private int numChunks;
    private String filename;

    public FileThread(BlockingQueue<byte[]> queue, String filename, int numChunks) {
        this.queue = queue;
        this.numChunks = numChunks;
        this.filename = filename;
    }

    public void run() {
        try {
            final RandomAccessFile file = new RandomAccessFile(filename, "rw");
            final FileChannel channel = file.getChannel();
            int count = 0;

            while (count != numChunks) //once chunks written to file = number of chunks requested stop, might change this depending on drop file implementation
            {
                byte[] chunk = queue.take(); // chunk has format "bytestart|message" ex) "0this is file data"
                byte[] bytestart = Arrays.copyOfRange(chunk, 0, 4); //extracts byte start position
                byte[] payload = Arrays.copyOfRange(chunk, 4, chunk.length); //extracts message
                int position = ByteBuffer.wrap(bytestart).getInt(); //convert byte to int
                channel.position(position); //specify position to write to.
                channel.write(ByteBuffer.wrap(payload, 0, payload.length)); //write message to position
                count++;
            }
            file.close();
            channel.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}

