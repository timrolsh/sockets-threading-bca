package sockets;

public class MessageCtoS_Kick extends Message{
    public String sendingUser;
    public String targetUser;

    public MessageCtoS_Kick(String sendingUser, String targetUser) {
        this.sendingUser = sendingUser;
        this.targetUser = targetUser;
    }

}
