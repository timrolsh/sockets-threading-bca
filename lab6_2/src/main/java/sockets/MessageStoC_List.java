package sockets;

import java.util.ArrayList;

public class MessageStoC_List extends Message {
    public ArrayList<String> users;

    public MessageStoC_List(ArrayList<String> users) {
        this.users = users;
    }

    public String toString() {
        return "List of currently connected users:\n" + users;
    }
}
