import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by rittick on 2/15/17.
 */
public class Query implements Serializable {
    private String type = "query";
    private MessageID messageID;
    private Integer TTL;
    private String fileName;
    private ArrayList<Integer> forwardPath = new ArrayList<>();

    public Query(MessageID messageID, Integer TTL, String fileName, Integer peerId) {
        this.messageID = messageID;
        this.TTL = TTL;
        this.fileName = fileName;
        this.forwardPath.add(peerId);
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

    public void setTTL() {
        this.TTL = this.TTL - 1; //CHECK
    }

    public void setForwardPath(Integer peerID) {
        this.forwardPath.add(peerID);
    }

    public ArrayList<Integer> getForwardPath() {
        return forwardPath;
    }
}
