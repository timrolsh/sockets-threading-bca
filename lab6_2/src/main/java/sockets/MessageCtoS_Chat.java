package sockets;

public class MessageCtoS_Chat extends Message {
    public String sender;
    public String recipient;
    public String msg;
    public boolean publicMsg;

    public MessageCtoS_Chat(String sender, String recipient, String msg) {
        this.sender = sender;
        this.recipient = recipient;
        this.msg = msg;
        publicMsg = recipient == null || recipient.equals("Everyone");
    }

    public String toString() {
        return sender + " says \"" + msg + "\" to " + (publicMsg ? "everyone" : recipient);
    }
}
