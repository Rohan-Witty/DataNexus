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

By default, the server is assumed to be running on the same machine as the client. If the server is running on a different machine, please add the IP address of the server as a command line argument. <server_ip_address> is optional, if it is not given (ie no command line arguments provided), the client would run with localhost as the server.

2.a) For TCP, run the following command in the terminal:

```
java RunTCP.java <server_ip_address>
```

2.b) For UDP, run the following command in the terminal:

```
java RunUDP.java <server_ip_address>
```

2.c) For RMI, run the following command in the terminal:

```
java RunRMI.java <server_ip_address>
```

### Step 3: Give commands

The client will ask for commands to be given. The commands are as follows:

```
1. put <key> <value>
2. get <key>
3. del <key>
4. store
5. exit
6. clean
```

clean is an additional command that is not part of the assignment. It is used to compress the database file and remove any deleted entries from the database file.
