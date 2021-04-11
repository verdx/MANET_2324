package d2d.testing.gui.main;

public class StreamDetail {
    private String ip;
    private String uuid;
    private String name;
    private int port;
    private boolean download;

    public StreamDetail(String uuid, String name, String ip, int port){
        this.uuid = uuid;
        this.ip = ip;
        this.download = false;
        this.port = port;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public boolean equals(Object o) {
        if(o instanceof StreamDetail) {
            StreamDetail streamDetail = (StreamDetail) o;
            return streamDetail.uuid.equals(this.uuid) && streamDetail.ip.equals(this.ip);
        }
        return false;
    }
}
