package sockets;

public class MessageStoC_Kick extends Message{
    public String sendingUser;
    public String targetUser;

    public MessageStoC_Kick(String sendingUser, String targetUser) {
        this.sendingUser = sendingUser;
        this.targetUser = targetUser;
    }

}
