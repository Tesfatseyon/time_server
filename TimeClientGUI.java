package client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.*;
import javafx.animation.*;
import javafx.util.Duration;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Map;
import server.TimeServerInterface;

public class TimeClientGUI extends Application {
    private Label timeLabel;
    private Label ipLabel;
    private Label statusBar;
    private TextArea logArea;
    private ComboBox<String> zoneSelector;
    private TimeServerInterface server;

    private Timeline autoRefreshTimer;

    private final Map<String, String> serverCodeMap = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {
        // Map code → IP
        serverCodeMap.put("1234", "10.221.30.55");

        // Header
        Label title = new Label("Time Client");
        title.getStyleClass().add("title");

        ipLabel = new Label("Registered IP: Not registered yet");
        ipLabel.getStyleClass().add("ip-label");

        HBox header = new HBox(title, new Pane(), ipLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setPadding(new Insets(12, 16, 8, 16));

        // Center clock (compact font)
        timeLabel = new Label("Press button to get time");
        timeLabel.setId("clockLabel");
        timeLabel.setAlignment(Pos.CENTER);
        timeLabel.setStyle("-fx-font-size: 28px; -fx-font-family: 'Consolas'; -fx-text-fill: #2E86C1;");

        // Controls
        zoneSelector = new ComboBox<>();
        zoneSelector.getItems().addAll(
            "Etc/UTC", "Africa/Addis_Ababa", "America/New_York", "America/Los_Angeles",
            "Europe/London", "Europe/Paris", "Asia/Tokyo", "Asia/Dubai", "Australia/Sydney"
        );
        zoneSelector.setPrefWidth(220);

        Button connectButton = new Button("Connect to Server");
        connectButton.getStyleClass().add("btn-primary");
        connectButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("1234");
            dialog.setHeaderText("Enter Server Code");
            dialog.setContentText("Code:");
            dialog.showAndWait().ifPresent(code -> {
                String ip = serverCodeMap.get(code);
                if (ip != null) {
                    connectToServer(ip, 1099);
                } else {
                    statusBar.setText("Status: Invalid code");
                    statusBar.getStyleClass().setAll("status-bar", "status-error");
                    logArea.appendText("Invalid server code entered: " + code + "\n");
                }
            });
        });

        Button registerButton = new Button("Register & Get Time");
        registerButton.getStyleClass().add("btn-primary");
        registerButton.setOnAction(e -> fetchTime(true));

        Button clearButton = new Button("Clear Log");
        clearButton.getStyleClass().add("btn-secondary");
        clearButton.setOnAction(e -> logArea.clear());

        Button autoRefreshButton = new Button("Toggle Auto-Refresh");
        autoRefreshButton.getStyleClass().add("btn-secondary");
        autoRefreshButton.setOnAction(e -> toggleAutoRefresh());

        HBox buttons = new HBox(10, connectButton, registerButton, clearButton, autoRefreshButton);
        buttons.setAlignment(Pos.CENTER);

     // Log area (compact)
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(120);
        logArea.setPrefWidth(400);
        logArea.getStyleClass().add("log");

        // Status bar (compact font)
        statusBar = new Label("Status: Disconnected");
        statusBar.getStyleClass().addAll("status-bar", "status-error");
        statusBar.setMaxWidth(Double.MAX_VALUE);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-font-size: 12px; -fx-font-family: 'Arial'; -fx-font-weight: bold;");

        // Controls + log stacked
        VBox bottomPanel = new VBox(10,
            new Label("Select Time Zone:"), zoneSelector,
            buttons,
            logArea,
            statusBar
        );
        bottomPanel.setPadding(new Insets(10));

        // Center clock in its own VBox
        VBox clockBox = new VBox(timeLabel);
        clockBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(timeLabel, Priority.ALWAYS);

        // Root layout
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(clockBox);
        root.setBottom(bottomPanel);

        // Scene compact
        Scene scene = new Scene(root, 640, 400);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        primaryStage.setTitle("Time Client (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(360);
        primaryStage.show();

        // Auto-refresh clock every second
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> fetchTime(false)));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void connectToServer(String ip, int port) {
        try {
            Registry registry = LocateRegistry.getRegistry(ip, port);
            server = (TimeServerInterface) registry.lookup("TimeServer");
            ipLabel.setText("Registered IP: " + ip);
            statusBar.setText("Status: Connected to " + ip + ":" + port);
            statusBar.getStyleClass().setAll("status-bar", "status-ok");
            logArea.appendText("Connected successfully to " + ip + ":" + port + "\n");
        } catch (Exception ex) {
            server = null;
            statusBar.setText("Status: Connection failed");
            statusBar.getStyleClass().setAll("status-bar", "status-error");
            logArea.appendText("Connect error: " + ex.getMessage() + "\n");
        }
    }

    private void fetchTime(boolean logThisCall) {
        try {
            if (server == null) {
                timeLabel.setText("Error: Not connected to any server");
                statusBar.setText("Status: Disconnected");
                statusBar.getStyleClass().setAll("status-bar", "status-error");
                return;
            }

            String clientName = "FXClient";
            String ip = server.registerClient(clientName);
            ipLabel.setText("Registered IP: " + ip);

            String selectedZone = zoneSelector.getValue();
            if (selectedZone == null) selectedZone = "Etc/UTC";
            String response = server.getTimeForZone(selectedZone);

            String source = "UNKNOWN";
            String clock = response;
            String zone = selectedZone;

            String[] parts = response.split("\\|");
            if (parts.length == 3) {
                source = parts[0];
                clock = parts[1];
                zone = parts[2];
            }

            timeLabel.setText(clock);

            if (logThisCall) {
                logArea.appendText("[" + clientName + " - " + zone + "] " + source + " time: " + clock + "\n");
            }

            if ("INTERNET".equals(source)) {
                statusBar.setText("Status: INTERNET time");
                statusBar.getStyleClass().setAll("status-bar", "status-ok");
            } else if ("LOCAL".equals(source)) {
                statusBar.setText("Status: LOCAL fallback time");
                statusBar.getStyleClass().setAll("status-bar", "status-error");
            } else {
                statusBar.setText("Status: Unknown format");
                statusBar.getStyleClass().setAll("status-bar", "status-idle");
            }
        } catch (Exception ex) {
            timeLabel.setText("Error: " + ex.getMessage());
            statusBar.setText("Status: Connection failed");
            statusBar.getStyleClass().setAll("status-bar", "status-error");
        }
    }

    private void toggleAutoRefresh() {
        if (autoRefreshTimer == null) {
            autoRefreshTimer = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> fetchTime(false))
            );
            autoRefreshTimer.setCycleCount(Animation.INDEFINITE);
            autoRefreshTimer.play();
            statusBar.setText("Status: Auto-refresh ON");
            statusBar.getStyleClass().setAll("status-bar", "status-idle");
        } else {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
            statusBar.setText("Status: Auto-refresh OFF");
            statusBar.getStyleClass().setAll("status-bar", "status-idle");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
