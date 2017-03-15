import java.io.File;
import java.util.Date;

/**
 * Created by rittick on 3/7/17.
 */
public class FileInfo {
    private String fileName;
    private Integer versionNumber;
    private Integer originServerID;
    private File fileObject;
    private Date TTR;
    private String ip;
    private Integer port;

    //For Downloaded Files ONLY
    private String consistencyState; // Valid, Invalid or TTR Expired

    public FileInfo(String fileName, Integer versionNumber, Integer originServerID, File fileObject) {
        this.fileName = fileName;
        this.versionNumber = versionNumber;
        this.originServerID = originServerID;
        this.fileObject = fileObject;
    }

    //------Getters-------

    public String getFileName() {
        return fileName;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public Integer getOriginServerID() {
        return originServerID;
    }

    public File getFileObject() {
        return fileObject;
    }

    public String getConsistencyState() {
        return consistencyState;
    }

    public Date getTTR() {
        return TTR;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    //------Setters---


    public void setVersionNumber() {
        this.versionNumber += 1;
    }

    public void setVersionNumber_2(Integer versionNumber){
        this.versionNumber = versionNumber;
    }

    public void setConsistencyState(String consistencyState) {
        this.consistencyState = consistencyState;
    }

    public void setOriginServerID(Integer originServerID) {
        this.originServerID = originServerID;
    }

    public void setTTR(Date TTR) {
        this.TTR = TTR;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
