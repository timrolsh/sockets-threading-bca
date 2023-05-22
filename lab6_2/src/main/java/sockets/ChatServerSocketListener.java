package sockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ChatServerSocketListener implements Runnable {
    private ClientConnectionData client;
    private List<ClientConnectionData> clientList;

    public ChatServerSocketListener(ClientConnectionData client, List<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    private void processChatMessage(MessageCtoS_Chat m) {
        System.out.println("Chat received: " + m);
        broadcast(new MessageStoC_Chat(m.sender, m.recipient, m.msg), null);
    }

    private void processListMessage(MessageCtoS_List m) {
        System.out.println("List request received from " + client.getUserName() + " - sending list");

        ArrayList<String> users = new ArrayList<String>();
        for (int i = 0; i < clientList.size(); i++) {
            users.add(clientList.get(i).getUserName());
        }
        try {
            client.getOut().writeObject(new MessageStoC_List(users));
        } catch (IOException ex) {
            System.out.println("Error sending list to " + client.getUserName());
            ex.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all clients connected to the server.
     */
    public void broadcast(Message m, ClientConnectionData skipClient) {
        try {
            System.out.println("broadcasting: " + m);
            for (ClientConnectionData c : clientList) {
                // if c equals skipClient, then c.
                // or if c hasn't set a userName yet (still joining the server)
                if ((c != skipClient) && (c.getUserName() != null)) {
                    c.getOut().writeObject(m);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = client.getInput();

            MessageCtoS_Join joinMessage = (MessageCtoS_Join) in.readObject();
            client.setUserName(joinMessage.userName);

            // Broadcast the welcome back to the client that joined.
            // Their UI can decide what to do with the welcome message.
            broadcast(new MessageStoC_Welcome(joinMessage.userName), null);

            while (true) {
                Message msg = (Message) in.readObject();
                if (msg instanceof MessageCtoS_Quit) {
                    break;
                } else if (msg instanceof MessageCtoS_Chat) {
                    processChatMessage((MessageCtoS_Chat) msg);
                } else if (msg instanceof MessageCtoS_List) {
                    processListMessage((MessageCtoS_List) msg);
                } else {
                    System.out.println("Unhandled message type: " + msg.getClass());
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " +
                        client.getName());
            } else {
                System.out.println(ex);
                ex.printStackTrace();
            }
        } finally {
            // Remove client from clientList
            clientList.remove(client);

            // Notify everyone that the user left.
            broadcast(new MessageStoC_Exit(client.getUserName()), client);

            try {
                client.getSocket().close();
            } catch (IOException ex) {
            }
        }
    }

}
