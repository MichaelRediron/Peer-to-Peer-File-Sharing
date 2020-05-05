package Network;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MD5hash {

    /**
     * Compare two byte arrays that represent hashes, and returns true if they match
     * @param hash first hash to match
     * @param hash2 second hash to match
     * @return True if hashes match, False otherwise
     */
    public boolean compareHash(byte[] hash, byte[] hash2) {
        return Arrays.equals(hash, hash2);
    }


    /**
     * For a filename, return the hash of the file.
     * @param filename name of file to hash
     * @return md5 hash of the file
     * @throws NoSuchAlgorithmException MD5 hashing exception
     * @throws IOException File reading exception
     */
    public byte[] getHashFile(String filename) throws NoSuchAlgorithmException, IOException {
        //URL url = getClass().getResource(filename);
        Path p = Paths.get(filename);
        byte[] b = Files.readAllBytes(p);
        return hashBytes(b);
    }

    /**
     * Hash an input array of bytes.
     * @param b input array to hash with md5
     * @return hasn of input array
     * @throws NoSuchAlgorithmException MD5 hashing exception
     */
    public byte[] hashBytes(byte[] b) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("MD5").digest(b);
        return hash;
    }

    /**
     * Print a hash to terminal.
     * @param hash hash to print
     */
    public void printHash(byte[] hash) {
        System.out.println(this.hashBytesToString(hash));
    }

    /**
     * Reformat a byte array containing an md5 hash to a string.
     * @param hash input hash to convert
     * @return string representing hash
     */
    public String hashBytesToString(byte[] hash) {
        BigInteger num = new BigInteger(1, hash);
        String hashout = num.toString(16);
        while (hashout.length() < 32)
            hashout += "0";
        return hashout;
    }
}
