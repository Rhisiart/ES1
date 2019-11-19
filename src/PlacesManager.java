import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.SplittableRandom;

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
        sendingSocket("ola");
        receivingMsg();
    }

    private String chooseLeader() throws RemoteException {
        String biggerHash = "";
        int length = 0;
        for (String a : placeManagerList) {
            if (a.length() > length) {
                biggerHash = a;
            }
        }
        return biggerHash;
    }

    private void sendingSocket(String mensage) throws IOException {
        String msgPlusUrl = mensage + "," + urlPlace;
        Thread t1 = (new Thread(() -> {
            while (true) {
                DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
                try {
                    s.send(hi);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Mensagem enviado: " + msgPlusUrl);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
        t1.start();
    }

    private void receivingMsg()
    {
        Thread t1 = (new Thread(() -> {
                try {
                    receivingSocket();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }));
        t1.start();
    }

    private void receivingSocket() throws IOException
    {
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        while (true)
        {
            s.receive(recv);
            String msg = new String(buf);
            String[] hash = msg.split(",");
            String _hash = hash[1];
            System.out.println("Mensagem recebida: " + msg);
            System.out.println("Pelo PlaceManager: " + urlPlace);
            if (!placeManagerList.contains(_hash)) {
                placeManagerList.add(_hash);
            }
            if(hash[0].equals("null"))
            {
                break;
            }
        }
        s.leaveGroup(addr);
        s.close();
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
