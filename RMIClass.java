import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.*;
import java.rmi.server.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

/* RMIClass is the main class that either starts the RMI server or client based on the command line arguments. */

public class RMIClass {
    public static void main(String args[]) {
        /*
         * Start the server or client based on the command line arguments
         * If no arguments are provided, start the client
         * If the argument "server" is provided, start the server
         */

        boolean isServer = args.length > 0 && args[0].equals("server");
        if (isServer) {
            startServer();
        } else {
            startClient();
        }
    }

    /*
     * Starts the RMI server by creating a registry and binding the "Command" stub.
     */
    static void startServer() {
        try {
            /* Create a new instance of RemoteCommand */
            Command stub = new RemoteCommand();

            int port = 1099; // default RMI registry port
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("Command", stub);
            System.out.println("Server ready");
        }

        catch (Exception e) {
            System.out.println(e);
        }
    }

    /*
     * Starts the RMI client, looks up the "Command" stub, and processes user
     * commands.
     */
    static void startClient() {
        String host = null;
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            Command stub = (Command) registry.lookup("Command");

            boolean connected = true;
            while (connected) {
                /* SEND DATA */
                System.out.println("ENTER MESSAGE FOR SERVER with max 32 characters");
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String msg = reader.readLine();

                String response = stub.processRequest(msg);
                System.out.println("response: " + response);
                if (msg.equals("exit")) {
                    connected = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

/*
 * Terminate is a thread to terminate the server after a delay.
 */
class Terminate extends Thread {

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

/*
 * RemoteCommand implements the Command interface and provides methods for
 * processing client requests remotely.
 */
public class RemoteCommand extends UnicastRemoteObject implements Command {
    private static ReentrantLock lock = null;

    public RemoteCommand() throws RemoteException {
        super();
        lock = new ReentrantLock(true);
    }

    /**
     * Processes client requests and performs corresponding actions such as put,
     * get, del, exit, store, and clean.
     *
     * @param msg The client request message.
     * @return The response message after processing the request.
     * @throws RemoteException
     */
    public String processRequest(String msg) {
        /* Acquire the lock */
        lock.lock();
        /* Sleep for 2 seconds to simulate a long running process */
        // try {
        // Thread.sleep(2000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        String response = "";
        String[] parts = msg.split(" ");
        String req = parts[0];

        if (req.equals("put")) {
            /* Code to add a new key-value pair to the database */
            if (parts.length == 3) {
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
                    /* If the key was found, delete the old value */
                    if (found) {
                        /* Delete the value */
                        file.seek(file.getFilePointer() - line.length() - 1);
                        file.writeBytes(" ");
                    }
                    /* Append the new key-value pair to the end of the file */
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

        } else if (req.equals("exit")) {
            /* Code to terminate the server */
            response = "Goodbye";
            Terminate terminateThread = new Terminate();
            terminateThread.start();
            return response;
        } else if (req.equals("store")) {
            /* Code to retrieve and return a portion of the database (if needed) */
            try {
                /* Open the file for reading as well as writing */
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
        } else {
            response = "Invalid request";
        }
        /* Release the lock */
        lock.unlock();
        return response;
    }
}

/*
 * Command is the interface defining the remote method processRequest.
 */
public interface Command extends Remote {
    public String processRequest(String msg) throws RemoteException;
}