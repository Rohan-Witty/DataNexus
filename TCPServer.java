import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public class Server {
    private static final int BUFFERSIZE = 1024;
    private static final int MAXPENDING = 10;

    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(12345); // Port number can be changed
            System.out.println("Server Socket Created");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);

                ClientHandler clientThread = new ClientHandler(clientSocket);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class ClientHandler extends Thread {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

            String msg;
            while ((msg = input.readLine()) != null) {
                if (msg.equals(":exit")) {
                    System.out.println("Client disconnected: " + clientSocket);
                    break;
                } else {
                    String response = processRequest(msg);
                    output.println(response);
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String processRequest(String msg) {
        String response;
        // Open file database.txt
        return "Processed: " + msg;
    }
}
