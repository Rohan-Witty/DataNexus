import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

public class RunTCP {
    public static void main(String[] args) {
        // Start the server or client based on the command line arguments
        // If no arguments are provided, start the client
        // If the argument "server" is provided, start the server
        boolean isServer = args.length > 0 && args[0].equals("server");
        if (isServer) {
            TCPServer server = new TCPServer();
            server.runServer();
        } else {
            TCPClient client = new TCPClient();
            client.runClient();
        }
    }
}

class TCPClient {
    public void runClient() { 
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

                if (receivedMessage.startsWith("Goodbye")) {
                    connected = false;
                    System.out.println("Disconnecting from server");

                }
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class TCPServer {

    // lock
    private static ReentrantLock lock = null;

    public void runServer() {
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
        ClientHandler.lock = lock;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

            String msg;
            while ((msg = input.readLine()) != null) {
                if (msg.equals("exit")) {
                    System.out.println("Client disconnected: " + clientSocket);
                    output.println("Goodbye");
                    break;
                } else {
                    lock.lock();
                    try {
                        System.out.println("Received message: " + msg);
                        String response = processRequest(msg);
                        output.println(response);
                        System.out.println("Sent response: " + response);
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
        // Uncomment the following line to simulate a slow server to test FIFO ordering
        // of requests
        // try {
        // Thread.sleep(5000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        String response = "";
        String[] parts = msg.split(" ");
        String req = parts[0];

        if (req.equals("put")) {
            if (parts.length >= 3) {
                String key = parts[1];
                String value = parts[2];

                try {
                    // Open the file for reading as well as writing
                    RandomAccessFile file = new RandomAccessFile("database.txt", "rw");
                    String line;
                    boolean found = false;
                    while ((line = file.readLine()) != null) {
                        if (line.startsWith(" ")) {
                            continue;
                        }
                        String[] lineParts = line.split(" ");
                        String lineKey = lineParts[0];
                        if (lineKey.equals(key)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        // Delte the value
                        file.seek(file.getFilePointer() - line.length() - 1);
                        file.writeBytes(" ");
                    }
                    // Append the new key-value pair
                    file.seek(file.length());
                    file.writeBytes(key + " " + value + "\n");
                    response = "Added new key-value pair";

                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    response = "Error while processing request";
                }
            } else {
                response = "Invalid request";
            }
        } else if (req.equals("get")) {
            try {
                // Open the file for reading
                RandomAccessFile file = new RandomAccessFile("database.txt", "r");
                String line;
                boolean found = false;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(" ")) {
                        continue;
                    }
                    String[] lineParts = line.split(" ");
                    String lineKey = lineParts[0];
                    String lineValue = lineParts[1];
                    if (lineKey.equals(parts[1])) {
                        found = true;
                        response = lineValue;
                        break;
                    }
                }
                if (!found) {
                    response = "Key not found";
                }
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
                response = "Error while processing request";
            }
        } else if (req.equals("del")) {
            try {
                // Open the file for reading as well as writing
                RandomAccessFile file = new RandomAccessFile("database.txt", "rw");
                String line;
                boolean found = false;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(" ")) {
                        continue;
                    }
                    String[] lineParts = line.split(" ");
                    String lineKey = lineParts[0];
                    if (lineKey.equals(parts[1])) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    // Update the value
                    file.seek(file.getFilePointer() - line.length() - 1);
                    // Mark the line as deleted
                    file.writeBytes(" ");
                    response = "Deleted key " + parts[1];
                } else {
                    response = "Key not found";
                }
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
                response = "Error while processing request";
            }
        } else if (req.equals("store")) {
            try {
                // Open the file for reading as well as writing
                RandomAccessFile file = new RandomAccessFile("database.txt", "r");
                String line;
                int size = 0;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(" ")) {
                        continue;
                    }

                    if (line.length() + size < 65000) {
                        size += line.length();
                        response += line;
                    } else {
                        response = "TRIMMED: \n" + response;
                        response += line.substring(0, 65000 - size);
                        break;
                    }
                }
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
                response = "Error while processing request";
            }

        } else if (req.equals("clean")) {
            // Command to clear out all deleted lines by making a copy of the file while ommiting the deleted lines, deleting the original file, 
            // and renaming the copy to the original file name
            try {
                // Open the file for reading as well as writing
                RandomAccessFile file = new RandomAccessFile("database.txt", "r");
                RandomAccessFile fileCopy = new RandomAccessFile("databaseCopy.txt", "rw");
                String line;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(" ")) {
                        continue;
                    }
                    fileCopy.writeBytes(line + "\n");
                }
                file.close();
                fileCopy.close();
                File database = new File("database.txt");
                database.delete();
                File databaseCopy = new File("databaseCopy.txt");
                databaseCopy.renameTo(database);
                response = "Cleaned database";
            } catch (IOException e) {
                e.printStackTrace();
                response = "Error while processing request";
            }
        }
        else if (req.equals("exit")) {
            response = "Goodbye";
        }
        else {
            response = "Invalid request";
        }
        return response;
    }
}