/**
 * Project 8
 * @author eejiofor
 * @author rober230
 */

import edu.purdue.cs.cs180.channel.*;
import java.util.*;

public class Server implements MessageListener {
	private Channel channel;
	private int sleepTime;
	private final Queue<Volunteer> volunteers = new ArrayDeque<Volunteer>();
	private final Queue<Request> requests = new ArrayDeque<Request>();
	Object lock = new Object();
	private final int[][] times = {{0, 8, 6, 5, 4}, {8, 0, 4, 2, 5}, {6, 4, 0, 3, 1}, 
							{5, 2, 3, 0, 7}, {4, 5, 1, 7, 0}};
							
	public Server(Channel channel, int sleepTime) {
		this.channel = channel;
		this.sleepTime = sleepTime;
		channel.setMessageListener(this);
	}

	class Volunteer {
		final int id;
		final String location;

		Volunteer(int id, String location) {
			this.id = id;
			this.location = location;
		}
	}

	public int getNumberVolunteers() {
		return volunteers.size();
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

	public int getNumberRequesters() {
		return requests.size();	
	}
	
	public int getTime(String loc) {
		int coord = 0;
		if (loc.equals("CL50"))
			coord = 0;
		if (loc.equals("EE"))
			coord = 1;
		if (loc.equals("LWSN"))
			coord = 2;
		if (loc.equals("PMU"))
			coord = 3;
		if (loc.equals("PUSH"))
			coord = 4;
		return coord;		
	}
	
	public void closest() {
	
		if (volunteers.size() > 1 && requests.size() == 1) {
			
			sendMessage(volunteer, request);
		}
		else if (requests.size() > 1 && volunteers.size() == 1) {
			Volunteer volunteer = volunteers.remove(0);
			int temp = 10;
			int index = 0;
			for (int i = 0; i < requests.size(); i++)
			{
				Request request = requests.get(i);
				int curTime = times[getTime(volunteer.location)][getTime(request.location)];
				if (curTime < temp)
				{
					temp = curTime;
					index = i;
				}			
			}
			Request request = requests.remove(index);
			sendMessage(volunteer, request);
		}
		else if (requests.size() == 1 && volunteers.size() == 1) {
			Request request = requests.remove(0);
			Volunteer volunteer = volunteers.remove(0);
			sendMessage(volunteer, request);
		}
	
	} 
	
	
	@Override
	public void messageReceived(String message, int id) {
		sychronized (lock) {
			String[] m = message.split(" ");
			if (m[0].equals("REQUEST")) // REQUEST location urgency
				requests.add(new Request(id, m[1], m[2]));
			else if (m[0].equals("VOLUNTEER")) // VOLUNTEER location
				volunteers.add(new Volunteer(id, m[1]));
			else
				System.err.printf("IGNORING INVALID REQUEST: %s\n", message);
		}
	}

	public void sendMessage(Volunteer v, Request r) {
		try {
			channel.sendMessage("LOCATION " + r.location + " " + r.urgency, v.id);
			channel.sendMessage("VOLUNTEER " + v.id + " " + times[getTime(v.location)][getTime(r.location)], r.id);
		} catch (ChannelException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args, int sleepTime) {
		Channel channel = new TCPChannel(Integer.parseInt(args[0]));
		Server server = new Server(channel, sleepTime);
		server.run();
	}

	public void run() {
		while(true) {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			synchronized (lock) {
			Resquest req;
			Volunteer vol;
				while (!(requests.isEmpty() || volunteers.isEmpty())) {
					ArrayList<Request> emergency = new ArrayList<Request>();
					ArrayList<Request> urgent = new ArrayList<Request>();
					ArrayList<Request> normal = new ArrayList<Request>();
					
					for (int x = 0; x < requests.size(); x++) {
						if (requests.get(x).urgency.equals("EMERGENCY")) 
							emergency.add(requests.get(x));
						else if (requests.get(x).urgency.equals("URGENT"))
							urgent.add(requests.get(x));
						else if (requests.get(x).urgency.equals("NORMAL"))
							normal.add(requests.get(x));
					}
					if (emergency.isEmpty() == false)
					{
						req = requests.remove(emergency.get(0));
					}
					else if (urgent.isEmpty() == false)
					{
						req = requests.remove(urgent.get(0));
					}
					else if (normal.isEmpty() == false)
					{
						req = requests.remove(normal.get(0));
					}
					int temp = 10;
					int index = 0;
					for (int i = 0; i < volunteers.size(); i++)
					{
						Volunteer temp = volunteers.get(i);
						int curTime = times[getTime(volunteer.location)][getTime(request.location)];
						if (curTime < temp)
						{
							temp = curTime;
							index = i;
						}			
					}
					vol = volunteers.remove(index);
					sendMessage(vol, req);
				}
			}
		]
	}
}




