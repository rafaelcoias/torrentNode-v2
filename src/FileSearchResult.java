import java.io.Serializable;

public class FileSearchResult implements Serializable {
    private String fileName;
    private String fileHash;
    private long fileSize;
    private String nodeAddress;
    private int nodePort;

    public FileSearchResult(String fileName, String fileHash, long fileSize, String nodeAddress, int nodePort) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.nodeAddress = nodeAddress;
        this.nodePort = nodePort;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public int getNodePort() {
        return nodePort;
    }
}
