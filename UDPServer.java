import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

public class UDPServer {
    // lock
    private static ReentrantLock lock = null;
    private static DatagramSocket serverSocket = null;
    private static final int BUFFERSIZE = 1024;
    private static boolean connected = true;

    public static void close() {
        connected = false;
    }

    public static void main(String[] args) {
        try {
            lock = new ReentrantLock(true);
            serverSocket = new DatagramSocket(12345); // Port number can be changed

            while (connected) {
                DatagramPacket packet = new DatagramPacket(new byte[BUFFERSIZE], BUFFERSIZE);
                serverSocket.receive(packet);
                DatagramHandler handler = new DatagramHandler(serverSocket, packet, lock);
                handler.start();
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
                }

                catch (InterruptedException e) {
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
            }

            finally {
                lock.unlock();
            }

            if (toExit) {
                UDPServer.close();
            }
        }

        catch (IOException e) {
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
            }

            else {
                response = "Invalid request";
            }
        }

        else if (req.equals("get")) {
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
        }

        else if (req.equals("del")) {
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

        }

        else if (req.equals("exit")) {
            response = "Goodbye";
            toExit = true;
        }

        else {
            response = "Invalid request";
        }

        return response;
    }
}