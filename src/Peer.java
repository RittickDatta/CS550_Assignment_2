import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

    public static void main(String[] args) {

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
