The code was tested on a Linux machine having java 21.0.1 2023-10-17 LTS.

While it should run on other systems because of the Java Virtual Machine, please make sure you have a version of java that supports RMI and Reentrant Locks installed on your machine.

## How to run the code

### Step 1: Run a Server

1.a) For TCP, run the following command in the terminal:

```
java -cp ./bin/ Server.TCPServer
```