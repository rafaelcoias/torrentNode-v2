import java.io.*;
import java.net.*;

public class NetworkUtils {
    public static void sendMessage(String address, int port, Serializable message) {
        try (Socket socket = new Socket(address, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
