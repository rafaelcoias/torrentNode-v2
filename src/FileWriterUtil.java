import java.io.*;
import java.util.Map;

public class FileWriterUtil {

    public static void writeFileToDisk(String fileHash, long fileSize, String filePath, Map<String, byte[]> downloadedBlocks) {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            for (long offset = 0; offset < fileSize; offset += 10240) {
                byte[] block = downloadedBlocks.get(fileHash + "-" + offset);
                if (block != null) {
                    fos.write(block);
                } else {
                    throw new IOException("Bloco ausente no offset: " + offset);
                }
            }
            System.out.println("Arquivo " + filePath + " foi escrito com sucesso!");
        } catch (IOException e) {
            System.err.println("Erro ao escrever o arquivo: " + filePath);
            e.printStackTrace();
        }
    }
}
