

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rittick on 2/14/17.
 */
public class Peer_New {
    // After reading the config file, these fields will be initialized.
    private static String IP;
    private static Integer PORT;
    private static Integer NUMBER_OF_PEERS;

    //Stream to read user input.
    private static BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

    //Config file data is stored here.
    private static ConcurrentHashMap<Integer, String> configFileData;

    //Peer ID to Neighbors List
    private static ArrayList<Integer> peerNeighbors = new ArrayList<>();

    //Peer ID to IP & PORT
    private static ConcurrentHashMap<Integer, String> peerIdtoIPAndPort = new ConcurrentHashMap<>();

    //Peer ID and sequence number of Message ID
    private static String peerID;
    private static Integer sequenceNumber = 0;
    private static Integer invalidationSequenceNumber = 0; //---------------- NEW

    // List of files of this peer.
    private static ArrayList<String> myFiles = new ArrayList<>();

    //Neighbors to Socket Mappings
    private static ConcurrentHashMap<Integer, Socket> sockets = new ConcurrentHashMap<>();

    //TTL Constant
    private static final Integer TTL = 2;
    private  static final String configFileName = "network_linear.config";

    public static void main(String[] args) {
        //--------------Getting Peer ID-----------------------------------------
        System.out.println("Enter Peer ID number: (1,2,3,4) ");
        try {
            peerID = userInput.readLine();
        } catch (IOException e) {
            System.out.println("Exception while getting peer ID from user.");
        }

        //-----Getting IP Address, Port, Number of peers and Neighbors of each peer.
        configFileData = ReadConfigFile.readFile(configFileName);

        for (Integer key : configFileData.keySet()) {
            if (key == Integer.parseInt(peerID)) {
                // GET NEIGHBORS
                String[] split = configFileData.get(key).split(":");
                String[] neighbors = split[2].split("-");
                for (int i = 0; i < neighbors.length; i++) {
                    peerNeighbors.add(Integer.parseInt(neighbors[i]));
                }

                //GET IP AND PORT
                IP = split[0];
                PORT = Integer.parseInt(split[1]);
            }
        }

        NUMBER_OF_PEERS = configFileData.keySet().size();

        //---------Peer ID to IP & Port---------
        for (Integer key : configFileData.keySet()) {
            String[] split = configFileData.get(key).split(":");
            peerIdtoIPAndPort.put(key, split[0] + ":" + split[1]);
        }

        //-------Preparing list of files belonging to this peer--------------
        myFiles = getFileData("Node" + peerID + "/Myfiles");

        //----------Starting Peer Server---------------------------
        try {
            ServerSocket peerServer = new ServerSocket(PORT);
            System.out.println("Peer Server is listening...");

            //-------Starting Peer Client--------------------------
            new Client(peerID, IP, PORT, NUMBER_OF_PEERS, peerNeighbors, peerIdtoIPAndPort).start();
            System.out.println("Peer Client is running...");

            while (true) {
                Socket newConnection = peerServer.accept();
                new Server(peerID, IP, PORT, NUMBER_OF_PEERS, peerNeighbors, newConnection, peerIdtoIPAndPort).start();
            }
        } catch (IOException e) {
            System.out.println("IOException in main method.");
        }
    }

    private static ArrayList<String> getFileData(String path) {
        ArrayList<String> fileNames = new ArrayList<>();

        File file = new File(path);
        String[] files = file.list();
        for (int i = 0; i < files.length; i++) {
            fileNames.add(files[i]);
        }

        return fileNames;
    }

    private static boolean searchPeerFiles(String fileName) {
        boolean flag = false;

        for (int i = 0; i < myFiles.size(); i++) {
            if (myFiles.get(i).equals(fileName)) {
                flag = true;
                return flag;
            }
        }

        return flag;
    }

    private static class Server extends Thread {
        private String ID_SERVER;
        private String IP_SERVER;
        private Integer PORT_SERVER;
        private Integer numberOfPeers_SERVER;
        private ArrayList<Integer> myNeighbors_SERVER;
        private Socket connection;
        private Socket homeConnection;
        private ConcurrentHashMap<Integer, String> peerIdToIPAndPort_SERVER;
        private ConcurrentHashMap<MessageID, UpstreamPeerID> seenQueries = new ConcurrentHashMap<>();
        private Boolean neighborsFlag = false;


