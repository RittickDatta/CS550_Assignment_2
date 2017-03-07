import java.io.File;

/**
 * Created by rittick on 3/7/17.
 */
public class FileInfo {
    private String fileName;
    private Integer versionNumber;
    private Integer originServerID;
    private File fileObject;

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

    //------Setters---


    public void setVersionNumber() {
        this.versionNumber += 1;
    }
}
