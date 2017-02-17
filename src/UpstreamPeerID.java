import java.io.Serializable;

/**
 * Created by rittick on 2/16/17.
 */
public class UpstreamPeerID implements Serializable{
    private String IP;
    private Integer port;

    public UpstreamPeerID(String IP, Integer port) {
        this.IP = IP;
        this.port = port;
    }

    public String getIP() {
        return IP;
    }

    public Integer getPort() {
        return port;
    }
}