        public Server(String peerID, String ip, Integer port, Integer numberOfPeers, ArrayList<Integer> peerNeighbors, Socket newConnection, ConcurrentHashMap<Integer, String> peerIdtoIPAndPort) {
            ID_SERVER = peerID;
            IP_SERVER = ip;
            PORT_SERVER = port;
            numberOfPeers_SERVER = numberOfPeers;
            myNeighbors_SERVER = peerNeighbors;
            connection = newConnection;
            peerIdToIPAndPort_SERVER = peerIdtoIPAndPort;
        }

        public void run() {
//            System.out.println("In run() of server.");

            ObjectOutputStream outputStream;
            ObjectInputStream inputStream;
            BufferedReader userInput2;
            Integer PORT_SELF = Integer.parseInt("4"+PORT_SERVER.toString().substring(1));

            try {
                inputStream = new ObjectInputStream(connection.getInputStream()); //<----same OOS and OIS for different Sockets
                outputStream = new ObjectOutputStream(connection.getOutputStream());
                homeConnection = connection;
                Iterator<Integer> neighborsIterator = myNeighbors_SERVER.iterator();

                Object o = inputStream.readObject();

                Query_New queryObj;
                if (o instanceof Query_New) {
                    queryObj = (Query_New) o;
                    //System.out.println(queryObj.getType());


                    //Add This Peer ID to Forward Path
                    queryObj.setForwardPath(Integer.parseInt(ID_SERVER));

                    //Decrease TTL by 1
                    queryObj.setTTL();
                    //System.out.println(queryObj.getTTL());

                    if (queryObj.getType().equals("query")) {
                        boolean searchResult = searchPeerFiles(queryObj.getFileName());
                        if (searchResult) {
                            //Update query object
//                            System.out.println("Peer " + ID_SERVER + " Contains File.");
                            queryObj.setSearchResults(IP + ":" + PORT);
                        }

                        // Regardless of Hit or Miss, Forward Query to Neighbor if TTL > 0
                        if (queryObj.getTTL() > 0) {

                            String peerID = queryObj.getMessageID().getPeerID();
                            Integer seqNum = queryObj.getMessageID().getSequenceNumber();
                            Boolean msgSeen = checkIFMsgSeen(peerID, seqNum, seenQueries);


                            if (!msgSeen) {
                                while (neighborsIterator.hasNext()) {
                                    Integer nextNeighbor = neighborsIterator.next();
                                    String ip = peerIdtoIPAndPort.get(nextNeighbor).split(":")[0];
                                    int port = Integer.parseInt(peerIdtoIPAndPort.get(nextNeighbor).split(":")[1]);

                                    seenQueries.put(new MessageID(queryObj.getMessageID().getPeerID(),
                                                    queryObj.getMessageID().getSequenceNumber()),
                                            new UpstreamPeerID(ip, port));
                                    //System.out.println("CHECKPOINT");

                                    sockets.put(nextNeighbor, new Socket(ip, port));

                                    if (!queryObj.getForwardPath().contains(nextNeighbor)) {

                                        outputStream = new ObjectOutputStream(sockets.get(nextNeighbor).getOutputStream()); // Exception, CHECK
                                        outputStream.flush();
                                        outputStream.writeObject(queryObj);
                                        outputStream.flush();

                                    }/*else if(queryObj.getMessageID().getPeerID().equals(nextNeighbor.toString())){
                                        outputStream = new ObjectOutputStream(sockets.get(nextNeighbor).getOutputStream()); // Exception, CHECK
                                        outputStream.flush();
                                        outputStream.writeObject(queryObj);
                                        outputStream.flush();
                                    }*/ else {
                                        neighborsFlag = true;
                                    }
                                }
                            }

                        } else if ((queryObj.getTTL() == 0)) {
                            QueryHit_New queryHitObject = createQueryHitObject(queryObj);


                            if (!queryHitObject.getMessageID().getPeerID().equals(ID_SERVER)) {
                                try {
                                    Integer backwardPath = queryHitObject.getBackwardPath(); // ------EXCEPTION HERE--------

                                    Socket socket = sockets.get(backwardPath);
                                    if (socket == null) {
                                        socket = new Socket(peerIdToIPAndPort_SERVER.get(backwardPath).split(":")[0],
                                                Integer.parseInt(peerIdToIPAndPort_SERVER.get(backwardPath).split(":")[1]));
                                    }
                                    outputStream = new ObjectOutputStream(socket.getOutputStream());
                                    outputStream.flush();
                                    outputStream.writeObject(queryHitObject);
                                    outputStream.flush();
                                } catch (IOException e) {
                                    System.out.println("In Query Hit Section of Server.");
                                }
                            } else {
                                System.out.println(queryHitObject.getSearchResults());
                            }
                        }
                        if ((this.ID_SERVER.equals("1") && Boolean.TRUE.equals(neighborsFlag)) || this.ID_SERVER.equals("4") && Boolean.TRUE.equals(neighborsFlag)) {
                            {
                                QueryHit_New queryHitObject = createQueryHitObject(queryObj);


                                if (!queryHitObject.getMessageID().getPeerID().equals(ID_SERVER)) {
                                    try {
                                        Integer backwardPath = queryHitObject.getBackwardPath(); // ------EXCEPTION HERE--------

                                        Socket socket = sockets.get(backwardPath);
                                        if (socket == null) {
                                            socket = new Socket(peerIdToIPAndPort_SERVER.get(backwardPath).split(":")[0],
                                                    Integer.parseInt(peerIdToIPAndPort_SERVER.get(backwardPath).split(":")[1]));
                                        }
                                        outputStream = new ObjectOutputStream(socket.getOutputStream());
                                        outputStream.flush();
                                        outputStream.writeObject(queryHitObject);
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        System.out.println("In Query Hit Section of Server.");
                                    }
                                } else {
                                    System.out.println(queryHitObject.getSearchResults());
                                }
                            }
                        }

                    }
                }


                QueryHit_New queryHitObj;
                if (o instanceof QueryHit_New) {
                    queryHitObj = (QueryHit_New) o;
                    //System.out.println(queryHitObj.getType());

                    //Send Backward to Requesting Peer
                    if (!queryHitObj.getMessageID().getPeerID().equals(ID_SERVER)) {
                        try {
                            Integer backwardPath = queryHitObj.getBackwardPath(); // ------EXCEPTION HERE--------

                            Socket socket = sockets.get(backwardPath);
                            outputStream = new ObjectOutputStream(socket.getOutputStream());
                            outputStream.flush();
                            outputStream.writeObject(queryHitObj);
                            outputStream.flush();
                        } catch (IOException e) {
                            System.out.println("In Query Hit Section of Server.");
                        }
                    } else {

                        //CONNECT TO CLIENT THREAD AND SEND QUERYHIT OBJECT
                        //TODO
                        ArrayList<String> searchResults = queryHitObj.getSearchResults();


                        System.out.println("--------------Distributed Search Result--------------");
                        for (int i = 0; i < searchResults.size(); i++) {
                            String option = searchResults.get(i);
                            String[] split = option.split(":");
                            String ip = split[0];
                            String port = split[1];
                            String peerID = port.substring(port.length() - 1);

                            System.out.println("Peer ID: " + peerID + " IP: " + ip + " PORT#: " + port);
                        }
                        System.out.println("Do you want to download file?");

                        if (queryHitObj.getSearchResults().size() > 0) {
                            Socket newSocket = new Socket("127.0.0.1", PORT_SELF);
                            ObjectOutputStream objOutput = new ObjectOutputStream(newSocket.getOutputStream());
                            objOutput.writeObject(queryHitObj);
                        }


                    }
                }

                Download_Request download_request;
                if (o instanceof Download_Request) {
                    download_request = (Download_Request) o;

                    BufferedOutputStream outputStream1 = new BufferedOutputStream(connection.getOutputStream());

                    obtain(outputStream1, download_request.getFullFilePath());
                }


            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception Server After Server Run()");
            } finally {

            }

        }

