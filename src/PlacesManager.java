import java.io.IOException;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
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
       Thread t3= (new Thread(() -> {
                if(urlPlace.equals("rmi://localhost:" + 2030 + "/placelist")) {
                    try {
                        Thread.sleep(60*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Dead placeManager");
                    exit=false;
                }
        }));
        t3.start();
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

    private void majorityVote() throws RemoteException, NotBoundException, MalformedURLException {
        if (!(voteHash.size() > 1))
        {
            for (Map.Entry<String,Integer> me : voteHash.entrySet()) {
                    consenso = true;
                    majorLeader = me.getKey();
                    /*if (urlPlace.equals(majorLeader)){
                        for (String a : placeManagerView) {
                            PlacesListInterface pl = (PlacesListInterface) Naming.lookup(a);
                            ArrayList<Place> place = pl.allPlaces();
                            if(!placeArrayList.equals(place)){
                                System.out.println("Aquiii");
                            }
                        }
                    }*/
            }
        }
    }

    private void setVote(String vote)
    {
        if(timeVote == time) voteHash.clear();
        if(!voteHash.containsKey(vote)) voteHash.put(vote,1);
        else
            voteHash.replace(vote,voteHash.get(vote),voteHash.get(vote) + 1);
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
                    sendingSocket("voto");
                    break;
                }
            }
        }
    }

    private void heartBeats() throws IOException, NotBoundException {
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


    private void sendingSocket(String msg) throws IOException{
        String msgPlusUrl = "";
        switch (msg) {
            case "Alive":
                msgPlusUrl = msg + "," + urlPlace + "," + majorLeader + ",";
                break;
            case "voto":
                msgPlusUrl = msg + "," + urlPlace + "," + leader + ",";
                break;
            case "addPlace":
                msgPlusUrl = msg + "," +  urlPlace + ","  + orderLog + "," + registryLog.get(orderLog).getPostalCode() + "," + registryLog.get(orderLog).getLocality();
                break;
            case "getPlace":
                msgPlusUrl = msg + "," + urlPlace + "," + key + ",";
                break;
            case "addLostPlace":
                msgPlusUrl = msg + "," + urlPlace + "," + key + "," + urlPlaceManager + "," + registryLog.get(key).getPostalCode() + "," + registryLog.get(key).getLocality();
                break;
        }
        DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
        DatagramSocket dS = new DatagramSocket();
        dS.send(hi);
        System.out.println("Mensagem enviada: " + msgPlusUrl);
    }

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
            if (placeManagerView.contains(hash[1]) && !hash[0].equals("Alive")) {
                switch (hash[0]) {
                    case "voto":
                        setVote(hash[2]);
                        timeVote = -1;
                        break;
                    case "addPlace":
                        /**simulacao de uma mensagem que nao chegou**/
                       /* if (urlPlace.equals("rmi://localhost:2028/placelist") && hash[1].equals("1")) {

                        } else {*/
                            orderLog = Integer.parseInt(hash[2]);
                            key = Integer.parseInt(hash[2]) - 1;
                            registryLog.put(Integer.parseInt(hash[2]), new Place(hash[3], hash[4]));
                            if (!urlPlace.equals(majorLeader)) placeArrayList.add(new Place(hash[3], hash[4]));
                            if (!registryLog.containsKey(key) && key != 0) sendingSocket("getPlace");
                        //}
                        break;
                    case "getPlace":
                        if (urlPlace.equals(majorLeader)) {
                            key = Integer.parseInt(hash[2]);
                            urlPlaceManager = hash[1];
                            sendingSocket("addLostPlace");
                        }
                        break;
                    case "addLostPlace":
                        if (urlPlace.equals(hash[3])) {
                            registryLog.put(Integer.parseInt(hash[2]), new Place(hash[4], hash[5]));
                            placeArrayList.add(new Place(hash[4], hash[5]));
                        }
                        break;
                }
            }
            if(!placeManagerView.contains(hash[1])) placeManagerView.add(hash[1]);

            //System.out.println("Mensagem recebida: " + msg);
            //System.out.println("Pelo PlaceManager: " + urlPlace);
        }
        s.leaveGroup(addr);
        s.close();
    }



    @Override
    public void addPlace(Place p) throws IOException {
        if (!placeArrayList.contains(p)){
            System.out.println("O place com o codigo postal " + p.getPostalCode() + " foi adicionado");
            orderLog++;
            placeArrayList.add(p);
            registryLog.put(orderLog,p);
            /*if (urlPlace.equals("rmi://localhost:2029/placelist")){
            for (Map.Entry<Integer,Place> me : registryLog.entrySet()){
                System.out.println(me.getValue().getLocality());
                System.out.println(me.getKey());
                }
            }*/
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