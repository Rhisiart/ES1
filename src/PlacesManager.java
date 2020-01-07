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

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private static final long serialVersionUID = 1L;
    private HashMap<Integer,Place> registryLog = new HashMap<>();
    private ArrayList<Place> placeArrayList = new ArrayList<>();
    private ArrayList<String> placeManagerView = new ArrayList<>();
    private HashMap<Integer,ArrayList<String>> timeWithViewPlaceManager = new HashMap<>();
    private HashMap<String,Integer> voteHash = new HashMap<>();
    private InetAddress addr;
    private static int port = 8888;
    private String urlPlace;
    private String leader = "";
    private String majorLeader = "";
    private String urlPlaceManager = "";
    private boolean exit = true;
    private int time = 0,timeVote = 0, orderLog = 0, key = 0;
    private boolean consenso = true;

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        Thread t1 = (new Thread(() -> {
            try {
                receivingSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        t1.start();
        Thread t2 = (new Thread(() -> {
            try {
                heartBeats();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        t2.start();
       /* Thread t3= (new Thread(() -> {
                if(urlPlace.equals("rmi://localhost:" + 2030 + "/placelist")) {
                    try {
                        Thread.sleep(30*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Dead placeManager");
                    exit=false;
                }
        }));
        t3.start();*/
    }

    private void chooseLeader()  {
        String biggestHash = "";
        int length = 0,i;
        for (String a : placeManagerView) {
            if (a.hashCode() < 0) i = -1*a.hashCode();
            else i = a.hashCode();
            if (i > length) {
                length = i;
                biggestHash = a;
            }
        }
        leader = biggestHash;
    }

    private void majorityVote() {
        if (!(voteHash.size() > 1))
        {
            for (Map.Entry<String,Integer> me : voteHash.entrySet()) {
                    consenso = true;
                    majorLeader = me.getKey();
                    //System.out.println("o lider por unanimidade e " + me.getKey() + " para " + urlPlace);
                    //sendingSocket("lider");
            }
        } else
            consenso = false;
    }

    private void setVote(String vote)
    {
        if(timeVote == time) voteHash.clear();
        if(!voteHash.containsKey(vote)) voteHash.put(vote,1);
        else
            voteHash.replace(vote,voteHash.get(vote),voteHash.get(vote) + 1);
        //for(Map.Entry<String,Integer> me : voteHash.entrySet()) System.out.println( "Em " + me.getKey() + " voto "+ me.getValue() + " " + urlPlace);
    }

    private void compareHashMap() throws IOException {
        if (timeWithViewPlaceManager.containsKey(time) && timeWithViewPlaceManager.containsKey(time-1))
        {
            for (String a : timeWithViewPlaceManager.get(time))
            {
                if (!(timeWithViewPlaceManager.get(time - 1).contains(a)) || timeWithViewPlaceManager.get(time).size() < timeWithViewPlaceManager.get(time - 1).size() || !consenso)
                {
                    majorLeader = "";
                    consenso = false;
                    chooseLeader();
                    //System.out.println("O voto para lider e " + leader + " pelo " + urlPlace + " " + time);
                    sendingSocket("voto");
                    break;
                }
            }
        }
    }

    private void heartBeats() throws IOException {
        while (exit) {
            Thread t1 = (new Thread(() -> {
                try {
                    sendingSocket("Alive");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
            t1.start();
            ArrayList<String> clone = new ArrayList<>(placeManagerView);
            timeWithViewPlaceManager.put(time, clone);
            compareHashMap();
            if(!consenso) majorityVote();
            /*for (Map.Entry<Integer,ArrayList<String>> me : timeWithViewPlaceManager.entrySet()) {
                if (me.getKey() == time) System.out.println("hash = " + me + " " + urlPlace);}*/
            time += 1;
            timeVote = time;
            placeManagerView.clear();
            try {
                Thread.sleep(5*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void sendingSocket(String msg) throws IOException {
        String msgPlusUrl = "";
        switch (msg) {
            case "Alive":
                msgPlusUrl = msg + "," + urlPlace + "," + majorLeader + ",";
                break;
            case "voto":
                msgPlusUrl = msg + "," + urlPlace + "," + leader + ",";
                break;
            case "addPlace":
                msgPlusUrl = msg + "," + orderLog + "," + registryLog.get(orderLog).getPostalCode() + "," + registryLog.get(orderLog).getLocality();
                break;
            case "getPlace":
                msgPlusUrl = msg + "," + key + "," + urlPlace + ",";
                break;
            case "addLostPlace":
                msgPlusUrl = msg + "," + key + "," + urlPlaceManager + "," + registryLog.get(key).getPostalCode() + "," + registryLog.get(key).getLocality();
                System.out.println(msgPlusUrl);
                break;
        }
        DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
        DatagramSocket dS = new DatagramSocket();
        dS.send(hi);
        System.out.println("Mensagem enviada: " + msgPlusUrl);
    }

    /**se uma mensagem atrasada for enviado por um placeManage que crashou, nao e considerada**/
    private void receivingSocket() throws IOException{
        addr = InetAddress.getByName("224.0.0.3");
        MulticastSocket s = new MulticastSocket(port);
        s.joinGroup(addr);
        while (exit) {
            byte[] buf = new byte[1024];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            String msg = new String(recv.getData());
            String[] hash = msg.split(",");
            switch (hash[0]) {
                case "voto":
                    setVote(hash[2]);
                    timeVote = -1;
                    break;
                case "addPlace":
                    /**simulacao de uma mensagem que nao chegou**/
                   /*if (urlPlace.equals("rmi://localhost:2028/placelist") && hash[1].equals("1")){

                    }
                    else {*/
                        key = Integer.parseInt(hash[1]) - 1;
                        registryLog.put(Integer.parseInt(hash[1]), new Place(hash[2], hash[3]));
                        if (!urlPlace.equals(majorLeader)) placeArrayList.add(new Place(hash[2], hash[3]));
                        if (!registryLog.containsKey(key) && key != 0) sendingSocket("getPlace");
                    //}
                    break;
                case "Alive":
                    if (!placeManagerView.contains(hash[1])) placeManagerView.add(hash[1]);
                    break;
                case "getPlace":
                    if (urlPlace.equals(majorLeader)) {
                        key = Integer.parseInt(hash[1]);
                        urlPlaceManager = hash[2];
                        sendingSocket("addLostPlace");
                    }
                    break;
                case "addLostPlace":
                    if (urlPlace.equals(hash[2])) {
                        registryLog.put(Integer.parseInt(hash[1]),new Place(hash[3],hash[4]));
                        placeArrayList.add(new Place(hash[3],hash[4]));
                        for (Place a : placeArrayList){System.out.println(a.getLocality());}
                    }
                    break;
            }
            //System.out.println("Mensagem recebida: " + msg);
            //System.out.println("Pelo PlaceManager: " + urlPlace);
        }
        s.leaveGroup(addr);
        s.close();
    }



    @Override
    public void addPlace(Place p) throws IOException {
        if (!placeArrayList.contains(p)){
            System.out.println("O place com o codigo postal " + p.getPostalCode());
            orderLog++;
            placeArrayList.add(p);
            registryLog.put(orderLog,p);
            sendingSocket("addPlace");
        }
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