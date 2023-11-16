import java.net.*;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class RunUDP {
    public static void main(String[] args) {
        // Start the server or client based on the command line arguments
        // If no arguments are provided, start the client
        // If the argument "server" is provided, start the server
        boolean isServer = args.length > 0 && args[0].equals("server");
        if (isServer) {
            UDPServer server = new UDPServer();
            server.startServer();
        } else {
            String ipAddress = "127.0.0.1";
            if (args.length > 0) {
                ipAddress = args[0];
            }
            UDPClient client = new UDPClient();
            client.startClient(ipAddress);
        }
    }
}

class UDPServer {
    // lock
    private static ReentrantLock lock = null;
    private static DatagramSocket serverSocket = null;
    private static final int BUFFERSIZE = 1024;
    private static boolean connected = true;

    public static void close() {
        connected = false;
    }

    public void startServer() {
        try {
            lock = new ReentrantLock(true);
            serverSocket = new DatagramSocket(12345); // Port number can be changed

            while (connected) {
                DatagramPacket packet = new DatagramPacket(new byte[BUFFERSIZE], BUFFERSIZE);
                serverSocket.receive(packet);
                DatagramHandler handler = new DatagramHandler(serverSocket, packet, lock);
                handler.start();
                // If the client sends the exit command, close the server
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.equals("exit")) {
                    connected = false;
                    break;
                }
            }
        }

        catch (IOException e) {
            e.printStackTrace();
        }

        finally {
            if (serverSocket != null) {
                // wait for all threads to finish
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                serverSocket.close();
            }
        }

    }
}

class DatagramHandler extends Thread {
    private DatagramSocket serverSocket = null;
    private DatagramPacket packet = null;
    private boolean toExit = false;
    private ReentrantLock lock = null;

    public DatagramHandler(DatagramSocket socket, DatagramPacket packet, ReentrantLock lock) {
        this.serverSocket = socket;
        this.packet = packet;
        this.lock = lock;
    }

    @Override
    public void run() {
        try {
            String msg = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Client connected: " + packet.getAddress() + ":" + packet.getPort());
            System.out.println("Message from client: " + msg);
            lock.lock();
            try {
                String response = processRequest(msg);
                System.out.println("Response: " + response);
                packet.setData(response.getBytes());
                serverSocket.send(packet);
            } finally {
                lock.unlock();
            }

            if (toExit) {
                UDPServer.close();
            }
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
                }

                catch (IOException e) {
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
            }

            catch (IOException e) {
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
                }

                else {
                    response = "Key not found";
                }

                file.close();
            }

            catch (IOException e) {
                e.printStackTrace();
                response = "Error while processing request";
            }

        } else if (req.equals("exit")) {
            response = "Goodbye";
            toExit = true;
        } else if (req.equals("store")) {
            try {
                // Open the file for reading as well as writing
                RandomAccessFile file = new RandomAccessFile("database.txt", "r");
                String line;
                int size = 0;
                while ((line = file.readLine()) != null) {
                    if (line.startsWith(" ") || line.length() == 0) {
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
            // Command to clear out all deleted lines by making a copy of the file while
            // ommiting the deleted lines, deleting the original file,
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
        // else if (req.equals("Goodbye")) {
        // response = "Closing connection";
        // }
        else {
            response = "Invalid request";
        }

        return response;
    }
}

class UDPClient {
    public static boolean connected = true;
    public boolean test_case = true;
    // Array of start times
    public static long[] start_times = new long[1001];
    public static long[] end_times = new long[1001];
    public static String[] commands = new String[1001];

    public void startClient(String ipAddress) {
        try {
            // Create a datagram socket, look for the first available port
            DatagramSocket socket = new DatagramSocket(54321);
            InetAddress serverAddress = InetAddress.getByName(ipAddress);
            int serverPort = 12345;
            // Start a thread to receive messages
            Receiver receiver = new Receiver(socket);
            receiver.start();
            if (test_case) {
                // for each line of test case, send a message
                // Open test file
                File file = new File("testcase.txt");
                BufferedReader br = new BufferedReader(new FileReader(file));
                String msg;
                int i = 0;
                // For each line in the test file, send the message to the server and log the
                // time taken in file "UDPLatency.csv" along with the first word of message sent
                while ((msg = br.readLine()) != null) {
                    // Sleep for 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Create a datagram packet, containing a maximum buffer of 256 bytes
                    DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), serverAddress,
                            serverPort);
                    // Send a datagram packet from this socket
                    socket.send(packet);
                    // Log the time
                    start_times[i] = System.nanoTime();
                    commands[i] = msg.split(" ")[0];
                    i++;
                }
                // Close the file
                br.close();
                System.out.println("Test cases sent");
                // Open file to write the latency
                FileWriter latency = new FileWriter("UDPLatency.csv");
                latency.write("Command,Latency\n");
                // Wait for all threads to finish
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Write the latency to the file
                for (int j = 0; j < i; j++) {
                    latency.write(commands[j] + "," + (end_times[j] - start_times[j]) + "\n");
                    // System.out.println(commands[j] + "," + (end_times[j] - start_times[j]));
                }
                // Close the file
                latency.close();
            }
            while (connected) {
                /* SEND DATA */
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!connected)
                    break;
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
    public static int counter = 0;
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
                // Log the time
                UDPClient.end_times[counter] = System.nanoTime();
                counter++;
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message from server: " + msg);
                if (msg.equals("Goodbye")) {
                    UDPClient.connected = false;
                    break;
                }
            }
            // Send a datagram packet from this socket
            // DatagramPacket packet = new DatagramPacket("Goodbye".getBytes(),
            // "Goodbye".length(),
            // InetAddress.getByName("localhost"), 12345);
            // socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null)
                socket.close();
        }
    }
}