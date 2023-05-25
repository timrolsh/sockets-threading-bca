package sockets;

import java.io.ObjectInputStream;
import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.util.Callback;

public class ChatGuiSocketListener implements Runnable {

    private ObjectInputStream socketIn;
    private ChatGuiClient chatGuiClient;
    private String username = null;

    // volatile guarantees that different threads reading the same variable will
    // always see the latest write.
    // volatile variables are not stored in caches.
    volatile boolean appRunning = false;

    public ChatGuiSocketListener(ObjectInputStream socketIn,
            ChatGuiClient chatClient) {
        this.socketIn = socketIn;
        this.chatGuiClient = chatClient;
    }

    private void processKickMessage(MessageStoC_Kick m) {
        // this user has been kicked from the chatroom
        if (m.targetUser.equals(this.username)) {
            chatGuiClient.getMessageArea().appendText("You have been kicked from the chatroom by " + m.sendingUser
                    + ". You will no longer be able to send or receive messages and may close this window. ");
            chatGuiClient.getTextInput().setEditable(false);
            chatGuiClient.getSendButton().setDisable(true);
        } else {
            Platform.runLater(() -> {
                chatGuiClient.getMessageArea()
                        .appendText(m.targetUser + " has been kicked from the chatroom by " + m.sendingUser + "\n");
            });
            updateUserList();
        }
    }

    private void processWelcomeMessage(MessageStoC_Welcome m) {
        String user = m.userName;
        if (user.equals(this.username)) {
            Platform.runLater(() -> {
                chatGuiClient.getStage().setTitle("Chatter - " + username);
                chatGuiClient.getTextInput().setEditable(true);
                chatGuiClient.getSendButton().setDisable(false);
                chatGuiClient.getMessageArea().appendText("Welcome to the chat, " + username + "\n");
            });
        } else {
            Platform.runLater(() -> {
                chatGuiClient.getMessageArea().appendText(m.userName + " joined the chat!\n");
            });
        }
        updateUserList();
    }

    private void processChatMessage(MessageStoC_Chat m) {
        Platform.runLater(() -> {
            if (m.publicMsg) {
                chatGuiClient.getMessageArea().appendText(m.sender + ": " + m.msg + "\n");
            }
            if (m.recipient.equals(username)) {
                chatGuiClient.getMessageArea().appendText(m.sender + " (Private): " + m.msg + "\n");
            }
        });
    }

    public void processListMessage(MessageStoC_List m) {
        m.users.remove(this.username);
        m.users.sort(String::compareToIgnoreCase);
        m.users.add(0, "Everyone");
        this.chatGuiClient.updateUserList(m.users);
    }

    private void processExitMessage(MessageStoC_Exit m) {
        Platform.runLater(() -> {
            chatGuiClient.getMessageArea().appendText(m.userName + " has left the chat!\n");
            updateUserList();
        });
    }

    private void updateUserList() {
        chatGuiClient.sendMessage(new MessageCtoS_List());
    }

    public void run() {
        try {
            appRunning = true;

            // Ask the gui to show the username dialog and update username
            // getName launches a modal, therefore needs to run in the JavaFx thread, hence
            // wrapping this with run later
            // save the username for later comparison in processWelcomeMessage
            // Send to the server

            Platform.runLater(() -> {
                this.username = getName();
                chatGuiClient.username = this.username;
                chatGuiClient.sendMessage(new MessageCtoS_Join(username));
                // if the user is an admin, give them the ability to kick people and place Kick
                // buttons on their screen. This removes the private messaging feature, so for
                // now, admins will not be able to send private messages.
                if (username.equals("admin")) {
                    chatGuiClient.listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
                        @Override
                        public ListCell<String> call(ListView<String> param) {
                            return new ListCell<String>() {
                                @Override
                                protected void updateItem(String item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        setText(null);
                                        setGraphic(null);
                                    } else {
                                        if (item != "Everyone")
                                        {
                                            Button button = new Button("Kick");
                                            button.setOnAction(event -> {
                                                chatGuiClient.sendMessage(new MessageCtoS_Kick(username, item));
                                            });
                                            setGraphic(button);
                                            setText(item);
                                        }
                                        else
                                        {
                                            setText("Everyone (Admin can't DM)");
                                            setGraphic(null);
                                        }
                                    }
                                }
                            };
                        }
                    });
                }
            });

            // If the user closes the UI window in ChatGuiClient, appRunning is changed to
            // false to exit this loop.
            while (appRunning) {
                Message msg = (Message) socketIn.readObject();
                if (msg instanceof MessageStoC_Welcome) {
                    processWelcomeMessage((MessageStoC_Welcome) msg);
                } else if (msg instanceof MessageStoC_Chat) {
                    processChatMessage((MessageStoC_Chat) msg);
                } else if (msg instanceof MessageStoC_Exit) {
                    processExitMessage((MessageStoC_Exit) msg);
                } else if (msg instanceof MessageStoC_List) {
                    processListMessage((MessageStoC_List) msg);
                } else if (msg instanceof MessageStoC_Kick) {
                    processKickMessage((MessageStoC_Kick) msg);
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

    private String getName() {
        String username = "";
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Enter Chat Name");
        nameDialog.setHeaderText("Please enter your username.");
        nameDialog.setContentText("Name: ");

        while (username.equals("")) {
            Optional<String> name = nameDialog.showAndWait();
            if (!name.isPresent() || name.get().trim().equals(""))
                nameDialog.setHeaderText("You must enter a nonempty name: ");
            else
                username = name.get().trim();
        }
        return username;
    }

}
