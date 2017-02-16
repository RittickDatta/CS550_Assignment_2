import java.io.Serializable;

/**
 * Created by rittick on 2/15/17.
 */
public class Query implements Serializable {
    private String type = "query";
    private MessageID messageID;
    private Integer TTL;
    private String fileName;

    public Query(MessageID messageID, Integer TTL, String fileName) {
        this.messageID = messageID;
        this.TTL = TTL;
        this.fileName = fileName;
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
}
