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
    private ArrayList<Place> placeArrayList = new ArrayList<>();
    private ArrayList<String> placeManagerView = new ArrayList<>();
    private HashMap<Integer,ArrayList<String>> timeWithViewPlaceManager = new HashMap<>();
    private HashMap<String,Integer> voteHash = new HashMap<>();
    private InetAddress addr;
    private static int port = 8888;
    private MulticastSocket s;
    private String urlPlace;
    private String leader = " ";
    private String majorLeader = "";
    private boolean exit = true;
    private int time = 0;
    private int timeVote = 0;
    private boolean consenso = false;

    PlacesManager(int port2) throws IOException {
        urlPlace = "rmi://localhost:" + port2 + "/placelist";
        addr = InetAddress.getByName("224.0.0.3");
        s = new MulticastSocket(port);
        s.joinGroup(addr);
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
                msgPeriodic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        t2.start();
        Thread t3= (new Thread(() -> {
                if(urlPlace.equals("rmi://localhost:" + 2029 + "/placelist")) {
                    try {
                        Thread.sleep(30*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Aquiiiii");
                    exit=false;
                }
        }));
        t3.start();

    }

    private void chooseLeader()  {
        String biggestHash = "";
        int length = 0;
        int i ;
        for (String a : placeManagerView) {
            if (a.hashCode() < 0) i = -1*a.hashCode();
            else i = a.hashCode();
            if (i > length) {
                length = a.hashCode();
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
                    System.out.println("o lider por unanimidade e " + me.getKey() + " para " + urlPlace);
                    //sendingSocket("lider");
            }
        } else consenso = false;
    }

    private void modifyHash(String vote)
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
                    chooseLeader();
                    //System.out.println("O voto para lider e " + leader + " pelo " + urlPlace + " " + time);
                    sendingSocket("voto");
                    break;
                }
            }
        }
    }



    private void msgPeriodic() throws IOException {
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
                msgPlusUrl = msg + "," + urlPlace + ",";
                break;
            case "voto":
                msgPlusUrl = msg + "," + urlPlace + "," + leader + ",";
                break;
        }
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
            if(hash[0].equals("voto"))
            {
                modifyHash(hash[2]);
                timeVote = -1;
            }else
                if (!placeManagerView.contains(hash[1])) placeManagerView.add(hash[1]);

            //System.out.println("Mensagem recebida: " + msg);
            //System.out.println("Pelo PlaceManager: " + urlPlace);
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