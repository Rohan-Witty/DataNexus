import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

interface Command extends Remote
{
    public String processRequest(String msg) throws RemoteException;
}

class RemoteCommand extends UnicastRemoteObject implements Command
{
    public RemoteCommand() throws RemoteException
    {
        super();
    }

    public String processRequest(String msg)
    {
        String response = "";
        String[] parts = msg.split(" ");
        String req = parts[0];

        if (req.equals("put")) {
            if (parts.length == 3) {
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
                        // Delete the value
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
        } else if (req.equals("exit")) {
            response = "Goodbye";
            // Write code to terminate the server here
            Terminate terminateThread = new Terminate();
            terminateThread.start();
            return response;
        }
        else {
            response = "Invalid request";
        }
        return response;
    }
}

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


public class RMIServer
{
    public static void main(String args[])
    {
        try
        {
            Command stub = new RemoteCommand();
            Naming.rebind("rmi://localhost:5000/sonoo",stub);
        }
        
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

}
