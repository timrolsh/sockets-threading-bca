package lab6_1;

import java.util.ArrayList;

public class MessageStoC_List extends Message {
    public ArrayList<String> usernames;

    public MessageStoC_List(ArrayList<String> usernames) {
        this.usernames = usernames;
    }
}
