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
        //sendingSocket("hi");
        receivingMsg();
        //chooseLeader();
        //System.out.println(leader);
        //sendingSocket(leader + " este e o lider");
    }

    /*PlacesManager(int port2) throws IOException{
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        addr = InetAddress.getByName("224.0.0.3");
        s = new MulticastSocket(port);
        s.joinGroup(addr);
        DatagramPacket recv = new DatagramPacket(buf, buf.length);
        Thread t1 = (new Thread(() -> {
            while (true){
                String mensage = "";
                try {
                    s.receive(recv);
                    String msg = new String(buf);
                    String _port = msg.substring(28, 32);
                    mensage = msg.substring(43, 47);
                    System.out.println("Mensagem recebida: " + msg);
                    System.out.println("PlaceManager: " + urlPlace);
                    if (!placeManagerList.contains(_port)) {
                        placeManagerList.add(_port);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
               if(mensage.equals("null"))
                {
                    break;
                }

        }
            int length = 0;
            for(String a: placeManagerList)
            {
               if(Integer.parseInt(a) > length )
               {
                   length = Integer.parseInt(a);
               }
            }
            try {
                sendingSocket(Integer.toString(length) + " este e o lider");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        t1.start();
       //System.out.println("AAAAAAAAA");
    }*/

    @Override
    public  int chooseLeader() throws RemoteException
    {
        int leader = 0;
        for(String a: placeManagerList)
        {
            if(Integer.parseInt(a) > leader )
            {
                leader = Integer.parseInt(a);
            }
        }
        placeManagerList.clear();
        return leader;
    }

    /*private void sendingMsg(String msg)
    {
        Thread t1 = (new Thread(() -> {
        try {
            sendingSocket(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(30*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }));
        t1.start();
    }*/

    @Override
    public void sendingSocket(String mensage) throws IOException
    {
        String msgPlusUrl = "Enviado por " + urlPlace + " " +  mensage;
        DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
        s.send(hi);
        System.out.println(msgPlusUrl);
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
                    Thread.sleep(30*1000);
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
            String _port = msg.substring(28, 32);
            String mensage = msg.substring(43, 47);
            System.out.println(msg);
            System.out.println("PlaceManager: " + urlPlace);
            if (!placeManagerList.contains(_port)) {
                placeManagerList.add(_port);
            }

            if(mensage.equals("null"))
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
