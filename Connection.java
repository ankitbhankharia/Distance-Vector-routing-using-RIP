/*
 * Connection.java
 * 
 * Project 1
 * 
 * Implementing Distance Vector Routing using RIP2
 * 
 * @version: Connection.java v2.2, 10/5/2015, 10:00 pm
 * 
 * Author: Ankit Bhankharia - atb5880
 * 
 */


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

/**
 * Connection class takes input from the user
 * and creates threads for the client and server
 * 
 * @author Ankit
 *
 */
public class Connection {

	// Stores destination and cost to reach
	public static HashMap<String, Integer> table;
	// Stores local ip address
	static String[] localIP;
	// Stores it's neighbors
	static ArrayList<String> neighbors;
	//Stores destination and next hop
	public static HashMap<String, String> hop;
	// Subnet mask
	public static String subnetMask = "255.255.255.0";
	
	public static void main(String[] args) throws IOException,
			InterruptedException {
		table = new HashMap<String, Integer>();
		hop = new HashMap<String, String>();
		neighbors = new ArrayList<String>();
		System.out.println(InetAddress.getLocalHost() + " connection started");
		localIP = InetAddress.getLocalHost().toString().split("/");
		System.out.println("Updated position 1");
		table.put(localIP[1], 0);
		hop.put(localIP[1], localIP[1]);
		// Starts the server
		Server server = new Server();
		server.start();

		int length = args.length;
		// User input for the neighbor ip and cost
		Scanner scan = new Scanner(System.in);
		for (int i = 0; i < length; i++) {
			System.out.println("Enter the cost for " + args[i].toString());
			int cost = scan.nextInt();
			new Client(args[i], cost).start();
		}
		scan.close();
	}

	/**
	 * Updates the destination cost and the next hop
	 * 
	 * @param temp - routing table of neighbor
	 * @param senderIP - sender ip address
	 * @param tempHop - Next hops of the sender
	 */
	public static void updateTable(Map<String, Integer> temp, String senderIP, Map<String, String> tempHop) {
		// updates the table if neighbor has smaller value
		if (table.containsKey(senderIP) && temp.containsKey(localIP[1])) {
			if (table.get(senderIP) > temp.get(localIP[1])) {
				table.put(senderIP, temp.get(localIP[1]));
				if(tempHop.get(localIP[1]).equals(localIP[1])) {
					hop.put(senderIP, senderIP);
				}
				else {
					for(int i = 0; i < neighbors.size(); i++) {
						if(!neighbors.get(i).equals(senderIP)) {
							hop.put(senderIP, neighbors.get(i));
						}
					}
				}
				
			}
		}
		
		// Add the neighbor if neighbor has its entry
		if (!table.containsKey(senderIP)) {
			table.put(senderIP, temp.get(localIP[1]));
			hop.put(senderIP, senderIP);
		}

		@SuppressWarnings("rawtypes")
		Iterator it = temp.entrySet().iterator();
		while (it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry) it.next();
			String ip = pair.getKey().toString();
			if (!table.containsKey(ip)) {
				table.put(ip, Integer.parseInt(pair.getValue().toString())
						+ table.get(senderIP));
				hop.put(ip, senderIP);
			}

			else {
				if (table.get(ip) > (Integer.parseInt(pair.getValue()
						.toString()) + table.get(senderIP))) {
					table.put(
							ip,
							(Integer.parseInt(pair.getValue().toString()) + table
									.get(senderIP)));
					hop.put(ip, senderIP);
				}
			}
		}
	}

	/**
	 * Displays the desired output i.e.
	 * Destination, subnet mask, cost and next hop
	 */
	public static void display() {
		@SuppressWarnings("rawtypes")
		Iterator it = table.entrySet().iterator();
		System.out.println("------ UPDATED TABLE -----");
		System.out.println("Destination" + '\t' + "Subnet Mask" + '\t' + "Cost" + '\t' + "Next Hop");
		while(it.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry pair = (Map.Entry) it.next();
			String[] ipSubParts = pair.getKey().toString().split("\\.");
			String[] subnetSubParts = subnetMask.split("\\.");
			String[] netID = new String[4];
			for(int i = 0; i < ipSubParts.length; i++) {
				netID[i] = Integer.toString((Integer.parseInt(ipSubParts[i]) & Integer.parseInt(subnetSubParts[i])));
			}
			String networkID = netID[0] + "." + netID[1] + "." + netID[2] + "." + netID[3];
			// Desired output displayed
			System.out.println(networkID + '\t' + subnetMask + '\t' + pair.getValue() + '\t' + hop.get(pair.getKey().toString()));
		}
		System.out.println();
		System.out.println("----------------------------------------------");
	}

}

