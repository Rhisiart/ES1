import org.omg.Messaging.SYNC_WITH_TRANSPORT;

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
    private HashMap<String,String> voteHash = new HashMap<>();
    private InetAddress addr;
    private static int port = 8888;
    private String urlPlace;
    private String leader = "";
    private String majorLeader = "";
    private String urlPlaceManager = "";
    private boolean exit = true;
    private int time = 0, orderLog = 0, key = 0;
    private boolean consenso = true;

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        timeWithViewPlaceManager.put(time,new ArrayList<>(placeManagerView));
        Thread t1 = (new Thread(() -> {
            try {
                receivingSocket();
            } catch (IOException | NotBoundException e) {
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
            if(urlPlace.equals("rmi://localhost:2030/placelist")) {
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

    private void majorityVote() throws IOException{
        int numVote = 0;
        String vote = "";
        if(voteHash.size() == placeManagerView.size()) {
            for (Map.Entry<String, String> me : voteHash.entrySet()) {
                if (vote.isEmpty() || me.getValue().equals(vote)) {
                    vote = me.getValue();
                    numVote++;
                }
            }
        }
        if (numVote == placeManagerView.size()) {
            majorLeader = vote;
            consenso = true;
            orderLog = registryLog.size();
            voteHash.clear();
            if (urlPlace.equals(majorLeader)) sendingSocket("checkPlaces");
        }
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
                    Thread t1 = (new Thread(() -> {
                        try {
                            sendingSocket("voto");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }));
                    t1.start();
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
                try {
                    Thread.sleep(5*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
            t1.start();
            ArrayList<String> clone = new ArrayList<>(placeManagerView);
            time += 1;
            timeWithViewPlaceManager.put(time, clone);
            compareHashMap();
            if(!consenso) majorityVote();
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
            case "checkPlaces":
                msgPlusUrl = msg + "," + urlPlace + ",";
                break;
        }
        DatagramPacket hi = new DatagramPacket(msgPlusUrl.getBytes(), msgPlusUrl.getBytes().length, addr, port);
        DatagramSocket dS = new DatagramSocket();
        dS.send(hi);
        System.out.println("Mensagem enviada: " + msgPlusUrl);
    }

    /**simulacao de uma mensagem que nao chegou**/
    private void receivingSocket() throws IOException, NotBoundException {
        addr = InetAddress.getByName("224.0.0.3");
        MulticastSocket s = new MulticastSocket(port);
        s.joinGroup(addr);
        while (exit) {
            byte[] buf = new byte[1024];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            String msg = new String(recv.getData());
            String[] hash = msg.split(",");
            if (timeWithViewPlaceManager.get(time).contains(hash[1]) && !hash[0].equals("Alive")) {
                switch (hash[0]) {
                    case "voto":
                        if (!consenso) {
                            if (!voteHash.containsKey(hash[1])) voteHash.put(hash[1], hash[2]);
                            else voteHash.replace(hash[1], hash[2]);
                        }
                        break;
                    case "addPlace":
                       if (urlPlace.equals("rmi://localhost:2028/placelist") && hash[2].equals("1")) {

                        } else {
                           key = Integer.parseInt(hash[2]) - 1;
                           registryLog.put(Integer.parseInt(hash[2]), new Place(hash[3], hash[4]));
                           if (!urlPlace.equals(majorLeader)) placeArrayList.add(new Place(hash[3], hash[4]));
                           if (!registryLog.containsKey(key) && key != 0) sendingSocket("getPlace");
                       }
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
                    case "checkPlaces":
                        //System.out.println("Aqui " + urlPlace + " " + placeManagerView.isEmpty());
                        for (String a : timeWithViewPlaceManager.get(time))
                        {
                            if (!a.equals(urlPlace)) {
                                System.out.println("aquiiiiiiii" + " " + urlPlace);
                                PlacesListInterface p1 = (PlacesListInterface) Naming.lookup(a);
                                ArrayList<Place> place = p1.allPlaces();
                                place.removeAll(placeArrayList); // os place que o array placeArrayList nao tem
                                placeArrayList.addAll(place);
                            }
                        }
                        if (urlPlace.equals("rmi://localhost:2028/placelist")) System.out.println(placeArrayList.size());
                        break;
                }
            }
            if(!placeManagerView.contains(hash[1])) placeManagerView.add(hash[1]);

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