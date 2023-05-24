package sockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public class ChatGuiClient extends Application {
    private Socket socket;
    private ObjectOutputStream socketOut;
    private ObjectInputStream socketIn;

    private final ObservableList<String> names = FXCollections.observableArrayList();
    private ToggleGroup group = new ToggleGroup();

    private Stage stage;
    private TextArea messageArea;
    private TextField textInput;
    private Button sendButton;

    private ServerInfo serverInfo;
    public String username;
    public ListView<String> listView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // If ip and port provided as command line arguments, use them
        List<String> args = getParameters().getUnnamed();
        if (args.size() == 2) {
            this.serverInfo = new ServerInfo(args.get(0), Integer.parseInt(args.get(1)));
        } else {
            // otherwise, use a Dialog.
            Optional<ServerInfo> info = getServerIpAndPort();
            if (info.isPresent()) {
                this.serverInfo = info.get();
            } else {
                Platform.exit();
                return;
            }
        }

        this.stage = primaryStage;
        BorderPane borderPane = new BorderPane();

        messageArea = new TextArea();
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
        borderPane.setCenter(messageArea);

        // active user list
        this.listView = new ListView<>();
        listView.setPrefSize(200, 250);
        listView.setEditable(false);
        listView.setItems(names);
        listView.setCellFactory(param -> new RadioListCell());
        borderPane.setLeft(listView);

        // At first, can't send messages - wait for WELCOME!
        textInput = new TextField();
        textInput.setEditable(false);
        textInput.setOnAction(e -> sendChatMessage());
        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendChatMessage());

        HBox hbox = new HBox();
        hbox.getChildren().addAll(new Label("Message: "), textInput, sendButton);
        HBox.setHgrow(textInput, Priority.ALWAYS);
        borderPane.setBottom(hbox);

        Scene scene = new Scene(borderPane, 400, 500);
        stage.setTitle("Chat Client");
        stage.setScene(scene);
        stage.show();

        socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
        socketOut = new ObjectOutputStream(socket.getOutputStream());
        socketIn = new ObjectInputStream(socket.getInputStream());

        // Start the socketListener
        ChatGuiSocketListener socketListener = new ChatGuiSocketListener(socketIn, this);

        // Handle close requests
        stage.setOnCloseRequest(e -> {
            sendMessage(new MessageCtoS_Quit());
            socketListener.appRunning = false;

            try {
                socket.close();
            } catch (IOException ex) {
                System.out.println("Exception caught: " + ex);
                ex.printStackTrace();
            }
        });

        new Thread(socketListener).start();
    }

    public void updateUserList(ArrayList<String> users) {
        Platform.runLater(() -> {
            names.clear();
            names.addAll(users);
        });
    }

    public void sendMessage(Message m) {
        try {
            socketOut.writeObject(m);
        } catch (IOException ex) {
            System.out.println("Exception caught when sending Message");
            ex.printStackTrace();
        }
    }

    private void sendChatMessage() {
        String msg = textInput.getText().trim();
        if (msg.length() == 0) {
            return;
        }
        textInput.clear();
        sendMessage(new MessageCtoS_Chat(username, getSelectedRecipient(), msg));
        if (!getSelectedRecipient().equals("Everyone")) {
            Platform.runLater(() -> {
                getMessageArea().appendText(username + " to " + getSelectedRecipient() + "(Private): " + msg);
            });
        }
    }

    public String getSelectedRecipient() {
        if (group.getSelectedToggle() == null) {
            return "Everyone";
        }
        return ((RadioButton) group.getSelectedToggle()).getText();
    }

    public ObjectOutputStream getSocketOut() {
        return socketOut;
    }

    public ObjectInputStream getSocketIn() {
        return socketIn;
    }

    public Stage getStage() {
        return stage;
    }

    public TextArea getMessageArea() {
        return messageArea;
    }

    public TextField getTextInput() {
        return textInput;
    }

    public Button getSendButton() {
        return sendButton;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public ObservableList<String> getNames() {
        return names;
    }

    private Optional<ServerInfo> getServerIpAndPort() {
        // In a more polished product, we probably would have the ip /port hardcoded
        // But this a great way to demonstrate making a custom dialog
        // Based on Custom Login Dialog from
        // https://code.makery.ch/blog/javafx-dialogs-official/

        // Create a custom dialog for server ip / port
        Dialog<ServerInfo> getServerDialog = new Dialog<>();
        getServerDialog.setTitle("Enter Server Info");
        getServerDialog.setHeaderText("Enter your server's IP address and port: ");

        // Set the button types.
        ButtonType connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        getServerDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Create the ip and port labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipAddress = new TextField();
        ipAddress.setPromptText("e.g. localhost, 127.0.0.1");
        grid.add(new Label("IP Address:"), 0, 0);
        grid.add(ipAddress, 1, 0);

        TextField port = new TextField();
        port.setPromptText("e.g. 54321");
        grid.add(new Label("Port number:"), 0, 1);
        grid.add(port, 1, 1);

        // Enable/Disable connect button depending on whether a username was entered.
        Node connectButton = getServerDialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        ipAddress.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Only allow numeric values
            if (!newValue.matches("\\d*"))
                port.setText(newValue.replaceAll("[^\\d]", ""));

            connectButton.setDisable(newValue.trim().isEmpty());
        });

        getServerDialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> ipAddress.requestFocus());

        // Convert the result to a ServerInfo object when the login button is clicked.
        getServerDialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new ServerInfo(ipAddress.getText(), Integer.parseInt(port.getText()));
            }
            return null;
        });

        return getServerDialog.showAndWait();
    }

    private class RadioListCell extends ListCell<String> {
        @Override
        public void updateItem(String obj, boolean empty) {
            super.updateItem(obj, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                RadioButton radioButton = new RadioButton(obj);
                radioButton.setToggleGroup(group);
                // Add Listeners if any
                setGraphic(radioButton);
            }
        }
    }
}