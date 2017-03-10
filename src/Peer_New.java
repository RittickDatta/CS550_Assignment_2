

import java.io.*;
import java.lang.reflect.Array;
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
    private static final String configFileName = "network_linear.config";

    //----------Start Change--------------------
    private static ConcurrentHashMap<Integer, Socket> socketsConsistency = new ConcurrentHashMap<>();
    private static OriginFileInventory originFileInventory;
    private static DownloadsFileInventory downloadsFileInventory;
    private static ArrayList<FileInfo> fileInfosOrigin;
    private static ArrayList<FileInfo> fileInfosDownloads;
    //----------End Change----------------------

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
                if (checkFileValidity(myFiles.get(i))) {                //-------------Start Change------------------
                    flag = true;
                    return flag;
                }
            }
        }

        return flag;
    }

    private static boolean checkFileValidity(String filename) {
        boolean flag = false;

        for(int i=0; i<fileInfosOrigin.size(); i++){
            FileInfo fileInfo = fileInfosOrigin.get(i);
            if(fileInfo.getFileName().equals(filename)){
                if (fileInfo.getConsistencyState().equals("VALID")){
                    flag = true;
                    return flag;
                }
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
            Integer PORT_SELF = Integer.parseInt("4" + PORT_SERVER.toString().substring(1));

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

                        ArrayList<String> searchResults = queryHitObj.getSearchResults();


                        if (searchResults.size()>0) {
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
                        }

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

                Invalidation invalidation;
                if(o instanceof Invalidation) {
                    Invalidation invalidation_Obj = (Invalidation) o;
                    System.out.println("Invalidation Object Received.");

                    invalidation_Obj.setForwardPath(Integer.parseInt(peerID));
                    updateOriginFileInventory(invalidation_Obj);

                    while (neighborsIterator.hasNext()) {
                        Integer nextNeighbor = neighborsIterator.next();
                        String ip = peerIdtoIPAndPort.get(nextNeighbor).split(":")[0];
                        int port = Integer.parseInt(peerIdtoIPAndPort.get(nextNeighbor).split(":")[1]);

                        socketsConsistency.put(nextNeighbor, new Socket(ip, port));


                            if (!invalidation_Obj.getForwardPath().contains(nextNeighbor) ) {

                                outputStream = new ObjectOutputStream(socketsConsistency.get(nextNeighbor).getOutputStream()); // Exception, CHECK
                                outputStream.flush();
                                outputStream.writeObject(invalidation_Obj);
                                outputStream.flush();

                            }

                    }
                }

                Poll poll;
                if(o instanceof Poll){
                    Poll poll_Object = (Poll) o;
                    for(int j=0; j<fileInfosOrigin.size(); j++){
                        FileInfo fileInfo = fileInfosOrigin.get(j);
                        if (fileInfo.getFileName().equals(poll_Object.getFileName())){
                            if(fileInfo.getVersionNumber() > poll_Object.getVersionNumber()){
                                poll_Object.setConsistencyState("INVALID");//TODO
                                outputStream.flush();
                                outputStream.writeObject(poll_Object);
                                outputStream.flush();
                                break;
                            }
                        }
                    }
                }



            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception Server After Server Run()");
            } finally {

            }

        }

        private void updateOriginFileInventory(Invalidation invalidation_obj) {
            String fileName = invalidation_obj.getFileName();
            for(int i=0; i<fileInfosOrigin.size(); i++){
                FileInfo fileInfo = fileInfosOrigin.get(i);
                if(fileInfo.getFileName().equals(fileName)){
                    fileInfo.setConsistencyState("INVALID");

                    //Give User an Option to Update File
                    /*try {
                        BufferedReader keyBoardInput = new BufferedReader(new InputStreamReader(System.in));
                        System.out.println("Do you want to update "+fileName+" to lastest version?");
                        String updateInvalidFile = keyBoardInput.readLine();
                        if(updateInvalidFile.equals("y")){
                            String fullFilePath = "Node" + invalidation_obj.getOriginServerID() + "/Myfiles/" + invalidation_obj.getFileName();
                            Download_Request downloadRequest;
                            downloadRequest = new Download_Request(fullFilePath, invalidation_obj.getFileName());

                            System.out.println("Contacting Peer To Download File...");
                            Socket socketForPeer = new Socket(invalidation_obj.getIP(), invalidation_obj.getPORT());

                            ObjectOutputStream outputStream;
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

                                    bufferedOStream = new BufferedOutputStream(new FileOutputStream("Node" + invalidation_obj.getOriginServerID() + "/Myfiles/" + invalidation_obj.getFileName()));
                                    bytesRead = input.read(byteArray, 0, byteArray.length);

                                    do {
                                        byteOutputStream.write(byteArray, 0, byteArray.length);
                                        bytesRead = input.read(byteArray);
                                    } while (bytesRead != -1);

                                    bufferedOStream.write(byteOutputStream.toByteArray());
                                    bufferedOStream.flush();

                                    System.out.println("File Successfully Updated.");

                                    //nextSearchRequest();

                                } catch (IOException e) {
                                }
                            }

                        }else {
                            System.out.println("File "+invalidation_obj.getFileName()+" is in Invalid State.");
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/


                }
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

        //--------Start Change------------------

        Socket[] socketsNeighbors = null;

        //---------End Change---------------------

        public Client(String peerID, String ip, Integer port, Integer numberOfPeers, ArrayList<Integer> peerNeighbors, ConcurrentHashMap<Integer, String> peerIdtoIPAndPort) {
            ID_CLIENT = peerID;
            IP_CLIENT = ip;
            PORT_CLIENT = port;
            numberOfPeers_CLIENT = numberOfPeers;
            myNeighbors_CLIENT = peerNeighbors;
            peerIdToIPAndPort_CLIENT = peerIdtoIPAndPort;
        }

        public void run() {

            //--------Start Change--------------------------------


            originFileInventory = new OriginFileInventory("Node" + peerID + "/Myfiles/", Integer.parseInt(peerID));
            fileInfosOrigin = originFileInventory.prepareOriginFileInventory();


            downloadsFileInventory = new DownloadsFileInventory("Node" + peerID + "/Downloads/", Integer.parseInt(peerID));
            fileInfosDownloads = downloadsFileInventory.prepareDownloadsFileInventory();

            //************** PUSH *************************
            if (false) {
                if (peerID.equals("1")) {
                    String fileToModify = "file1.txt";
                    ModifyFile modifyFileObject = new ModifyFile(fileToModify);
                    boolean isFileModified = modifyFileObject.modifyFile();
                    if (isFileModified) {

                        Invalidation invalidation = new Invalidation(
                                new MessageID(peerID, invalidationSequenceNumber),
                                peerID,
                                fileToModify,
                                getVersionNumber(fileToModify),
                                IP_CLIENT,
                                PORT_CLIENT
                        );

                        invalidation.setForwardPath(Integer.parseInt(peerID));
                        inspectInvalidationObject(invalidation);

                        //File is Modified, Now Broadcast Invalidation Object to Other Nodes.
                        socketsNeighbors = new Socket[myNeighbors_CLIENT.size()];
                        try {


                            for (int i = 0; i < socketsNeighbors.length; i++) {
                                String IPAndPort = peerIdToIPAndPort_CLIENT.get(myNeighbors_CLIENT.get(i));
                                String ip = IPAndPort.split(":")[0];
                                int port = Integer.parseInt(IPAndPort.split(":")[1]);
                                socketsNeighbors[i] = new Socket(ip, port);
                            }


                            for (int i = 0; i < socketsNeighbors.length; i++) {
                                if (socketsNeighbors[i] != null) {
                                    outputStream = new ObjectOutputStream(socketsNeighbors[i].getOutputStream());
                                    inputStream = new ObjectInputStream(socketsNeighbors[i].getInputStream());

                                    outputStream.flush();
                                    outputStream.writeObject(invalidation);
                                    outputStream.flush();

                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }

            //****************** PULL **********************************
            if(true){

                if (peerID.equals("1")) {
                    String fileToModify = "file1.txt";
                    ModifyFile modifyFileObject = new ModifyFile(fileToModify);
                    boolean isFileModified = modifyFileObject.modifyFile();

                    if (isFileModified) {

                        for(int i=0; i<fileInfosOrigin.size(); i++){
                            FileInfo fileInfo = fileInfosOrigin.get(i);
                            if(fileInfo.getFileName().equals(fileToModify)){
                                fileInfosOrigin.get(i).setVersionNumber();
                            }
                        }

//                        inspectFileVersionNumbers();

                    }
                }

                //----------All Other Nodes will Poll Server------------
                else{
                    ArrayList<Integer> filesPEER1 = new ArrayList<>();
                    ArrayList<Integer> filesPEER2 = new ArrayList<>();
                    ArrayList<Integer> filesPEER3 = new ArrayList<>();
                    ArrayList<Integer> filesPEER4 = new ArrayList<>();

                    for(int i=1; i<=10; i++){ filesPEER1.add(i);}
                    for(int i=11; i<=20; i++){ filesPEER2.add(i);}
                    for(int i=21; i<=30; i++){ filesPEER3.add(i);}
                    for(int i=31; i<=40; i++){ filesPEER4.add(i);}

                    //Update Origin Server ID in FileInfosOrigin
                    for(int j=0; j<myFiles.size(); j++){
                        String fileName = myFiles.get(j);
                        String[] split1 = fileName.split(".txt");
                        String[] split2 = split1[0].split("file");
                        Integer fileNumber = Integer.parseInt(split2[1]);

                        if(filesPEER1.contains(fileNumber)){
                            updateFileInfosOrigin(fileName, 1);
                        }else if (filesPEER2.contains(fileNumber)){
                            updateFileInfosOrigin(fileName, 2);
                        }else if (filesPEER3.contains(fileNumber)){
                            updateFileInfosOrigin(fileName, 3);
                        }else if (filesPEER4.contains(fileNumber)){
                            updateFileInfosOrigin(fileName, 4);
                        }
                    }

                    System.out.println(fileInfosOrigin.size());

                    //Poll Other Servers with Files not Belonging to This Peer
                    for(int i=0; i<fileInfosOrigin.size(); i++){
                        FileInfo fileInfo = fileInfosOrigin.get(i);
                        if (fileInfo.getOriginServerID() != Integer.parseInt(ID_CLIENT)){
                            Poll poll = new Poll(fileInfo.getFileName(), fileInfo.getConsistencyState(), fileInfo.getVersionNumber());
                            String IPAndPort = peerIdToIPAndPort_CLIENT.get(fileInfo.getOriginServerID());
                            String ip = IPAndPort.split(":")[0];
                            int port = Integer.parseInt(IPAndPort.split(":")[1]);


                            try {
                                Socket socket = new Socket(ip, port);
                                ObjectOutputStream ObjectOutputPoll;
                                ObjectInputStream ObjectInputPoll;
                                ObjectOutputPoll = new ObjectOutputStream(socket.getOutputStream());
                                ObjectInputPoll = new ObjectInputStream(socket.getInputStream());

                                ObjectOutputPoll.flush();
                                ObjectOutputPoll.writeObject(poll);  //TODO
                                ObjectOutputPoll.flush();


                                try {
                                    Poll updatedPoll = (Poll) ObjectInputPoll.readObject();
                                    System.out.println(updatedPoll.getFileName());
                                    System.out.println(updatedPoll.getConsistencyState());

                                    for(int z=0; z<fileInfosOrigin.size(); z++){
                                        FileInfo fileInfo1 = fileInfosOrigin.get(z);
                                        if(fileInfo1.getFileName().equals(updatedPoll.getFileName())){
                                            fileInfosOrigin.get(z).setConsistencyState(updatedPoll.getConsistencyState());
                                        }

                                    }

                                    System.out.println(fileInfosOrigin.size());

                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }



                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                }

            }


            //--------End Change-----------------------------------

            ServerSocket clientListener = null;
            try {
                Integer PORT_CSS = Integer.parseInt("4" + PORT_CLIENT.toString().substring(1));
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

               /* if (searchPeerFiles(fileName)) { //---------NEW
                    System.out.println("Peer Already Contains File");
                    continue;
                }*/

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
                    while (true) {
                        Socket peerServer = clientListener.accept();
                        ObjectInputStream objInput = new ObjectInputStream(peerServer.getInputStream());
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
                            } else {
                                System.out.println("This search & download request is complete.");
                                //nextSearchRequest();
                            }
                        } else {
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

        private void updateFileInfosOrigin(String fileName, Integer originServerID) {
            for(int i=0; i<fileInfosOrigin.size(); i++){
                FileInfo fileInfo = fileInfosOrigin.get(i);
                if(fileInfo.getFileName().equals(fileName)){
                    fileInfosOrigin.get(i).setOriginServerID(originServerID);
                }
            }
        }

        private static void inspectFileVersionNumbers() {
            for(int i=0; i<fileInfosOrigin.size(); i++){
                FileInfo fileInfo = fileInfosOrigin.get(i);
                System.out.println(fileInfo.getFileName());
                System.out.println(fileInfo.getVersionNumber());
                System.out.println();
            }
        }

        private static void inspectInvalidationObject(Invalidation invalidation) {
            System.out.println("Peer ID: "+invalidation.getMessageID().getPeerID());
            System.out.println("Sequence Number: "+invalidation.getMessageID().getSequenceNumber());
            System.out.println("File Name: "+invalidation.getFileName());
            System.out.println("Origin Server ID: "+invalidation.getOriginServerID());
            System.out.println("Version Number: "+invalidation.getVersionNumber());
            System.out.println("Forward Path: "+invalidation.getForwardPath());
        }

        private Integer getVersionNumber(String fileToModify) {
            Integer versionNumber = -1;

            for (int i = 0; i < fileInfosOrigin.size(); i++) {

                FileInfo fileInfo = fileInfosOrigin.get(i);
                if(fileInfo.getFileName().equals(fileToModify)){
                    versionNumber = fileInfo.getVersionNumber();
                    return versionNumber;
                }
            }
            return versionNumber;
        }


    }


}
