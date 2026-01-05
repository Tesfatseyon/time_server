package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import server.TimeServerInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TimeClient extends Application {
    private Label ipLabel;
    private Label timeLabel;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Time Client (JavaFX)");

        // Labels
        ipLabel = new Label("Not registered yet");
        ipLabel.setFont(Font.font("Segoe UI", 14));

        timeLabel = new Label("Press button to get time");
        timeLabel.setFont(Font.font("Consolas", 24));
        timeLabel.setTextFill(Color.DODGERBLUE);

        statusLabel = new Label("Status: Disconnected");
        statusLabel.setFont(Font.font("Segoe UI", 12));
        statusLabel.setTextFill(Color.GRAY);

        // Button
        
        Button fetchButton = new Button("Register & Get Time");
        fetchButton.setOnAction(e -> fetchTime());

        VBox root = new VBox(15, ipLabel, timeLabel, fetchButton, statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 400, 250);
        stage.setScene(scene);
        stage.show();
    }

    private void fetchTime() {
        new Thread(() -> {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                TimeServerInterface server = (TimeServerInterface) registry.lookup("TimeServer");

                String clientName = "FXClient";
                String ip = server.registerClient(clientName);
                String time = server.getTime();

                Platform.runLater(() -> {
                    ipLabel.setText("Registered IP: " + ip);
                    timeLabel.setText(time);
                    statusLabel.setText("Status: Connected to server");
                    statusLabel.setTextFill(Color.GREEN);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    timeLabel.setText("Error: " + ex.getMessage());
                    statusLabel.setText("Status: Connection failed");
                    statusLabel.setTextFill(Color.RED);
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
