package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Implementation of the TimeServerInterface.
 * Provides time services to RMI clients.
 */
public class TimeServerImpl extends UnicastRemoteObject implements TimeServerInterface {

    private final Map<String, String> connectedClients;

    public TimeServerImpl() throws RemoteException {
        super();
        connectedClients = new HashMap<>();
    }

    @Override
    public String getTime() throws RemoteException {
        // Default to Addis Ababa timezone
        return getTimeForZone("Africa/Addis_Ababa");
    }

    @Override
    public String registerClient(String clientName) throws RemoteException {
        String ip = getServerIP();
        connectedClients.put(clientName, ip + "|Africa/Addis_Ababa");
        System.out.println("Client registered: " + clientName + " with IP " + ip);
        return ip;
    }

    @Override
    public Map<String, String> getConnectedClients() throws RemoteException {
        return connectedClients;
    }

    @Override
    public String getTimeForZone(String zone) throws RemoteException {
        try {
            long epochMillis = fetchInternetEpochMillis(zone);
            String formatted = formatTime(epochMillis, zone);
            return "INTERNET|" + formatted + "|" + zone;
        } catch (Exception e) {
            // Fallback to local system time if API is unreachable
            long epochMillis = System.currentTimeMillis();
            String formatted = formatTime(epochMillis, zone);
            return "LOCAL|" + formatted + "|" + zone;
        }
    }

    /**
     * Fetches epoch milliseconds from WorldTimeAPI for a given zone.
     */
    private long fetchInternetEpochMillis(String zone) throws Exception {
        URL url = new URL("https://worldtimeapi.org/api/timezone/" + zone);
        javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();
            int start = json.indexOf("\"datetime\":\"");
            if (start == -1) throw new Exception("datetime field not found in API response");
            start += 12;
            int end = json.indexOf("\"", start);
            String datetime = json.substring(start, end);

            OffsetDateTime odt = OffsetDateTime.parse(datetime);
            return odt.toInstant().toEpochMilli();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Formats epoch millis into HH:mm:ss string for a given zone.
     */
    private String formatTime(long epochMillis, String zone) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        ZoneId zoneId = ZoneId.of(zone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return formatter.format(instant.atZone(zoneId));
    }

    /**
     * Dynamically detect the server's LAN IP address.
     */
    private String getServerIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(".")) { // IPv4 only
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1"; // fallback
    }
}
