/*
 *    This file is part of bboTS.
 *    
 *    Copyright 2010 Bernhard Eder
 * 
 *    bboTS is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    bboTS is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with bboTS.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.bbgen.bbots;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * BOServerWorker sends teamStatus requests to the BO server.<br>
 * It uses an asynchronous call:<br>
 * After initializing the class, the superior class have to call {@link #registerAction(BOServerWorkerAction)} to set the callback class.<br>
 * Then after calling {@link #sendTeamStatusRequest()} the data will be transmitted to the server.<be>
 * When all data is received, the class will call {@link BOServerWorkerAction#commitBOUsers(List)} and inform its superior class about the received user list.
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class BOServerWorker extends Thread
{
	/**
	 * Initializes all local data but does not send anything to the server yet.
	 * 
	 * @param serverAddress Adress of the BO server
	 * @param serverPort Port of the RCon interface
	 * @param password RCon Password
	 * @throws BOWorkerException Will be thrown if the socket can not be opened.
	 */
	public BOServerWorker(InetAddress serverAddress, int serverPort, String password) throws BOWorkerException
	{
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.password = password;
		this.boActionClass = null;
		generateTeamStatusPackage();
		try
		{
			this.socket = new DatagramSocket();
		} catch (SocketException e)
		{
			throw new BOWorkerException("Error while trying to open udp socket: "+e.getMessage());
		}
	}
	
	/**
	 * Sends a teamStatus request to the BO server.
	 * 
	 * @throws BOWorkerException Will be thrown if the package could not be sent.
	 */
	public void sendTeamStatusRequest() throws BOWorkerException
	{
		if(sendTeamStatusPackage == null || sendTeamStatusPackage.length < 18)
			throw new BOWorkerException("Send Package has not been created yet.");
		DatagramPacket packet = new DatagramPacket(sendTeamStatusPackage, sendTeamStatusPackage.length, serverAddress, serverPort);
		try
		{
			socket.send(packet);
		} catch (IOException e)
		{
			throw new BOWorkerException("Error while trying to send UDP package: "+e.getMessage());
		}
	}
	
	/**
	 * Registers the callback class. See {@link BOServerWorker} for more details.
	 * 
	 * @param boActionClass The callback class
	 * @throws BOWorkerException Will be thrown if {@link #boActionClass} is invalid.
	 */
	public void registerAction(BOServerWorkerAction boActionClass) throws BOWorkerException
	{
		if(boActionClass != null)
			this.boActionClass = boActionClass;
		else
			throw new BOWorkerException("registerAction() error: boActionClass is null");
	}
	
	/**
	 * BOServerWorker runs in it's own thread and checks permanently for new incoming packages.
	 * 
	 */
	@Override
	public void run()
	{
		byte[] buf = new byte[BUFFERSIZE];
		while(true)
		{
			try
			{
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				//String received = new String(packet.getData(), 0, packet.getLength());
				//System.out.println("Quote of the Moment: " + received);
				

				List<BOUser> users = new LinkedList<BOUser>();
				
				
				int count_0a = 0;
				
				//System.out.println(getHex(packet.getData()));
				
				// analyze if the current package contains 0x0a 0x0a:
				boolean contains2LF = false;
				twoLFLoop:
				for(int i=0;i<packet.getLength()-1; ++i)
				{
					if(packet.getData()[i] == 0x0a && packet.getData()[i+1] == 0x0a)
					{
						contains2LF = true;
						break twoLFLoop;
					}
				}
				
				
				byte[] analyzePacket = new byte[0];
				
				if(!contains2LF)
				{
					tempPackage = new byte[packet.getLength()];
					System.arraycopy(packet.getData(), 0, tempPackage, 0, packet.getLength());
					tempPackageLength = packet.getLength();
				}
				else
				{
					if(tempPackage != null)
					{
						// TODO: presumption: package order is not reversed -> implement checking for "print.map"
						analyzePacket = new byte[tempPackageLength+packet.getLength()  -12];
						System.arraycopy(tempPackage, 0, analyzePacket, 0, tempPackageLength-1);
						System.arraycopy(packet.getData(), 11, analyzePacket, tempPackageLength-1, packet.getLength()-11);
						tempPackage = null;
					}
					else
						analyzePacket = packet.getData();
					
					packetLoop:
					for(int i=0; i<analyzePacket.length; ++i)
					{
						if(analyzePacket[i] == 0x0a)
							count_0a++;
						
						if(analyzePacket[i] == 0x0a && analyzePacket[i+1] == 0x0a)
						{
							//System.out.println("i="+i);
							//System.out.println("got 0x0a 0x0a");
							break packetLoop;
						}
						
						if(analyzePacket[i] == 0x0a && count_0a > 3)
						{
							// geting length:
							int j=0;
							for(j=i+1;j<analyzePacket.length;++j)
							{
								if(analyzePacket[j] == 0x0a)
									break;
							}
							
							byte[] line = new byte[j-i-1];
							System.arraycopy(analyzePacket, i+1, line, 0, j-i-1);
							
							String sLine = new String(line, "US-ASCII");
							
							ArrayList<String> parameters = splitParameter(sLine);
							
							if(parameters.size() < 10)
							{
								i += 1;
								continue packetLoop;
							}
							
							//System.out.println("i="+i);
							//System.out.println("|"+getHex(getParameter(packet.getData(), i, 94).getBytes()));
							/**** ID ****/
							String sId = parameters.get(0);
							//System.out.println("id = "+getHex(sId.getBytes()));
							int id = -1;
							try
							{
								id = Integer.parseInt(sId.trim());
							} catch (NumberFormatException e)
							{
								System.out.println("Error while trying to convert id ("+sId+"): "+e.getMessage());
								
								i+=1;
								continue packetLoop;
							}
							
							if(id < 1) // just the democlient
							{
								i+=1;
								continue packetLoop;
							}
							
							/**** Score ****/
							
							/**** Ping ****/
							
							/**** GUID ****/
							//String sGUID = getParameter(analyzePacket, i+OFF_GUID, LENGTH_GUID);
							String sGUID = parameters.get(3);
							int guid = -1;
							try
							{
								guid = Integer.parseInt(sGUID.trim());
							}	catch (NumberFormatException e)
							{
								System.out.println("Error while trying to convert guid ("+sGUID+"): "+e.getMessage());
								
								i+=1;
								continue packetLoop;
							}
							
							
							/**** Nickname ****/
							
							/**** Team ****/
							//String sTeam = getParameter(analyzePacket, i+OFF_TEAM, LENGTH_TEAM);
							String sTeam = parameters.get(parameters.size()-5);
							int team = -1;
							try
							{
								team = Integer.parseInt(sTeam.trim());
							} catch (NumberFormatException e)
							{
								System.out.println("Error while trying to convert team ("+sTeam+"): "+e.getMessage());
								i+=1;
								continue packetLoop;
							}
							if(team > 2 || team < 0)
								continue packetLoop; // invalid team -> ignore (probably CNCT?)
							
							BOUser boUser = new BOUser();
							boUser.setId(id);
							boUser.setGuid(guid);
							boUser.setTeam(team);
							
							if(id > 0)
							{
								users.add(boUser);
								//System.out.println("Added user: "+boUser);
							}
							
							i += 1;
							
						}
					}
					
					
					if(boActionClass != null)
						boActionClass.commitBOUsers(users);
					else
						System.out.println("Cannot commit to boActionClass: boActionClass == null");
					
				}

			} catch (IOException e)
			{
				if(!socket.isClosed())
					System.out.println("Error while trying to receive UDP packages.");
				else
					return;
			}
		}
	}
	
	/**
	 * Is used to split the columns returned by the rcon protocol
	 * @param sLine a whole line given by the rcon protocol (e.g. by the teamStatus request)
	 * @return a List of cells
	 */
  private ArrayList<String> splitParameter(String sLine)
	{
		ArrayList<String> retList = new ArrayList<String>();
		
		StringBuilder par = new StringBuilder();
		for(int i=0;i<sLine.length();++i)
		{
			if((int)sLine.charAt(i) == 32)
			{
				if(par.length() > 0)
				{
					retList.add(par.toString());
					par = new StringBuilder();
				}
			}
			else
				par.append(sLine.charAt(i));
		}
		if(par.length() > 0)
			retList.add(par.toString());
  	
		return retList;
	}

	/**
	 * encodes specified bytes by US-ASCII
	 * @param data byte data array
	 * @param start offset
	 * @param count length
	 * @return String representation of the encoded byte[] array.
	 */
	public String getParameter(byte[] data, int start, int count)
	{
		try
		{
			return new String(data, start, count, "US-ASCII");
		} catch (UnsupportedEncodingException e)
		{
			System.out.println("unsupported encoding: "+e.getMessage());
			return "";
		}
	}
	
	/**
	 * stops the internal thread
	 */
	public void stopWorker()
	{
		socket.close();
	}
	
	/**
	 * genereates a teamStatus Package (so it does not have to be generated each request) 
	 */
	private void generateTeamStatusPackage()
	{
		sendTeamStatusPackage = new byte[17+password.length()];
		int i=0;
		sendTeamStatusPackage[i++] = sendTeamStatusPackage[i++] = sendTeamStatusPackage[i++] = sendTeamStatusPackage[i++] = (byte)0xff;
		sendTeamStatusPackage[i++] = 0x00;
		for(int j=0;j<password.length();++j)
		{
			sendTeamStatusPackage[i++] = (byte)password.charAt(j);
		}
		sendTeamStatusPackage[i++] = 0x20;
		String teamstatus = "teamstatus";
		for(int j=0;j<teamstatus.length();++j)
		{
			sendTeamStatusPackage[i++] = (byte)teamstatus.charAt(j);
		}
		sendTeamStatusPackage[i++] = 0x00;
	}
	
	/*private static final int OFF_ID = 1;
	private static final int LENGTH_ID = 3;
	
	private static final int OFF_SCORE = 5;
	private static final int LENGTH_SCORE = 5;
	
	private static final int OFF_PING = 11;
	private static final int LENGTH_PING = 4;
	
	private static final int OFF_GUID = 16;
	private static final int LENGTH_GUID = 8;
	
	private static final int OFF_NICK = 25;
	private static final int LENGTH_NICK = 20;
	
	private static final int OFF_TEAM = 46;
	private static final int LENGTH_TEAM = 1;*/
	
	
	private int tempPackageLength;
	private byte[] tempPackage;
	
	private byte[] sendTeamStatusPackage;
	private DatagramSocket socket;
	private static final int BUFFERSIZE = 32768;
	private BOServerWorkerAction boActionClass;
	private String password;
	private InetAddress serverAddress;
	private int serverPort;
}

