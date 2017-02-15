/**
 * Created by rittick on 2/15/17.
 */
public class QueryHit {
    private MessageID messageID;
    private Integer TTL;
    private String fileName;
    private String peerIP;
    private Integer port;

    public QueryHit(MessageID messageID, Integer TTL, String fileName, String peerIP, Integer port) {
        this.messageID = messageID;
        this.TTL = TTL;
        this.fileName = fileName;
        this.peerIP = peerIP;
        this.port = port;
    }

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

    public void setTTL(Integer TTL) {
        this.TTL = TTL - 1; //CHECK
    }

    public void setPeerIP(String peerIP) {
        this.peerIP = peerIP;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
