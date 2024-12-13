import java.io.Serializable;

public class FileBlockAnswerMessage implements Serializable {
    private byte[] blockData;

    public FileBlockAnswerMessage(byte[] blockData) {
        this.blockData = blockData;
    }

    public byte[] getBlockData() {
        return blockData;
    }
}
