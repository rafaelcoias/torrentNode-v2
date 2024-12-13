import java.io.*;
import java.net.*;
import java.util.*;

public class NodeServer {
    private final int port;
    private final String fileDirectory;

    public NodeServer(int port, String fileDirectory) {
        this.port = port;
        this.fileDirectory = fileDirectory;
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor ativo na porta " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            Object message = in.readObject();
            processMessage(message, clientSocket, out);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void processMessage(Object message, Socket clientSocket, ObjectOutputStream out) {
        if (message instanceof WordSearchMessage) {
            handleSearch((WordSearchMessage) message, out);
        } else if (message instanceof FileBlockRequestMessage) {
            handleBlockRequest((FileBlockRequestMessage) message, out);
        } else if (message instanceof FileBlockAnswerMessage) {
            handleBlockResponse((FileBlockAnswerMessage) message);
        }
    }

    private void handleSearch(WordSearchMessage searchMessage, ObjectOutputStream out) {
        List<FileSearchResult> results = new ArrayList<>();
        File folder = new File(fileDirectory);
    
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.getName().toLowerCase().contains(searchMessage.getKeyword().toLowerCase())) {
                    String hash = HashUtils.calculateHash(file.getAbsolutePath());
                    results.add(new FileSearchResult(file.getName(), hash, file.length(), "127.0.0.1", port));
                }
            }
        }
    
        try {
            out.writeObject(results); // Envia os resultados ao cliente
            out.flush();              // Garante que os dados são enviados
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void handleBlockRequest(FileBlockRequestMessage blockRequest, ObjectOutputStream out) {
        try {
            File file = new File(fileDirectory + "/" + blockRequest.getFileHash());
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(blockRequest.getOffset());
                byte[] buffer = new byte[blockRequest.getLength()];
                raf.read(buffer);
                out.writeObject(new FileBlockAnswerMessage(buffer));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleBlockResponse(FileBlockAnswerMessage blockAnswer) {
        System.out.println("Bloco recebido: " + blockAnswer.getBlockData().length + " bytes");
    }
}