        /*private void nextSearchRequest() {
            Socket[] sockets;
            String fileName = null;
            BufferedReader keyboardInput;
            ObjectOutputStream outputStream = null;
            ObjectInputStream inputStream = null;

            for (int count = 1; count <= 1; count++) {

                sockets = new Socket[peerNeighbors.size()];
                keyboardInput = new BufferedReader(new InputStreamReader(System.in));

                System.out.println("In run() of client.");
                System.out.println("Enter Name of File to Search:");
                try {
                    fileName = keyboardInput.readLine();
                } catch (IOException e) {
                    System.out.println("IOException while user entering filename to search.");
                }


                Query_New query = new Query_New(new MessageID(peerID, ++sequenceNumber), TTL, fileName, Integer.parseInt(peerID));
                //showQueryData(query);

                try {
                    for (int i = 0; i < sockets.length; i++) {
                        String IPAndPort = peerIdtoIPAndPort.get(peerNeighbors.get(i));
                        String ip = IPAndPort.split(":")[0];
                        int port = Integer.parseInt(IPAndPort.split(":")[1]);
                        sockets[i] = new Socket(ip, port);
                    }


                    for (int i = 0; i < sockets.length; i++) {
                        if (sockets[i] != null) {
                            outputStream = new ObjectOutputStream(sockets[i].getOutputStream());
                            inputStream = new ObjectInputStream(sockets[i].getInputStream());

                            outputStream.flush();
                            outputStream.writeObject(query);
                            outputStream.flush();

                        }
                    }


                } catch (Exception e) {
                    System.out.println("Exception in TRY/CATCH, 355");
                } finally {

                }
            }
        }*/

