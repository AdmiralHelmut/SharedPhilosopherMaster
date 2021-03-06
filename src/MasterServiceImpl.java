import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by Admiral Helmut on 01.05.2015.
 */
public class MasterServiceImpl extends UnicastRemoteObject implements MasterRemote {


    private ArrayList<Client> clientList;
    private HashMap<String, ClientRemote> clientRemoteMap;
    private HashMap<String, String> ipList;

    protected MasterServiceImpl() throws RemoteException {
        clientList = new ArrayList<Client>();
        clientRemoteMap = new HashMap<String, ClientRemote>();
        ipList = new HashMap<String, String>();
    }

    @Override
    public boolean register(String ip, String lookup) throws RemoteException {

        System.out.println("# Neuer Client unter IP "+ip+" versucht sich zu registrieren!");

        Client newClient = new Client(ip, lookup);
        for(Client client : clientList){
            if(client.equals(newClient)){
                return false;
            }
        }
        clientList.add(newClient);
        ClientRemote newClientRemote = null;
        try {
            System.out.println("Suche CLient");
            //System.setProperty("java.rmi.server.hostname", "192.168.1.3");
            System.out.println(newClient.getIp());
            newClientRemote = (ClientRemote) Naming.lookup("rmi://"+newClient.getIp()+"/"+newClient.getLookupName());

            System.out.println("Client gefunden");
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        clientRemoteMap.put(newClient.getLookupName(), newClientRemote);
        ipList.put(newClient.getLookupName(), newClient.getIp());

        boolean connectionToNewClient = checkClient(clientRemoteMap.get(newClient.getLookupName()), newClient.getIp());
        if(!connectionToNewClient){
            clientList.remove(newClient);
            clientRemoteMap.remove(newClient.getLookupName());
        }
        System.out.println("");
        System.out.println("#######################");
        System.out.println("Registrierte Clients:");
        System.out.println("");
        int mapCounter = 1;
        for(Map.Entry<String, ClientRemote> e : clientRemoteMap.entrySet()) {
            System.out.print("-- "+mapCounter+" "+e.getKey());
            mapCounter++;
        }

        System.out.println("Registriere neuen Client bei allen anderen Clients als Nachbar");
        // Registriere neuen Client bei allen anderen Clients als Nachbar
        for(Map.Entry<String, ClientRemote> e : clientRemoteMap.entrySet()) {
            if(!e.getKey().equals(lookup)){
                e.getValue().setNeighbour(ip,lookup);
            }
        }

        // Registriere alle alten Clients beim neuen Client als Nachbarn
        System.out.println("Registriere alle alten Clients beim neuen Client als Nachbarn");
        for(Map.Entry<String, ClientRemote> e : clientRemoteMap.entrySet()) {

            if(!e.getKey().equals(lookup)){
                clientRemoteMap.get(lookup).setNeighbour(ipList.get(e.getKey()),e.getKey());
            }
        }
        System.out.println("Registriere alle alten Clients beim neuen Client als Nachbarn FERTIG");
        synchronized (Main.getMonitor()){
            Main.getMonitor().notify();
        }

        return connectionToNewClient;
    }

    public int getClientListSize(){
        return clientList.size();
    }

    public  HashMap<String, ClientRemote> getRemoteMap(){
        return clientRemoteMap;
    }
    public  ArrayList<Client> getClientList(){
        return clientList;
    }
    private boolean checkClient(ClientRemote clientRemote, String ip){

        System.out.println("# Prüfe Client unter IP "+ip+"!");


        boolean clientOK;
        try {
            clientOK = clientRemote.checkClient();
        } catch (RemoteException e) {
            e.printStackTrace();
            clientOK = false;
        }

        if(clientOK){
            System.out.println("# Client unter IP "+ip+" erfolgreich geprüft!");
        }else{
            System.out.println("# FEHLER auf Client unter IP "+ip+"!");
        }

        return clientOK;
    }

}
