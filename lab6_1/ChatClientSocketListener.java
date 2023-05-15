package lab6_1;

import java.io.ObjectInputStream;

public class ChatClientSocketListener implements Runnable {
    private ObjectInputStream socketIn;

    public ChatClientSocketListener(ObjectInputStream socketIn) {
        this.socketIn = socketIn;
    }

    private void processChatMessage(MessageStoC_Chat m) {
        System.out.println(m.userName + ": " + m.msg);
    }

    private void processWelcomeMessage(MessageStoC_Welcome m) {
        System.out.println(m.userName + " joined the server!");
    }

    private void processExitMessage(MessageStoC_Exit m) {
        System.out.println(m.userName + " left the server!");
    }

    private void processListMessage(MessageStoC_List m) {
        System.out.print(m.messageContent);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message msg = (Message) socketIn.readObject();
                if (msg instanceof MessageStoC_Welcome) {
                    processWelcomeMessage((MessageStoC_Welcome) msg);
                } else if (msg instanceof MessageStoC_Chat) {
                    processChatMessage((MessageStoC_Chat) msg);
                } else if (msg instanceof MessageStoC_Exit) {
                    processExitMessage((MessageStoC_Exit) msg);
                } else if (msg instanceof MessageStoC_List) {
                    processListMessage((MessageStoC_List) msg);
                } else if (msg instanceof MessageStoC_Denied) {
                    System.out.println(
                            "The server denied you from performing this action because you are not an admin. ");
                } else if (msg instanceof MessageStoC_Kick) {
                    System.out.println("You have been kicked from the chat room by an admin.");
                } else {
                    System.out.println("Unhandled message type: " + msg.getClass());
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
        } finally {
            System.out.println("Client Listener exiting");
        }
    }
}
