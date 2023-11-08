import java.io.*;
import java.net.*;

public class TCPClient {
    public static void main(String[] args) {
        int BUFSIZE = 32;
        Socket socket = null;

        try {
            /* CREATE A TCP SOCKET */
            socket = new Socket("127.0.0.1", 12345); // Replace IP and port as needed

            System.out.println("Client Socket Created");

            /* GET SOCKET OUTPUT STREAM */
            OutputStream out = socket.getOutputStream();
            PrintWriter output = new PrintWriter(out, true);

            /* GET SOCKET INPUT STREAM */
            InputStream in = socket.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(in));

            boolean connected = true;
            while (connected) {
                /* SEND DATA */
                System.out.println("ENTER MESSAGE FOR SERVER with max 32 characters");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String msg = reader.readLine();

                output.println(msg);

                System.out.println("Data Sent");

                /* RECEIVE BYTES */
                char[] recvBuffer = new char[BUFSIZE];
                int bytesRecvd = input.read(recvBuffer, 0, BUFSIZE - 1);

                if (bytesRecvd < 0) {
                    System.out.println("Error while receiving data from server");
                    System.exit(0);
                }

                String receivedMessage = new String(recvBuffer, 0, bytesRecvd);
                System.out.println(receivedMessage);

                if (msg.equals(":exit")) {
                    connected = false;
                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
