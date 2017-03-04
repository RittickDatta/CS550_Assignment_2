import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by rittick on 2/15/17.
 */
public class QueryHit_New implements Serializable {
    private String type = "queryHit";
    private MessageID messageID;
    private Integer TTL;
    private String fileName;
    private String peerIP;
    private Integer port;
    private ArrayList<Integer> backwardPath = new ArrayList<>();
    private ArrayList<String> searchResults = new ArrayList<>();
    private Socket homeSocket;

    public QueryHit_New(MessageID messageID, Integer TTL, String fileName, String peerIP, Integer port, ArrayList<Integer> backwardPath, ArrayList<String> search_Results) {
        this.messageID = messageID;
        this.TTL = TTL;
        this.fileName = fileName;
        this.peerIP = peerIP;
        this.port = port;
        this.backwardPath = backwardPath;
        this.searchResults = search_Results;
    }

    public String getType() { return type; }

    public MessageID getMessageID() {
        return messageID;
    }

    public Integer getTTL() {
        return TTL;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPeerIP() {
        return peerIP;
    }

    public Integer getPort() {
        return port;
    }

    public void setTTL() {
        this.TTL = this.TTL - 1; //CHECK
    }

    public void setPeerIP(String peerIP) {
        this.peerIP = peerIP;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getBackwardPath() {

        /*Integer max = Collections.max(this.backwardPath);
        int indexOfMax = this.backwardPath.indexOf(max);
        if(indexOfMax != this.backwardPath.size()-1){
            //Collections.reverse(this.backwardPath);
            this.backwardPath.remove(0);
            this.backwardPath.remove(0);
            return this.backwardPath.get(0);
        }*/

        //this.backwardPath.remove(this.backwardPath.size() - 1);
        this.backwardPath.remove(this.backwardPath.size()-1); // CHECK
        return this.backwardPath.get(this.backwardPath.size()-1);

    }

    public ArrayList<String> getSearchResults() {
        return searchResults;
    }
}
