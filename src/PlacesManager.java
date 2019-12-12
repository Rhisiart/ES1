import org.omg.Messaging.SYNC_WITH_TRANSPORT;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private ArrayList<Place> placeArrayList = new ArrayList<>();
    private ArrayList<String> placeManagerList = new ArrayList<>();
    private HashMap<String, Integer> voteHash = new HashMap<>();
    private HashMap<Integer, ArrayList<String>> placeHashTimer = new HashMap<>();
    private InetAddress addr;
    private static int port = 8888;
    private MulticastSocket s;
    private String urlPlace;
    private static int ts = 0;
    private int tsVote = 5000;
    private String leader = "";
    private boolean exit = true;

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        addr = InetAddress.getByName("224.0.0.3");
        s = new MulticastSocket(port);
        s.joinGroup(addr);
        placeHashTimer.put(ts,new ArrayList<>());
        Thread t1 = (new Thread(() -> {
            try {
                receivingSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        t1.start();
        sendingSocket("ola");
    }

    private void chooseLeader()  {
        String biggestHash = "";
        int length = 0;
        int i ;
        for (String a : placeManagerList) {
            if (a.hashCode() < 0) i = -1*a.hashCode();
            else i = a.hashCode();
            if (i > length) {
                length = a.hashCode();
                biggestHash = a;
            }
        }
        leader = biggestHash;
    }
    //funcao para haver consenso na escolha do lider, atraves da maioria, se um place escolher o lider que nao foi da maioria tera que fazer o processo novamente
    private void majorityVote() throws IOException {
        //o voto tem que ser superior 1
        if (!(voteHash.size() > 1))
        {
            for (Map.Entry<String,Integer> me : voteHash.entrySet()) {
                if (!me.getValue().equals(1)) {
                    System.out.println("o lider por unamidade e " + me.getKey());
                    sendingSocket("lider");
                } else sendingSocket("ola");
            }
        } else sendingSocket("ola");
        tsVote = ts;
    }

    private void modifyHash(String leader)
    {
        if(tsVote != ts) voteHash.clear();
        if(!voteHash.containsKey(leader)) voteHash.put(leader,1);
        else
            voteHash.replace(leader,voteHash.get(leader),voteHash.get(leader) + 1);
    }

    private void compareHashMap() throws IOException {
        if (placeHashTimer.containsKey(ts) && placeHashTimer.containsKey(ts-5000))
        {
            for (String a : placeHashTimer.get(ts))
            {
                if (!(placeHashTimer.get(ts-5000).contains(a)) || placeHashTimer.get(ts).size() < placeHashTimer.get(ts-5000).size())
                {
                    chooseLeader();
                    System.out.println("O lider e " + leader);
                    sendingSocket("voto");
                    break;
                }
            }
        }
    }

    private void sendingSocket(String msg) throws IOException {
        String msgPlusUrl = "";
        switch (msg){
            case "ola":
                msgPlusUrl = msg + "," + urlPlace + ",";
                break;
            case "voto":
                msgPlusUrl = msg + "," + urlPlace + "," + leader;
                break;
        }
        ts+=5000;
        DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
        DatagramSocket dS = new DatagramSocket();
        dS.send(hi);
        System.out.println("Mensagem enviado: " + msgPlusUrl);
    }

    private void receivingSocket() throws IOException{
            while (exit) {
                byte[] buf = new byte[1024];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                s.receive(recv);
                String msg = new String(recv.getData());
                String[] hash = msg.split(",");
                /*switch (hash[0]){
                    case "voto":
                        modifyHash(hash[1]);
                        majorityVote();
                        if(!placeManagerList.contains(hash[2])) placeManagerList.add(hash[2]);
                        break;
                }
                if(hash[0].equals("voto"))
                {
                    //modifyHash(hash[2]);
                    //majorityVote();
                }*/
                if(!placeManagerList.contains(hash[1])) placeManagerList.add(hash[1]);
                ArrayList<String> clone = new ArrayList<>(placeManagerList);
                placeHashTimer.put(ts,clone);
                System.out.println("Mensagem recebida: " + msg);
                System.out.println("Pelo PlaceManager: " + urlPlace);
                compareHashMap();
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