        private Boolean checkIFMsgSeen(String peerID, Integer seqNum, ConcurrentHashMap<MessageID, UpstreamPeerID> seenQueries) {
            boolean flagForPeer = false;
            boolean flagForSeqNum = false;

            for (MessageID messageID : seenQueries.keySet()) {
                String peerID1 = messageID.getPeerID();
                Integer sequenceNumber = messageID.getSequenceNumber();

                if (peerID1 == peerID) {
                    flagForPeer = true;
                }
                if (sequenceNumber == seqNum) {
                    flagForSeqNum = true;
                }
            }

            if (flagForPeer && flagForSeqNum) {
                return true;
            } else {
                return false;
            }
        }

        private QueryHit_New createQueryHitObject(Query_New queryObj) {
            QueryHit_New queryHit;

            queryHit = new QueryHit_New(queryObj.getMessageID(), TTL/*queryObj.getTTL()*/, queryObj.getFileName(), IP, PORT, queryObj.getForwardPath(), queryObj.getSearchResults());
//            System.out.println("Forward Path" + queryObj.getForwardPath());
            return queryHit;
        }

        public void obtain(/*BufferedInputStream InputStream , PrintWriter writerServer,*/ BufferedOutputStream output, String fullFileAddress) {
            // boolean flag = false;

            try {

                if (output != null) {
                    //fullFileAddress = fullFileAddress.substring(9);
                    File file = new File(fullFileAddress);
//                    System.out.println("File Address: " + fullFileAddress);
                    byte[] byteArray = new byte[(int) file.length()];

                    FileInputStream fileInputStream = null;

                    try {
                        fileInputStream = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        System.out.println("File Not Found.");
                    }

                    BufferedInputStream InputStream = new BufferedInputStream(fileInputStream);

                    try {
                        InputStream.read(byteArray, 0, byteArray.length);
                        //System.out.println("Bytes Read : "+byteArray);
                        output.write(byteArray, 0, byteArray.length);
                        output.flush();
                    } catch (IOException e) {
                        System.out.println("Problem in Reading and Writing File.");
                    }


                    try {
                        output.close();
                        connection.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //writerServer.println("FILE SUCCESSFULLY SENT.");
                    //writerServer.flush();

                }

            } finally {

            }
        }

    }

    private static class Client extends Thread {
        //----------------------Basic Information for Client
        private String ID_CLIENT;
        private String IP_CLIENT;
        private Integer PORT_CLIENT;
        private Integer numberOfPeers_CLIENT;
        private ArrayList<Integer> myNeighbors_CLIENT;
        private ConcurrentHashMap<Integer, Socket> peerIdToSocket = new ConcurrentHashMap<>();
        private ConcurrentHashMap<Integer, String> peerIdToIPAndPort_CLIENT;

        //----------------------Variables to Communicate
        Socket[] sockets = null;//new Socket[myNeighbors_CLIENT.size()];
        BufferedReader keyboardInput = null;
        ObjectInputStream inputStream = null;
        ObjectOutputStream outputStream = null;
        Query_New query = null;
        QueryHit_New queryHit = null;

        //----------Variable for filename to search
        private String fileName = null;

        private BufferedReader socketInput;

        public Client(String peerID, String ip, Integer port, Integer numberOfPeers, ArrayList<Integer> peerNeighbors, ConcurrentHashMap<Integer, String> peerIdtoIPAndPort) {
            ID_CLIENT = peerID;
            IP_CLIENT = ip;
            PORT_CLIENT = port;
            numberOfPeers_CLIENT = numberOfPeers;
            myNeighbors_CLIENT = peerNeighbors;
            peerIdToIPAndPort_CLIENT = peerIdtoIPAndPort;
        }

