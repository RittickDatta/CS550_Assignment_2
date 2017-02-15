/**
 * Created by rittick on 2/15/17.
 */
public class MessageID {
    private String peerID;
    private Integer sequenceNumber;

    public MessageID(String peerID, Integer sequenceNumber) {
        this.peerID = peerID;
        this.sequenceNumber = sequenceNumber;
    }

    public String getPeerID() {
        return peerID;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }
}
