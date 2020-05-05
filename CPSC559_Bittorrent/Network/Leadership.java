package Network;

import javax.sound.midi.Track;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.lang.*;
import java.util.Arrays;

public class Leadership {

    /**
     * Given an arraylist of peers, we elect a new leader.
     * @param peerList list of peers to get a new leader from
     * @return leader chosen
     */
    public static String election(ArrayList<String> peerList) {
//        for (int i = 0; i < peerList.size(); i++) {
//            System.out.println("election " + peerList);
//        }
        String newLeader = "";
        Boolean running = false;
        int i = 0;
        int[] arr = new int[peerList/*.get(0)*/.size()];
        Random rand = new Random();

        //Assigning random numbers to the ip addresses
        while (i < peerList.size()) {
            int randNum = rand.nextInt(10000);
            boolean checker = true;
            for (int j = 0; j < arr.length; j++) {
                if (arr[j] == randNum) {
                    checker = false;
                }
            }
            //checking if we get a number that is already in the list
            if (checker) {
                arr[i] = randNum;
                i++;
            }
        }

        int[] l = Arrays.copyOf(arr, arr.length);

        /*Getting the highest id*/
        Arrays.sort(l);
        int p = l[l.length - 1];

        for (i = 0; i < arr.length; i++) {
            if (p == arr[i]) {
//                System.out.println("The new leader is: " + peerList.get(i));
                newLeader = peerList.get(i);
                break;
            }
        }
        return newLeader;
    }
}
