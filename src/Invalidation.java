import java.io.Serializable;

/**
 * Created by rittick on 3/7/17.
 */
public class Invalidation implements Serializable{
    private MessageID messageID;
    private String originServerID;
    private String fileName;
    private Integer versionNumber;

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
}
