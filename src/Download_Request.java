import java.io.BufferedOutputStream;
import java.io.Serializable;

/**
 * Created by rittick on 2/21/17.
 */
public class Download_Request implements Serializable{
    String ip;
    String port;
    String fullFilePath;
    String fileName;

    public Download_Request(String fullFilePath, String fileName) {
        this.fullFilePath = fullFilePath;
        this.fileName = fileName;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getFullFilePath() {
        return fullFilePath;
    }

    public String getFileName() {
        return fileName;
    }
}
