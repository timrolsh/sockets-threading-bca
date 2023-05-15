package lab6_1;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ChatServerSocketListener implements Runnable {
    private ClientConnectionData client;
    private List<ClientConnectionData> clientList;
    private String adminUsername = "admin";

    public ChatServerSocketListener(ClientConnectionData client, List<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    private void processChatMessage(MessageCtoS_Chat m) {
        System.out.println("Chat received from " + client.getUserName() + " - broadcasting");
        broadcast(new MessageStoC_Chat(client.getUserName(), m.msg), client);
    }

    private void processListMessage(MessageCtoS_List m) {
        System.out.println("Request from user " + m.name + " to see list: sending...");
        ArrayList<String> list = new ArrayList<>();
        for (ClientConnectionData client : clientList) {
            list.add(client.getUserName());
        }

        individualSend(new MessageStoC_List(list), client);

        // send back the list of people to the client that requested the list itself,
        // make a MessageStoS_List.java file
    }

    private void processKickMessage(MessageCtoS_Kick m) {
        for (ClientConnectionData client : clientList) {
            if (client.getUserName().equals(m.username)) {
                try {
                    client.getOut().writeObject(new MessageStoC_Kick());
                    client.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientList.remove(client);
            }
        }

    }

    public void individualSend(Message m, ClientConnectionData client) {
        try {
            client.getOut().writeObject(m);
        } catch (IOException ex) {
            System.out.println(ex);
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
                }

                else if (msg instanceof MessageCtoS_Kick) {
                    // make sure client is an admin
                    if (client.getUserName().equals(adminUsername)) {
                        processKickMessage((MessageCtoS_Kick) msg);
                    } else {
                        individualSend(new MessageStoC_Denied(), client);
                    }

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
