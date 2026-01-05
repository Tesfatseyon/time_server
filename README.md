# time_server
Time Server & Client (Java RMI + JavaFX)
========================================

What this project is
--------------------
A small, polished desktop application that provides:
- An RMI-based Time Server (Java) that fetches authoritative time (worldtimeapi.org) and falls back to NTP or local time if needed.
- A native JavaFX "Time Client" GUI (desktop) which displays a large synced clock, lets the user select time zones, syncs with the server, and keeps an activity log.
- A native JavaFX "Admin Dashboard" GUI to view connected clients and their last requested timezone.

Key goals implemented
---------------------
- Modern, compact UI with a centered white card, soft shadows and a calm blue theme.
- The client clock shows a large, glowing digital clock, a compact timezone selector and a vertical action column for common actions.
- Server advertises a reachable IP (reads external config) so clients on other machines can connect.
- Settings persistence: the client saves `config.properties` externally so non-technical users can re-use the settings.
- Threaded network calls to keep the UI responsive.

Files of interest
-----------------
- Server
  - `src/server/TimeServerMain.java` — starts the server GUI and RMI registry.
  - `src/server/TimeServerImpl.java` — remote implementation for time operations and client registration.
  - `src/server/TimeServerInterface.java` — RMI interface used by clients.
  - `src/server/TimeAdminGUI.java` — admin dashboard GUI.
  - `src/server/ClientInfo.java` — simple POJO used by `TimeAdminGUI`.

- Client
  - `src/client/TimeClientGUI.java` — polished native JavaFX client (main app the user interacts with).
  - `src/client/TimeClient.java` — a minimal sample client (non-GUI) to test register/getTime behavior.

- Resources
  - `resources/config.properties` — packaged default config (server.ip/server.port). You can create an external `config.properties` in the working directory to override these values.
  - `resources/style.css` — the application stylesheet (colors, shadows, spacing, and responsive tweaks).

- Utilities
  - `src/tools/SmokeTest.java` — headless smoke test that starts a server on a test port, registers a client and verifies time fetching and client registration.

How the server/client talk
-------------------------
- The server exposes RMI methods:
  - `String getTime()` — convenience method returning a simple string for default zone.
  - `String registerClient(String clientName)` — registers a client and returns the observed client IP.
  - `Map<String,String> getConnectedClients()` — returns a map of clientName -> "IP|LastZone".
  - `String getTimeForZone(String zone)` — returns a composite string `SOURCE|epochMillis|zone` where SOURCE is typically `INTERNET` or `LOCAL`.

- The client parses `SOURCE|epochMillis|zone` and displays a synced clock, logs the action, and changes the status bar color:
  - INTERNET => green (accurate, server used internet time)
  - LOCAL => orange (fallback to server local time)
  - errors => red

Run & build (Windows, cmd.exe)
------------------------------
Prerequisites
- Java 11 or later installed.
- JavaFX 11+ SDK (unless you use a JDK that already bundles JavaFX such as Liberica Full).
  If you use Java 11 with a separate JavaFX SDK, point `--module-path` to the JavaFX `lib` directory and add the modules `javafx.controls` and `javafx.fxml`.

Quick compile & run (developer-friendly)
1) Compile (example using JavaFX SDK in %PATH_TO_FX%):

```bat
cd /d "C:\path\to\myServer"
set PATH_TO_FX=C:\path\to\javafx-sdk-XX\lib
javac --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -d bin -sourcepath src src\server\*.java src\client\*.java src\tools\*.java
```

2) Run server GUI (on the machine that should host the time source):

```bat
set PATH_TO_FX=C:\path\to\javafx-sdk-XX\lib
java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -cp bin server.TimeServerMain
```

3) Run client GUI (on same or another machine):

```bat
set PATH_TO_FX=C:\path\to\javafx-sdk-XX\lib
java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -cp bin client.TimeClientGUI
```

4) Run admin GUI (to inspect connected clients):

```bat
set PATH_TO_FX=C:\path\to\javafx-sdk-XX\lib
java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.fxml -cp bin server.TimeAdminGUI
```

Notes
- You may also run the headless `tools.SmokeTest` to quickly verify server logic (no JavaFX required):

```bat
javac -d bin -sourcepath src src\server\TimeServerInterface.java src\server\TimeServerImpl.java src\server\ClientInfo.java src\tools\SmokeTest.java
java -cp bin tools.SmokeTest
```

Make the server reachable from other machines
-------------------------------------------
1) Decide the IP the server will publish. Edit or create `config.properties` in the working directory where you will run the server and set:

```
server.ip=192.168.1.42
server.port=1099
```

2) Start the server. `TimeServerMain` sets `java.rmi.server.hostname` to the configured `server.ip` so that remote clients receive stubs with that address.

3) Open firewall for the chosen port on the server machine (`netsh advfirewall firewall add rule name="TimeServer RMI" dir=in action=allow protocol=TCP localport=1099`).

4) If the server is behind NAT and you want external access, configure port forwarding on your router for the port you chose — but prefer VPN for secure access.

UI Features
-----------
- The client UI uses a compact card (≈460px wide) with centered content and a clear vertical flow.
- The digital clock is large (~64px) with a glow/pulse animation for emphasis.
- Buttons are vertically stacked and styled consistently: primary actions in bold blue gradients and secondary actions in soft gray.
- The admin dashboard uses the same visual theme and provides a compact table with clear headings, plus an auto-refresh option.

Troubleshooting & FAQ
---------------------
- Client shows "Disconnected": ensure `server.ip` and `server.port` are correct in the client settings (Settings dialog) and the server is running.
- "Connection refused": check firewall and that the server process is listening on the configured port.
- If worldtimeapi or NTP fail, the server returns local time as fallback — the client will display "Local fallback" in the status.
