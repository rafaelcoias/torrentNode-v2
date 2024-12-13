import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class IscTorrentNode {
    private final String ipAddress;
    private final int port;
    private final String fileDirectory;
    private final List<PeerConnection> connectedPeers = new CopyOnWriteArrayList<>();
    private JTextArea logArea;
    private DefaultListModel<String> searchResults;
    private JTextField searchField;

    public IscTorrentNode(String ipAddress, int port, String fileDirectory) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.fileDirectory = fileDirectory;
        start();
    }

    public void start() {
        new Thread(this::startServer).start();
        initializeInterface();
    }

    private void startServer() {
        NodeServer server = new NodeServer(port, fileDirectory);
        server.startServer();
    }

    private void initializeInterface() {
        JFrame frame = new JFrame("Sistema de Partilha de Ficheiros");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Informações do nó
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("IP: " + ipAddress + " | Porta: " + port + " | Pasta: " + fileDirectory);
        infoPanel.add(infoLabel);

        frame.add(infoPanel, BorderLayout.NORTH);

        // Painel central: pesquisa e resultados
        JPanel centerPanel = new JPanel(new BorderLayout());

        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton("Procurar");
        DefaultListModel<String> searchResults = new DefaultListModel<>();
        JList<String> searchResultsList = new JList<>(searchResults);
        JScrollPane scrollPane = new JScrollPane(searchResultsList);

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Texto a procurar:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        centerPanel.add(searchPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Log de progresso
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);

        // Divisão entre a pesquisa e os logs usando JSplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, logScrollPane);
        splitPane.setDividerLocation(500); // Define a largura inicial da divisão
        splitPane.setResizeWeight(0.7); // Prioriza o lado esquerdo ao redimensionar
        frame.add(splitPane, BorderLayout.CENTER);

        // Painel de ações
        JPanel actionPanel = new JPanel(new FlowLayout());
        JButton connectButton = new JButton("Ligar a Nó");
        JButton downloadButton = new JButton("Descarregar");
        actionPanel.add(downloadButton);
        actionPanel.add(connectButton);
        frame.add(actionPanel, BorderLayout.SOUTH);

        // Ações dos botões
        searchButton.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (searchTerm.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Por favor, insira um termo de pesquisa válido.");
                return;
            }
            searchFiles(searchTerm, searchResults);
        });

        connectButton.addActionListener(e -> connectNode());

        downloadButton.addActionListener(e -> {
            String selectedFile = searchResultsList.getSelectedValue();
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(frame, "Por favor, selecione um ficheiro para descarregar.");
                return;
            }
            downloadFile(selectedFile);
        });

        frame.setVisible(true);
    }

    private void connectNode() {
        String input = JOptionPane.showInputDialog("Digite o IP e a porta do nó (formato: IP:PORTA):");
        if (input == null || !input.contains(":")) {
            JOptionPane.showMessageDialog(null, "Formato inválido. Use IP:PORTA.");
            return;
        }

        String[] parts = input.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);

        PeerConnection peer = new PeerConnection(ip, port);
        connectedPeers.add(peer);
        logArea.append("Conectado ao nó: " + ip + ":" + port + "\n");
    }

    private void searchFiles(String searchTerm, DefaultListModel<String> results) {
        logArea.append("Procurando por: " + searchTerm + "\n");
    
        WordSearchMessage message = new WordSearchMessage(searchTerm);
        results.clear();
    
        for (PeerConnection peer : connectedPeers) {
            try (Socket socket = new Socket(peer.getIpAddress(), peer.getPort());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
    
                out.writeObject(message); 
                out.flush();           
    
                // Recebe os resultados
                Object response = in.readObject();
                if (response instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<FileSearchResult> searchResults = (List<FileSearchResult>) response;
                    for (FileSearchResult result : searchResults) {
                        results.addElement(result.getFileName() + " (" + result.getNodeAddress() + ":" + result.getNodePort() + ")");
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                logArea.append("Erro ao conectar ao nó: " + peer.getIpAddress() + ":" + peer.getPort() + "\n");
                e.printStackTrace();
            }
        }
    }
    

    private void downloadFile(String selectedFile) {
        logArea.append("Iniciando o download do arquivo: " + selectedFile + "\n");
        String fileHash = extractFileHash(selectedFile);
        long fileSize = getFileSize(fileHash);

        DownloadTasksManager manager = new DownloadTasksManager(5);
        int blockSize = 10240;

        for (long offset = 0; offset < fileSize; offset += blockSize) {
            int length = (int) Math.min(blockSize, fileSize - offset);
            FileBlockRequestMessage task = new FileBlockRequestMessage(fileHash, offset, length);

            // Enviar o pedido ao primeiro nó conectado (ajustar se necessário)
            if (!connectedPeers.isEmpty()) {
                manager.addTask(task, connectedPeers.get(0));
            } else {
                logArea.append("Nenhum nó conectado.\n");
                return;
            }
        }

        // Após o download, escrever o arquivo no disco
        new Thread(() -> {
            manager.shutdown();
            FileWriterUtil.writeFileToDisk(fileHash, fileSize, fileDirectory + "/" + fileHash,
                    manager.getDownloadedBlocks());
            logArea.append("Download concluído para o arquivo: " + fileHash + "\n");
        }).start();
    }

    private String extractFileHash(String selectedFile) {
        int spaceIndex = selectedFile.indexOf(' ');
        if (spaceIndex != -1) {
            return selectedFile.substring(0, spaceIndex);
        }
        return selectedFile;
    }

    private long getFileSize(String fileHash) {
        File file = new File(fileDirectory + "/" + fileHash);
        return file.exists() ? file.length() : 0;
    }

}
