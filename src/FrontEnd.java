import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;

public class FrontEnd extends UnicastRemoteObject implements PlacesListInterface {
    private static final long serialVersionUID = 1L;
    private boolean exit = true;
    private int timeOut = 0;
    private String leader = "";
    private ArrayList<String> followersArray = new ArrayList<>();

    FrontEnd(int port2) throws IOException {
        String urlPlace = "rmi://localhost:" + port2 + "/frontend";
        Thread t1 = (new Thread(() -> {
            try {
                receivingSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        t1.start();
    }
    /**melhorar a forma de verificar/atualizar a lista**/
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
            if(timeOut%3 == 0) followersArray.clear();
            switch (hash[0]){
                case "Alive":
                    timeOut++;
                    if (!followersArray.contains(hash[1])) followersArray.add(hash[1]);
                    if (!hash[1].equals("") || !leader.equals(hash[1])) leader = hash[1];
                    break;
                case "voto":
                    break;
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
