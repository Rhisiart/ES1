import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlacesManager extends UnicastRemoteObject implements PlacesListInterface {
    private static final long serialVersionUID = 1L;
    private ArrayList<String> frontEndFollowers;
    private String frontEndLeader;
    private ArrayList<Place> placeArrayList = new ArrayList<>();
    private ArrayList<String> placeManagerView = new ArrayList<>();
    private HashMap<Integer,ArrayList<String>> timeWithViewPlaceManager = new HashMap<>();
    private HashMap<String,Integer> voteHash = new HashMap<>();
    private InetAddress addr;
    private static int port = 8888;
    private String urlPlace;
    private String leader = " ";
    private String majorLeader = "";
    private boolean exit = true;
    private int time = 0;
    private int timeVote = 0;
    private boolean consenso = true;

   public PlacesManager() throws RemoteException {
        super();
        this.frontEndFollowers = new ArrayList<>();
        this.frontEndLeader = "";
        Thread t1 = (new Thread(() -> {
           try {
               Thread.sleep(10*1000);
               receivingSocket();
           } catch (IOException | InterruptedException e) {
               e.printStackTrace();
           }
        }));
        t1.start();
    }

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
                msgPeriodic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        t2.start();
        Thread t3= (new Thread(() -> {
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
                msgPlusUrl = msg + "," + urlPlace + "," + majorLeader + ",";
                break;
            case "voto":
                msgPlusUrl = msg + "," + urlPlace + "," + leader + ",";
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
            if(hash[0].equals("voto"))
            {
                modifyHash(hash[2]);
                timeVote = -1;
            }else{
                //if(!hash[2].equals("")) frontEndLeader = hash[2];
                //if (!hash[1].equals(frontEndLeader)) frontEndFollowers.add(hash[1]);
                if (!placeManagerView.contains(hash[1])) placeManagerView.add(hash[1]);
            }
            //System.out.println("FrontLeader " + frontEndLeader);
            //for (String a : frontEndFollowers) System.out.println("Followers " + a);

            //System.out.println("Mensagem recebida: " + msg);
            //System.out.println("Pelo PlaceManager: " + urlPlace);
        }
        s.leaveGroup(addr);
        s.close();
    }



    @Override
    public void addPlace(Place p) throws IOException {
       placeArrayList.add(p);
       if(frontEndLeader.equals(urlPlace)){
           sendingSocket("addPlace," + p.getPostalCode() + "," + p.getLocality());
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