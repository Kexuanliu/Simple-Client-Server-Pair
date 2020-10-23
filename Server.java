import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {
    public static void main(String[] args) {
        Server s = new Server();

        try {
            DatagramSocket socket = new DatagramSocket(12235);

            while (true) {
                byte[] buf = new byte[120];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);
                Thread t = new ServerRunnable(socket, dp);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}