import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private static ArrayList<Place> placeArrayList = new ArrayList<>();
    private static InetAddress addr;
    private static int port = 8888;
    private MulticastSocket s ;
    private String urlPlace ;
    private Thread t1;
    private byte[] buf = new byte[1000];

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        addr = InetAddress.getByName("224.0.0.3");
        s = new MulticastSocket(port);
        s.joinGroup(addr);
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        t1 = (new Thread(() -> {
            try {
                s.receive(recv);
                String msg = new String(buf);
                System.out.println("Mensagem recebida: "  + msg);
                System.out.println("PlaceManager: " + urlPlace);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        t1.start();

    }

    @Override
    public void sendingSocket(String mensage) throws IOException
    {
        DatagramPacket hi = new DatagramPacket(mensage.getBytes(), mensage.getBytes().length, addr, port);
        s.send(hi);
        System.out.println("Mensagem enviada = " + mensage + " pelo PlaceManager " + urlPlace);
    }

    @Override
    public void receivingSocket() throws IOException
    {
        byte[] buf = new byte[1000];
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        s.receive(recv);
        String msg = new String(buf);
        System.out.println("Mensagem recebida: "  + msg);
        System.out.println("PlaceManager: " + urlPlace);
        //s.leaveGroup(addr);
    }



    @Override
    public void addPlace(Place p)  {
        placeArrayList.add(p);
    }

    @Override
    public ArrayList<Place> allPlaces()  {
        return placeArrayList;
    }

    @Override
    public Place getPlace(String codigoPostal)  {
        for (Place p : placeArrayList) {
            if (p.getPostalCode().equals(codigoPostal)) {
                return p;
            }
        }
        return null;
    }
}
