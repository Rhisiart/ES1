import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class RMIServer {
    public static void main(String [ ] args) {
        Registry r = null;

        try {
            r = LocateRegistry.createRegistry(Integer.parseInt(args[0]));

        } catch (
                RemoteException a) {
            a.printStackTrace();
        }

            try {
                r = LocateRegistry.getRegistry(Integer.parseInt(args[0]));

            } catch (RemoteException a) {
                a.printStackTrace();
            }
        if(Integer.parseInt(args[0]) == 2031) {
            try {
                FrontEnd frontEnd = new FrontEnd(Integer.parseInt(args[0]));
                assert r != null;
                r.rebind("frontend", frontEnd);
                System.out.println("FrontEnd ready");
            } catch (Exception e) {
                System.out.println("FrontEnd main " + e.getMessage());
            }
        }else {
            try {
                PlacesManager placeList = new PlacesManager(Integer.parseInt(args[0]));
                assert r != null;
                r.rebind("placelist", placeList);

                System.out.println("Place server ready");
            } catch (Exception e) {
                System.out.println("Place server main " + e.getMessage());
            }
        }
    }
}
