import java.io.Serializable;

public class WordSearchMessage implements Serializable {
    private String keyword;

    public WordSearchMessage(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
