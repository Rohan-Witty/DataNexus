#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#define BUFFERSIZE 1024
#define MAXPENDING 10

int main()
{
    int sockfd = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (sockfd < 0)
    {
        printf("Error in opening a socket");
        exit(0);
    }
    printf("Server Socket Created\n");
    struct sockaddr_in serverAddress, clientAddress;
    memset(&serverAddress, 0, sizeof(serverAddress));
    serverAddress.sin_family = AF_INET;
    serverAddress.sin_port = htons(12345);
    serverAddress.sin_addr.s_addr = htonl(INADDR_ANY);
    printf("Server address assigned\n");
    int temp = bind(sockfd, (struct sockaddr *)&serverAddress,
                    sizeof(serverAddress));
    if (temp < 0)
    {
        printf("Error while binding\n");
        exit(0);
    }
    printf("Binding successful\n");
    int temp1 = listen(sockfd, MAXPENDING);
    if (temp1 < 0)
    {
        printf("Error in listen");
        exit(0);
    }
    printf("Now Listening\n");
    char msg[BUFFERSIZE];
    int clientLength = sizeof(clientAddress);
    int newSocket, childpid;
    while (1)
    {
        newSocket = accept(sockfd, (struct sockaddr *)&clientAddress, &clientLength);
        if (newSocket < 0)
        {
            exit(1);
        }
        printf("Connection accepted from %s:%d\n", inet_ntoa(clientAddress.sin_addr),
               ntohs(clientAddress.sin_port));
        if ((childpid = fork()) == 0)
        {
            close(sockfd);
            while (1)
            {
                recv(newSocket, msg, 1024, 0);
                if (strcmp(msg, ":exit") == 0)
                {
                    printf("Disconnected from %s:%d\n", inet_ntoa(clientAddress.sin_addr),
                           ntohs(clientAddress.sin_port));
                    break;
                }
                else
                {
                    printf("Client: %s\n", msg);
                    char *buf;
                    char *req;
                    req = strtok(msg, " \n");
                    if (strncmp(req, "bye", 3) == 0)
                    {
                        strcpy(msg, "Goodbye!");
                    }
                    int key = atoi(strtok(NULL, " \n"));
                    printf("Request: %s\n", req);
                    if (strcmp(req, "put") == 0)
                    {
                        char *value_str = strtok(NULL, " \n");
                        printf("Key = %d, Val = %s\n", key, buf);
                        FILE *fp = fopen("database.txt", "a+");
                        int readKey;
                        char readVal[BUFFERSIZE];
                        int writ = 0;
                        char localBuf[BUFFERSIZE];
                        while (fgets(localBuf, sizeof(localBuf), fp) != NULL)
                        {
                            int curKey = atoi(strtok(localBuf, " \n"));
                            buf = strtok(NULL, " \n");
                            if (curKey == key)
                            {
                                printf("Error: key already present");
                                sprintf(msg, "Key Present");
                                writ = 1;
                                break;
                            }
                        }

                        if (writ == 0)
                        {
                            printf("Key written\n");
                            fprintf(fp, "%d %s\n", key, value_str);
                            sprintf(msg, "OK");
                        }
                        fclose(fp);
                    }
                    else if (strcmp(req, "get") == 0)
                    {
                        FILE *fp = fopen("database.txt", "r");
                        char localBuf[BUFFERSIZE];
                        int got = 0;
                        while (fgets(localBuf, sizeof(localBuf), fp) != NULL)
                        {
                            int curKey = atoi(strtok(localBuf, " \n"));
                            buf = strtok(NULL, " \n");
                            if (curKey == key)
                            {
                                strcpy(msg, buf);
                                got = 1;
                                break;
                            }
                        }
                        if (!got)
                        {
                            sprintf(msg, "Not found key %d\n", key);
                        }
                    }
                    else if (strcmp(req, "del") == 0)
                    {
                        FILE *fp = fopen("database.txt", "r");
                        char localBuf[BUFFERSIZE];
                        int got = 0;
                        while (fgets(localBuf, sizeof(localBuf), fp) != NULL)
                        {
                            int curKey = atoi(strtok(localBuf, " \n"));
                            buf = strtok(NULL, " \n");
                            if (curKey == key)
                            {
                                got = 1;
                                break;
                            }
                        }
                        if (!got)
                        {
                            sprintf(msg, "Not found key %d\n", key);
                        }
                        else
                        {
                            // Rewind to beginning of line and put the value to as -1
                            fseek(fp, -strlen(buf) - 1, SEEK_CUR);
                            fprintf(fp, "-1\n");
                            sprintf(msg, "OK");
                        }
                        fclose(fp);
                    }
                    else
                    {
                        sprintf(msg, "Invalid request");
                    }
                    send(newSocket, msg, strlen(msg) + 1, 0);
                    bzero(msg, sizeof(msg));
                }
            }
        }
    }
    close(newSocket);
}