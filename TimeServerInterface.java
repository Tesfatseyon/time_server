package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Remote interface for the Time Server.
 * Defines methods that clients can call via RMI.
 */
public interface TimeServerInterface extends Remote {

    String getTime() throws RemoteException;

    String registerClient(String clientName) throws RemoteException;

    Map<String, String> getConnectedClients() throws RemoteException;

    String getTimeForZone(String zone) throws RemoteException;
}
