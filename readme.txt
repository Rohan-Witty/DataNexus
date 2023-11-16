The code was tested on a Linux machine having java 21.0.1 2023-10-17 LTS.

While it should run on other systems because of the Java Virtual Machine, please make sure you have a version of java that supports RMI and Reentrant Locks installed on your machine.

## How to run the code

### Step 1: Run a Server

1.a) For TCP, run the following command in the terminal:

```
java RunTCP.java server
```

1.b) For UDP, run the following command in the terminal:

```
java RunUDP.java server
```

1.c) For RMI, run the following command in the terminal:

```
java RunRMI.java server
```

### Step 2: Run a Client

By default, the server is assumed to be running on the same machine as the client. If the server is running on a different machine, please add the IP address of the server as a command line argument.

