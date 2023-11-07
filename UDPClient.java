import java.net.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class UDPClient {
    public static boolean connected = true;

    public static void main(String[] args) {

        try {
            // Create a datagram socket, look for the first available port
            DatagramSocket socket = new DatagramSocket(54321);
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 12345;
            // Start a thread to receive messages
            Receiver receiver = new Receiver(socket);
            receiver.start();

            while (connected) {
                /* SEND DATA */
                System.out.println("ENTER MESSAGE FOR SERVER with max 32 characters");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String msg = reader.readLine();

                // Create a datagram packet, containing a maximum buffer of 256 bytes
                DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), serverAddress, serverPort);
                if (connected) {
                    // Send a datagram packet from this socket
                    socket.send(packet);
                    System.out.println("Data Sent");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class Receiver extends Thread {
    DatagramSocket socket = null;

    Receiver(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            while (UDPClient.connected) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message from server: " + msg);
                if (msg.equals("Goodbye")) {
                    UDPClient.connected = false;
                    break;
                }
            }
            // Send a datagram packet from this socket
            DatagramPacket packet = new DatagramPacket("Goodbye".getBytes(), "Goodbye".length(),
                    InetAddress.getByName("localhost"), 12345);
            socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null)
                socket.close();
        }
    }
}