        public void run() {

            ServerSocket clientListener = null;
            try {
                Integer PORT_CSS = Integer.parseInt("4"+PORT_CLIENT.toString().substring(1));
                clientListener = new ServerSocket(PORT_CSS);
            } catch (IOException e) {
                e.printStackTrace();
            }

            sockets = new Socket[myNeighbors_CLIENT.size()];
            keyboardInput = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
//                System.out.println("In run() of client.");
                System.out.println("Enter Name of File to Search:");
                try {
                    fileName = keyboardInput.readLine();
                } catch (IOException e) {
                    System.out.println("IOException while user entering filename to search.");
                }

                if(searchPeerFiles(fileName)){ //---------NEW
                    System.out.println("Peer Already Contains File"); continue;
                }

                Query_New query = new Query_New(new MessageID(peerID, ++sequenceNumber), TTL, fileName, Integer.parseInt(peerID));
                //showQueryData(query);

                try {
                    for (int i = 0; i < sockets.length; i++) {
                        String IPAndPort = peerIdToIPAndPort_CLIENT.get(myNeighbors_CLIENT.get(i));
                        String ip = IPAndPort.split(":")[0];
                        int port = Integer.parseInt(IPAndPort.split(":")[1]);
                        sockets[i] = new Socket(ip, port);
                    }


                    for (int i = 0; i < sockets.length; i++) {
                        if (sockets[i] != null) {
                            outputStream = new ObjectOutputStream(sockets[i].getOutputStream());
                            inputStream = new ObjectInputStream(sockets[i].getInputStream());

                            outputStream.flush();
                            outputStream.writeObject(query);
                            outputStream.flush();

                        }
                    }


                QueryHit_New queryHitObj;
                while(true){
                    Socket peerServer = clientListener.accept();
                    ObjectInputStream objInput = new ObjectInputStream(peerServer.getInputStream()); //TODO
                    queryHitObj = (QueryHit_New) objInput.readObject();
                    System.out.println(queryHitObj.getSearchResults());

                    System.out.println("Peer Object Received.");

                    //-------------------Search Result ---------------------------

                    ArrayList<String> searchResults = queryHitObj.getSearchResults();


                   /* System.out.println("--------------Distributed Search Result--------------");
                    for (int i = 0; i < searchResults.size(); i++) {
                        String option = searchResults.get(i);
                        String[] split = option.split(":");
                        String ip = split[0];
                        String port = split[1];
                        String peerID = port.substring(port.length() - 1);

                        System.out.println("Peer ID: " + peerID + " IP: " + ip + " PORT#: " + port);
                    }*/


                    BufferedReader userInput2;
                    userInput2 = new BufferedReader(new InputStreamReader(System.in));

                    if (searchResults.size() > 0) {

                        System.out.println("Do you want to download file?");
                        String userChoice = userInput2.readLine();

                        if (userChoice.equals("y")) {
                            System.out.println("Please enter PORT NUMBER from above list:");
                            String userSelection = userInput2.readLine();
                            String ip = "127.0.0.1";
                            String port = userSelection;
                            String fullFilePath = "Node" + port.substring(port.length() - 1) + "/Myfiles/" + queryHitObj.getFileName();
                            System.out.println(ip + " " + port + " " + fullFilePath);

                            Download_Request downloadRequest;
                            downloadRequest = new Download_Request(fullFilePath, queryHitObj.getFileName());


                            System.out.println("Contacting Peer To Download File...");
                            Socket socketForPeer = new Socket(ip, Integer.parseInt(port));
                            outputStream = new ObjectOutputStream(socketForPeer.getOutputStream());
                            outputStream.writeObject(downloadRequest);
                            outputStream.flush();

                            BufferedReader socketPeerInput = new BufferedReader(new InputStreamReader(socketForPeer.getInputStream()));

                            byte[] byteArray = new byte[1];
                            int bytesRead;
                            InputStream input;
                            input = socketForPeer.getInputStream();
                            ByteArrayOutputStream byteOutputStream;
                            byteOutputStream = new ByteArrayOutputStream();

                            if (input != null) {

                                BufferedOutputStream bufferedOStream = null;
                                try {

                                    bufferedOStream = new BufferedOutputStream(new FileOutputStream("Node" + peerID + "/Downloads/" + queryHitObj.getFileName()));
                                    bytesRead = input.read(byteArray, 0, byteArray.length);

                                    do {
                                        byteOutputStream.write(byteArray, 0, byteArray.length);
                                        bytesRead = input.read(byteArray);
                                    } while (bytesRead != -1);

                                    bufferedOStream.write(byteOutputStream.toByteArray());
                                    bufferedOStream.flush();

                                    System.out.println("File Successfully Downloaded.");

                                    //nextSearchRequest();

                                } catch (IOException e) {
                                }
                            }
                        }else{
                            System.out.println("This search & download request is complete.");
                            //nextSearchRequest();
                        }
                    } else{
                        System.out.println("File Not Found.");
                        //nextSearchRequest();
                    }

                    break;
                }


                } catch (Exception e) {
                    System.out.println("Exception in TRY/CATCH, 355");
                } finally {

                }
            }


        }


    }


}
