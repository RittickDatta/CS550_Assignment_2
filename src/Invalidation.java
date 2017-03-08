import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by rittick on 3/7/17.
 */
public class Invalidation implements Serializable{
    private MessageID messageID;
    private String originServerID;
    private String fileName;
    private Integer versionNumber;
    private ArrayList<Integer> forwardPath = new ArrayList<>();

    public Invalidation(MessageID messageID, String originServerID, String fileName, Integer versionNumber) {
        this.messageID = messageID;
        this.originServerID = originServerID;
        this.fileName = fileName;
        this.versionNumber = versionNumber;
    }

    public MessageID getMessageID() {
        return messageID;
    }

    public String getOriginServerID() {
        return originServerID;
    }

    public String getFileName() {
        return fileName;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setForwardPath(Integer peerID) {
        this.forwardPath.add(peerID);
    }

    public ArrayList<Integer> getForwardPath() {
        return forwardPath;
    }
}
