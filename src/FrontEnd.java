import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;

public class FrontEnd extends UnicastRemoteObject implements PlacesListInterface {
    private static final long serialVersionUID = 1L;
    private String leader = "";
    private ArrayList<String> followersArray = new ArrayList<>();
    private boolean exit = true;
    private long timeStamp;

    FrontEnd() throws IOException {
        timeStamp = new Timestamp(System.currentTimeMillis()).getTime();
        Thread t1 = (new Thread(() -> {
            try {
                receivingSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        t1.start();
    }

    private void receivingSocket() throws IOException{
        InetAddress addr = InetAddress.getByName("224.0.0.3");
        MulticastSocket s = new MulticastSocket(8888);
        s.joinGroup(addr);
        while (exit) {
            byte[] buf = new byte[1024];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            s.receive(recv);
            String msg = new String(recv.getData());
            String[] hash = msg.split(",");
            if(new Timestamp(System.currentTimeMillis()).getTime() - timeStamp > 15000){
                followersArray.clear();
                timeStamp = new Timestamp(System.currentTimeMillis()).getTime();
            }
            if(hash[0].equals("Alive")) {
                if (!followersArray.contains(hash[1])) followersArray.add(hash[1]);
                if (!hash[2].equals("") && !leader.equals(hash[2])) leader = hash[2];
            }
        }
        s.leaveGroup(addr);
        s.close();
    }

    @Override
    public void addPlace(Place p) throws IOException, NotBoundException {
        PlacesListInterface pmLeader = (PlacesListInterface) Naming.lookup(leader);
        pmLeader.addPlace(p);
    }

    @Override
    public ArrayList<Place> allPlaces()  {
        //return placeArrayList;
        return null;
    }

    @Override
    public Place getPlace(String codigoPostal) throws RemoteException, NotBoundException, MalformedURLException {
        Random generator = new Random();
        PlacesListInterface pmFollower = (PlacesListInterface) Naming.lookup(followersArray.get(generator.nextInt(followersArray.size())));
        return pmFollower.getPlace(codigoPostal);
    }
}
