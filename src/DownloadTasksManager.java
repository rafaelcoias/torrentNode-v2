import java.util.*;
import java.util.concurrent.*;

public class DownloadTasksManager {
    private final Map<String, byte[]> downloadedBlocks = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor;

    public DownloadTasksManager(int maxThreads) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThreads);
    }

    public synchronized void addTask(FileBlockRequestMessage task, PeerConnection peer) {
        executor.execute(() -> processTask(task, peer));
    }

    private void processTask(FileBlockRequestMessage task, PeerConnection peer) {
        try {
            // Enviar a solicitação para o nó remoto
            FileBlockAnswerMessage response = requestBlockFromNode(task, peer);

            // Salvar o bloco recebido
            downloadedBlocks.put(task.getFileHash() + "-" + task.getOffset(), response.getBlockData());
        } catch (Exception e) {
            System.err.println("Erro ao processar bloco: " + task.getFileHash() + " Offset: " + task.getOffset());
            e.printStackTrace();
        }
    }

    private FileBlockAnswerMessage requestBlockFromNode(FileBlockRequestMessage task, PeerConnection peer) {
        // Simular ou implementar a comunicação com o nó remoto
        NetworkUtils.sendMessage(peer.getIpAddress(), peer.getPort(), task);
        return new FileBlockAnswerMessage(new byte[task.getLength()]); // Substituir com resposta real
    }

    public void shutdown() {
        executor.shutdown();
    }

    public Map<String, byte[]> getDownloadedBlocks() {
        return downloadedBlocks;
    }
}