/**
 * Server class accepts the connection from the neighbors
 * It waits for neighbors input, updates table
 * and sends updated table
 * @author Ankit
 *
 */
class Server extends Thread {

	private static ServerSocket server;
	private Socket connection;
	ObjectInputStream input;
	ObjectOutputStream output;

	public Server() throws IOException {
		server = new ServerSocket(6969);
	}

	public void run() {
		try {
			accept();
		} catch (UnknownHostException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void accept() throws UnknownHostException, ClassNotFoundException {
		try {
			int count = 0;
			while (count < 2) {
				count++;
				System.out.println(InetAddress.getLocalHost()
						+ " is waiting for connection " + count);
				// accepts the connection from the neighbors
				connection = server.accept();
				System.out.println(InetAddress.getLocalHost()
						+ " connected to " + connection.getInetAddress());
				String[] connectionIP = connection.getInetAddress().toString()
						.split("/");
				Connection.neighbors.add(connectionIP[1]);
				output = new ObjectOutputStream(connection.getOutputStream());
				input = new ObjectInputStream(connection.getInputStream());

				// Creates new thread to interact with the client
				new Thread(new Runnable() {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					@Override
					public void run() {
						while (true) {
							try {
								HashMap<HashMap<String, Integer>, HashMap<String, String>> receiver;
								// reads the input from the neighbor
								receiver = (HashMap<HashMap<String,Integer>, HashMap<String,String>>) input.readObject();
								HashMap<String, Integer>temp = (HashMap<String, Integer>) receiver.keySet().toArray()[0];
								HashMap<String, String> tempHop = receiver.get(temp);
								@SuppressWarnings("unused")
								Iterator it = temp.entrySet().iterator();
								Connection.updateTable(temp, connectionIP[1], tempHop);
								HashMap<HashMap<String, Integer>, HashMap<String, String>> sender;
								sender = new HashMap<HashMap<String,Integer>, HashMap<String,String>>();
								sender.put(Connection.table, Connection.hop);
								// sends the updated table to its neighbors
								output.writeObject(sender);
								output.reset();
								Connection.display();
								Thread.sleep(1000);
							} catch (Exception e) {
								System.exit(0);
							}
						}
					}
				}).start();

			}
		} catch (IOException e) {
			System.exit(0);
		}
	}
}

/**
 * Client asks for the connection to the server
 * It sends current routing table and recieves the updated routing table
 * 
 * @author Ankit
 *
 */
class Client extends Thread {

	Socket connection;
	String ipAddress = "";
	int cost;
	ObjectInputStream input;
	ObjectOutputStream output;

	public Client(String ip, int cost) throws IOException {
		ipAddress = ip;
		this.cost = cost;
	}

	@SuppressWarnings({ "unused", "unchecked" })
	public void run() {
		try {
			// connects to the server
			connection = new Socket(ipAddress, 6969);
			Connection.neighbors.add(ipAddress);
			output = new ObjectOutputStream(connection.getOutputStream());
			input = new ObjectInputStream(connection.getInputStream());
			System.out.println(InetAddress.getLocalHost().toString()
					+ " Connected to: " + ipAddress + " with cost: " + cost);
			if(Connection.table.containsKey(ipAddress)) {
				if(Connection.table.get(ipAddress) > cost) {
					Connection.table.put(ipAddress, cost);
					Connection.hop.put(ipAddress, ipAddress);
				}
			}
			else {
				Connection.table.put(ipAddress, cost);
				Connection.hop.put(ipAddress, ipAddress);
			}

			while (true) {
				Connection.display();
				HashMap<HashMap<String, Integer>, HashMap<String, String>> sender;
				sender = new HashMap<HashMap<String,Integer>, HashMap<String,String>>();
				sender.put(Connection.table, Connection.hop);
				// sends updated cost and next hop table to the neighbor
				output.writeObject(sender);
				output.reset();

				HashMap<HashMap<String, Integer>, HashMap<String, String>> receiver;
				// reads updated cost and next hop table from neighbor
				receiver = (HashMap<HashMap<String,Integer>, HashMap<String,String>>) input.readObject();
				HashMap<String, Integer>temp = (HashMap<String, Integer>) receiver.keySet().toArray()[0];
				HashMap<String, String> tempHop = receiver.get(temp);				@SuppressWarnings("rawtypes")
				Iterator it = temp.entrySet().iterator();
				Connection.updateTable(temp, ipAddress, tempHop);
				Thread.sleep(1000);
			}

		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to the network: "
					+ e.getMessage());
		} catch (IOException e) {
			System.exit(0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}