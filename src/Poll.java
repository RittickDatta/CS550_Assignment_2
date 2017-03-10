import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by rittick on 3/9/17.
 */
public class Poll implements Serializable{
    private String fileName;
    private String consistencyState;
    private Integer versionNumber;

    public Poll(String fileName, String consistencyState, Integer versionNumber) {
        this.fileName = fileName;
        this.consistencyState = consistencyState;
        this.versionNumber = versionNumber;
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
}
