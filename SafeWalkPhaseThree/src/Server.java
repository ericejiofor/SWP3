import edu.purdue.cs.cs180.channel.*;
import java.util.*;

public class Server implements MessageListener {
    private Channel channel;
    private String algorithm;
    private int interval;
    private final Queue<Volunteer> volunteers = new ArrayDeque<Volunteer>();
    private final Queue<Request> requests = new ArrayDeque<Request>();

    public Server(Channel channel, String algorithm) {
        this.channel = channel;
        this.algorithm = algorithm;
        channel.setMessageListener(this);
        System.out.printf("Matching Algorithm: %s\n", algorithm);
    }

    class Volunteer {
        final int id;
        final String location;

        Volunteer(int id, String location) {
            this.id = id;
            this.location = location;
        }
    }

    class Request {
        final int id;
        final String location;
        final String urgency;

        Request(int id, String location, String urgency) {
            this.id = id;
            this.location = location;
            this.urgency = urgency;
        }
    }

    @Override
    public void messageReceived(String message, int id) {
        System.out.printf("Received from %d: %s\n", id, message);

        String[] m = message.split(" ");
        if (m[0].equals("REQUEST")) // REQUEST location urgency
            requests.add(new Request(id, m[1], m[2]));
        else if (m[0].equals("VOLUNTEER")) // VOLUNTEER location
            volunteers.add(new Volunteer(id, m[1]));
        else
            System.err.printf("IGNORING INVALID REQUEST: %s\n", message);

        if (!(requests.isEmpty() || volunteers.isEmpty())) {
            if (algorithm.equals("FCFS"))
                matchFCFS();
            else if (algorithm.equals("CLOSEST"))
                matchClosest();
            else if (algorithm.equals("URGENCY"))
                matchUrgency();
        }
    }

    private void matchFCFS() {
        Volunteer volunteer = volunteers.remove();
        Request requester = requests.remove();
        sendMessages(volunteer, requester);
    }

    private int getDistance(String location1, String location2) {
        final String[] buildings = { "CL50", "EE", "LWSN", "PMU", "PUSH" };
        final int[][] distances = { { 0, 8, 6, 5, 4 }, { 8, 0, 4, 2, 5 }, { 6, 4, 0, 3, 1 }, { 5, 2, 3, 0, 7 },
                { 4, 5, 1, 7, 0 } };
        int i1 = findBuilding(buildings, location1);
        int i2 = findBuilding(buildings, location2);
        return distances[i1][i2];
    }

    private int findBuilding(String[] buildings, String location) {
        for (int i = 0; i < buildings.length; i++)
            if (buildings[i].equals(location))
                return i;
        System.err.println("UNKNOWN BUILDING: " + location);
        return -1;
    }

    private void matchClosest() {
        Volunteer volunteer = null;
        Request requester = null;
        if (requests.size() == 1) {
            requester = requests.remove();
            int distance = Integer.MAX_VALUE;
            for (Volunteer v : volunteers) {
                int d = getDistance(requester.location, v.location);
                if (d < distance) {
                    distance = d;
                    volunteer = v;
                }
            }
            volunteers.remove(volunteer);
        } else if (volunteers.size() == 1) {
            volunteer = volunteers.remove();
            int distance = Integer.MAX_VALUE;
            for (Request r : requests) {
                int d = getDistance(r.location, volunteer.location);
                if (d < distance) {
                    distance = d;
                    requester = r;
                }
            }
            requests.remove(requester);
        } else {
            System.err.println("bad");
        }

        if (volunteer != null && requester != null) {
            sendMessages(volunteer, requester);
        }
    }

    private void matchUrgency() {
        Volunteer volunteer = volunteers.remove();
        if (failedUrgency(volunteer, "EMERGENCY") && failedUrgency(volunteer, "URGENT")
            && failedUrgency(volunteer, "NORMAL"))
        System.err.println("FAILED TO HANDLE URGENCY REQUEST");
    }


    private boolean failedUrgency(Volunteer volunteer, String urgency) {
        for (Request requester : requests)
            if (requester.urgency.equals(urgency)) {
                requests.remove(requester);
                sendMessages(volunteer, requester);
                return false;
            }
        return true;
    }

    private void sendMessages(Volunteer volunteer, Request requester) {
        try {
            String rMessage = "VOLUNTEER " + volunteer.id + " " + getDistance(volunteer.location, requester.location);
            String vMessage = "LOCATION " + requester.location + " " + requester.urgency;
            System.out.printf("Sending:\n");
            System.out.printf("    to volunteer %d: %s\n", volunteer.id, vMessage);
            System.out.printf("    to requester %d: %s\n", requester.id, rMessage);
            channel.sendMessage(vMessage, volunteer.id);
            channel.sendMessage(rMessage, requester.id);
        } catch (ChannelException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args, int inter) {
        Channel channel = new TCPChannel(Integer.parseInt(args[0]));
        new Server(channel, args[1]);
    }
    
    public void run()
    {
    	 try {
             Thread.sleep(interval);
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
    }
}
