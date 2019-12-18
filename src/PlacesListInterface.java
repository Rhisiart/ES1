import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface PlacesListInterface extends Remote {
    void addPlace(Place p) throws IOException, NotBoundException;
    ArrayList<Place> allPlaces() throws RemoteException;
    Place getPlace(String codigoPostal) throws RemoteException;
}
