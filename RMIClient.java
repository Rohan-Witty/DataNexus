import java.rmi.*;

public class RMIClient
{
    public static void main(String args[])
    {
        try
        {
            RemoteCommand stub = (RemoteCommand) Naming.lookup("rmi://localhost:5000/sonoo");
            System.out.println(stub.processRequest("put 1 2"));
        }

        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}
