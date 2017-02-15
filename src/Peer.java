import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rittick on 2/14/17.
 */
public class Peer {
    // After reading the config file, these fields will be initialized.
    private static String IP;
    private static String PORT;
    private static Integer NUMBER_OF_SERVERS;

    //Stream to read user input.
    private static BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

    //Config file data is stored here.
    private static ConcurrentHashMap<Integer, String> configFileData;

    //Peer ID to Neighbors List
    private static ArrayList<Integer> peerNeighbors = new ArrayList<>();

    //Peer ID and sequence number of Message ID
    private static String peerID;
    private static Integer sequenceNumber = 0;

    public static void main(String[] args) {
        //--------------Getting Peer ID-----------------------------------------
        System.out.println("Enter Peer ID number: (1,2,3,4) ");
        try {
            peerID = userInput.readLine();
        }catch (IOException e){
            System.out.println("Exception while getting peer ID from user.");
        }

        //-----Getting IP Address, Port, Number of peers and Neighbors of each peer.
        configFileData = ReadConfigFile.readFile("network_linear.config");

        for(Integer key : configFileData.keySet()){
            if (key == Integer.parseInt(peerID)) {
                String[] split = configFileData.get(key).split(":");
                String[] neighbors = split[2].split("-");
                for (int i = 0; i< neighbors.length; i++){
                    peerNeighbors.add(Integer.parseInt(neighbors[i]));
                }
            }
        }

        /*for (int j = 0; j < peerNeighbors.size(); j++){
            System.out.println(peerNeighbors.get(j));
        }*/

    }

    private static class Client extends Thread{



        public void run(){

        }
    }

    private static class Server extends Thread{


        public void run(){

        }
    }

}
