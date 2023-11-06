import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int BUFFERSIZE = 1024;
    private static final int MAXPENDING = 10;

    // lock
    private static ReentrantLock lock = null;
    public static void main(String[] args) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(12345); // Port number can be changed
            System.out.println("Server Socket Created");
            lock = new ReentrantLock(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);

                ClientHandler clientThread = new ClientHandler(clientSocket, lock);
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
    // lock
    private static ReentrantLock lock = null;

    public ClientHandler(Socket socket, ReentrantLock lock) {
        this.clientSocket = socket;
        this.lock = lock;
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
                    lock.lock();
                    try {
                        String response = processRequest(msg);
                        output.println(response);
                    } finally {
                        lock.unlock();
                    }
                }
            }

            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String processRequest(String msg) {
        String response = "";
        String[] parts = msg.split(" ");
        String req = parts[0];

        if (req.equals("put")) {
            if (parts.length >= 3) {
                int key = Integer.parseInt(parts[1]);
                String value = parts[2];

                try {
                    // Open the file for reading as well as writing
                    RandomAccessFile file = new RandomAccessFile("data.txt", "rw");
                    String line;
                    boolean found = false;
                    while ((line = file.readLine()) != null) {
                        String[] lineParts = line.split(" ");
                        int lineKey = Integer.parseInt(lineParts[0]);
                        if (lineKey == key) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        // Update the value
                        file.seek(file.getFilePointer() - line.length() - 1);
                        file.writeBytes(key + " " + value + "\n");
                        response = "Updated value for key " + key;
                    } else {
                        // Append the new key-value pair
                        file.seek(file.length());
                        file.writeBytes(key + " " + value + "\n");
                        response = "Added new key-value pair";
                    }
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    response = "Error while processing request";
                }
            } else {
                response = "Invalid request";
            }
        }

        return response;
    }
}