package sockets;

public class ServerInfo {
    public final String serverAddress;
    public final int serverPort;

    public ServerInfo(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
}