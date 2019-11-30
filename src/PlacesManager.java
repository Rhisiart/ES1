import java.io.IOException;
import java.net.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private ArrayList<Place> placeArrayList = new ArrayList<>();
    private ArrayList<String> placeManagerList = new ArrayList<>();
    private HashMap<Integer, ArrayList<String>> placeHashTimer = new HashMap<>();
    private InetAddress addr;
    private static int port = 8888;
    private MulticastSocket s;
    private String urlPlace;
    private byte[] buf = new byte[100];
    private int ts = 0;

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        addr = InetAddress.getByName("224.0.0.3");
        s = new MulticastSocket(port);
        s.joinGroup(addr);
        placeHashTimer.put(ts,new ArrayList<>());
        sendingSocket("ola");
        receivingSocket();
    }

    private String chooseLeader()  {
        String biggestHash = "";
        int length = 0;
        ArrayList<String> place = placeHashTimer.get(ts);
        for (String a : place)
        {
            if((-1*a.hashCode() > length))
                biggestHash = a;
        }
        return biggestHash;
    }

    private void compareHashMap()
    {
       if(placeHashTimer.containsKey(ts) && placeHashTimer.containsKey(ts-5000)){
           ArrayList<String> placeUrlList = placeHashTimer.get(ts);
           ArrayList<String> placeUrlListCopy = placeHashTimer.get(ts-5000);
           for(String a : placeUrlList)
           {
               if(!placeUrlListCopy.contains(a) || placeUrlList.size() < placeUrlListCopy.size()) System.out.println("o lider e : " + chooseLeader());
           }
        }
    }

    private void sendingSocket(String mensage)  {
        String msgPlusUrl = mensage + "," + urlPlace;
        Thread t1 = (new Thread(() -> {
            while (true) {
                ts += 5000;
                DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
                try {
                    s.send(hi);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Mensagem enviado: " + msgPlusUrl);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
        t1.start();
    }


    private void receivingSocket()
    {
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        Thread t1 = (new Thread(() -> {
            while (true) {
                try {
                    s.receive(recv);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String msg = new String(buf);
                String[] hash = msg.split(",");
                String _hash = hash[1];
                System.out.println("Mensagem recebida: " + msg);
                System.out.println("Pelo PlaceManager: " + urlPlace);
                if(!placeManagerList.contains(_hash))
                {
                    placeManagerList.add(_hash);
                }
                placeHashTimer.put(ts,placeManagerList);
                compareHashMap();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
               /* if(hash[0].equals("null"))
                {
                    break;
                }
            }
            s.leaveGroup(addr);
            s.close();*/
            }
        }));
        t1.start();
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
