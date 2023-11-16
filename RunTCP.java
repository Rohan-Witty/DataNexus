import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

/*
 * RunTCP is the main class that starts either the TCP server or client based on command line arguments.
 * If no arguments are provided, it starts the client; if "server" is provided, it starts the server.
 */
public class RunTCP {
    public static void main(String[] args) {
        /*
         * Start the server or client based on the command line arguments
         * If no arguments are provided, start the client
         * If the argument "server" is provided, start the server
         */
        boolean isServer = args.length > 0 && args[0].equals("server");
        if (isServer) {
            TCPServer server = new TCPServer();
            server.runServer();
        } else {
            String ipAddress = "localhost"; // Default IP address
            if (args.length > 0) {
                ipAddress = args[0];
            }
            TCPClient client = new TCPClient(ipAddress);
            client.runClient();
        }
    }
}

/*
 * TCPClient class represents a TCP client that connects to a server, sends
 * messages, and receives responses.
 */
class TCPClient {
    private final String ipAddress;
    private boolean test_case = false;

    TCPClient(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void runClient() {
        int BUFSIZE = 32;
        Socket socket = null;

        try {
            /* CREATE A TCP SOCKET */
            socket = new Socket(ipAddress, 12345); // Port number can be changed

            System.out.println("Client Socket Created");

            /* GET SOCKET OUTPUT STREAM */
            OutputStream out = socket.getOutputStream();
            PrintWriter output = new PrintWriter(out, true);

            /* GET SOCKET INPUT STREAM */
            InputStream in = socket.getInputStream();
            BufferedReader input = new BufferedReader(new InputStreamReader(in));

            boolean connected = true;
            if (!test_case) {
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
            } else {
                // Open test file
                File file = new File("testcase.txt");
                BufferedReader br = new BufferedReader(new FileReader(file));
                String msg;
                // For each line in the test file, send the message to the server and log the
                // time taken in file "TCPLatency.csv" along with the first word of message sent
                while ((msg = br.readLine()) != null) {
                    long startTime = System.nanoTime();
                    output.println(msg);
                    // Receive response from server
                    char[] recvBuffer = new char[BUFSIZE];
                    int bytesRecvd = input.read(recvBuffer, 0, BUFSIZE - 1);
                    if (bytesRecvd < 0) {
                        System.out.println("Error while receiving data from server");
                        System.exit(0);
                    }
                    long endTime = System.nanoTime();
                    String receivedMessage = new String(recvBuffer, 0, bytesRecvd);
                    // Log the time taken in file "TCPLatency.csv" along with the first word of
                    // message received
                    try {
                        FileWriter fw = new FileWriter("TCPLatency.csv", true);
                        fw.write(msg.split(" ")[0] + "," + (endTime - startTime) + "\n");
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                br.close();
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
 * TCPServer class represents a TCP server that listens for client connections
 * and spawns threads to handle them.
 */
class TCPServer {

    /* lock */
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

/*
 * ClientHandler is a thread responsible for handling client connections in the
 * TCPServer.
 */
class ClientHandler extends Thread {
    private final Socket clientSocket;
    /* lock */
    private static ReentrantLock lock = null;
    private static boolean running = true;

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
            while ((msg = input.readLine()) != null && running) {
                if (msg.equals("exit")) {
                    System.out.println("Client disconnected: " + clientSocket);
                    running = false;
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
            output.println("Goodbye");
            clientSocket.close();
            // Wait 10 seconds before terminating the server, closing any clients that send a request in that time
            Thread.sleep(10000);
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String processRequest(String msg) {
        /*
         * Uncomment the following line to simulate a slow server to test FIFO ordering
         * of requests
         */
        // try {
        // Thread.sleep(5000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        String response = "";
        String[] parts = msg.split(" ");
        String req = parts[0];

        if (req.equals("put")) {
            /* Code to add a new key-value pair to the database */
            if (parts.length >= 3) {
                String key = parts[1];
                String value = parts[2];

                try {
                    /* Open the file for reading as well as writing */
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
                        /* Delete the value */
                        file.seek(file.getFilePointer() - line.length() - 1);
                        file.writeBytes(" ");
                    }
                    /* Append the new key-value pair */
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
            /* Code to retrieve the value for a given key from the database */
            try {
                /* Open the file for reading */
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
            /* Code to delete a key-value pair from the database */
            try {
                /* Open the file for reading as well as writing */
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
                    /* Update the value */
                    file.seek(file.getFilePointer() - line.length() - 1);
                    /* Mark the line as deleted */
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
            /* Code to retrieve and return a portion of the database (if needed) */
            try {
                /* Open the file for reading as well as writing */
                RandomAccessFile file = new RandomAccessFile("database.txt", "r");
                String line;
                int size = 0;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(" ") || line.equals("")) {
                        continue;
                    }
                    line += "\n";
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
            /*
             * Command to clear out all deleted lines by making a copy of the file while
             * ommiting the deleted lines, deleting the original file,
             * and renaming the copy to the original file name
             */
            try {
                /* Open the file for reading as well as writing */
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
        } else if (req.equals("exit")) {
            /* Code to terminate the server */
            response = "Goodbye";
        } else {
            response = "Invalid request";
        }
        return response;
    }
}