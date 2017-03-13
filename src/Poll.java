import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by rittick on 3/9/17.
 */
public class Poll implements Serializable{
    private String fileName;
    private String consistencyState;
    private Integer versionNumber;
    private String type;

    public Poll(String fileName, String consistencyState, Integer versionNumber, String type) {
        this.fileName = fileName;
        this.consistencyState = consistencyState;
        this.versionNumber = versionNumber;
        this.type = type;
    }

    public Poll(String fileName, String type) {
        this.fileName = fileName;
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public String getConsistencyState() {
        return consistencyState;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setConsistencyState(String consistencyState) {
        this.consistencyState = consistencyState;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getType() {
        return type;
    }
}
