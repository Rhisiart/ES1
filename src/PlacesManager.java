import com.sun.org.apache.xerces.internal.impl.XMLEntityScanner;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private static ArrayList<Place> placeArrayList = new ArrayList<>();
    private static ArrayList<String> placeManagerList = new ArrayList<>();
    private static InetAddress addr;
    private static int port = 8888;
    private MulticastSocket s;
    private String urlPlace;
    private byte[] buf = new byte[100];

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        addr = InetAddress.getByName("224.0.0.3");
        s = new MulticastSocket(port);
        s.joinGroup(addr);
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        Thread t1 = (new Thread(() -> {
            while (true){
                try {
                    s.receive(recv);
                    String msg = new String(buf);
                    String _port = msg.substring(28, 32);
                    System.out.println("Mensagem recebida: "  + msg);
                    System.out.println("PlaceManager: " + urlPlace);
                    if(!placeManagerList.contains(_port))
                    {
                       placeManagerList.add(_port);
                    }
                    System.out.println(placeManagerList.size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        t1.start();
    }

    @Override
    public void sendingSocket(String mensage) throws IOException
    {
        String msgPlusUrl = "Enviado por " + urlPlace + " " +  mensage;
        DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
        s.send(hi);
        System.out.println("Mensagem enviada = " + msgPlusUrl);
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
