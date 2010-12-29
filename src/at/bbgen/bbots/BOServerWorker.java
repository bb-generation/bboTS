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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
public class BOServerWorker
{
	/**
	 * Initializes all local data but does not send anything to the server yet.
	 * 
	 * @param serverAddress Adress of the BO server
	 * @param serverPort Port of the RCon interface
	 * @param password RCon Password
	 * @throws BOWorkerException Will be thrown if the socket can not be opened.
	 */
	public BOServerWorker(InetAddress serverAddress, int serverPort, String password, int connectionTimeout) throws BOWorkerException
	{
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.password = password;
		this.connectionTimeout = connectionTimeout;
		generateTeamStatusPackage();
	}
	
	/**
	 * Sends a teamStatus request to the BO server.
	 * 
	 * @throws BOWorkerException Will be thrown if the package could not be sent.
	 */
	public List<BOUser> sendTeamStatusRequest() throws BOWorkerException
	{
		if(sendTeamStatusPackage == null || sendTeamStatusPackage.length < 18)
			throw new BOWorkerException("Send Package has not been created yet.");
		DatagramPacket packet = new DatagramPacket(sendTeamStatusPackage, sendTeamStatusPackage.length, serverAddress, serverPort);

		String recPackage = sendPacket(packet);
		String[] pkgLines = recPackage.split("\n"); // line feed split
		int i = 0;
		List<BOUser> retList = new LinkedList<BOUser>();
		for(String line : pkgLines)
		{
			StrParser curLine = new StrParser(line);
			
			if(i==0) // map: [....]
			{
				// if(curLine.readSimpleString().equals("map:"))
				//	System.out.println("Current map is "+curLine.readSimpleString());
			}
			// else if(i == 1 || i == 2) // table headers
			// { }	
			else if(i > 2)
			{
				BOUser boUser = new BOUser();
				
				try
				{
					boUser.setId(curLine.readInteger());
					boUser.setScore(curLine.readInteger());
					boUser.setPing(curLine.readInteger());
					boUser.setGuid(curLine.readInteger());
					boUser.setName(curLine.readUsername());
					boUser.setTeam(curLine.readInteger());
					boUser.setLastmsg(curLine.readInteger());
					boUser.setIpAddress(curLine.readSimpleString());
					boUser.setQport(curLine.readInteger());
					boUser.setRate(curLine.readInteger());
					
					
					if(boUser.getId()>0) // only add real players (not the democlient)
					{
						System.out.println("added..."+boUser);
						retList.add(boUser);
					}
				} catch (BOWorkerException e)
				{
					System.out.println("Error while trying to parse line: "+e.getMessage());
				}
			}
			
			++i;
		}
		return retList;
	}
	
	
	private String sendPacket(DatagramPacket packet) throws BOWorkerException
	{
		DatagramSocket dSocket = null;
		try
		{
			dSocket = new DatagramSocket();
			dSocket.setSoTimeout(connectionTimeout);
			dSocket.send(packet);
		} catch (SocketException e)
		{
			throw new BOWorkerException("Error while trying to send package: "+e.getMessage());
		} catch (IOException e)
		{
			throw new BOWorkerException("Error while trying to send package: "+e.getMessage());
		}
		byte[] buf = new byte[BUFFERSIZE];
		DatagramPacket receivePackage = new DatagramPacket(buf, buf.length);
		
		StringBuffer retString = new StringBuffer();
		try
		{
			while(true)
			{
				dSocket.receive(receivePackage);
				
				retString.append(new String(receivePackage.getData(), 11, receivePackage.getLength()-11, "US-ASCII"));
			}
		} catch (SocketTimeoutException e)
		{
			// intended exit for endless loop
			
		} catch (IOException e)
		{
			throw new BOWorkerException("Error while trying to receive package: "+e.getMessage());
		}
		
		
		return retString.toString();
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
		
	private byte[] sendTeamStatusPackage;
	private static final int BUFFERSIZE = 2048;
	private String password;
	private InetAddress serverAddress;
	private int serverPort;
	private int connectionTimeout;
}


class StrParser
{
	public StrParser(String line)
	{
		this.totalString = line;
		this.stringLeft = line;
	}
	
	public int readInteger() throws BOWorkerException
	{
		int offset = 0;
		
		// skip leading whitespaces
		for(;offset<stringLeft.length();++offset)
		{
			if(stringLeft.charAt(offset) != ' ')
				break;
		}
		
		// get string representation of integer
		StringBuffer sVal = new StringBuffer();
		for(;offset<stringLeft.length();++offset)
		{
			char curChar = stringLeft.charAt(offset);
			if(curChar == ' ')
				break;
			else if(curChar != '-' && (curChar < '0' || curChar > '9'))
				throw new BOWorkerException("Illegal integer value ("+totalString+" | "+stringLeft+")");
			
			sVal.append(curChar);
		}
		
		int retVal = -1;
		try
		{
			retVal = Integer.parseInt(sVal.toString());
		} catch (NumberFormatException e)
		{
			throw new BOWorkerException("Unable to convert integer (from string: "+stringLeft+") :"+e.getMessage());
		}
		
		// set new left string
		stringLeft = stringLeft.substring(offset);
		
		return retVal;
	}
	
	public String readUsername() throws BOWorkerException
	{
		int offset = 0;
		
		// skip leading whitespaces
		for(;offset<stringLeft.length();++offset)
		{
			if(stringLeft.charAt(offset) != ' ')
				break;
		}
		
		StringBuffer sVal = new StringBuffer();
		for(;offset<stringLeft.length()-2;++offset)
		{
			char curChar = stringLeft.charAt(offset);
			char nextChar = stringLeft.charAt(offset+1);
			char nnextChar = stringLeft.charAt(offset+2);
			
			sVal.append(curChar);
			
			if(nextChar == '^' && nnextChar == '7')
				break;
			
		}
		
		// set new left string
		stringLeft = stringLeft.substring(offset+3);
		
		return sVal.toString();
	}
	
	public String readSimpleString() throws BOWorkerException
	{
		int offset = 0;
		
		// skip leading whitespaces
		for(;offset<stringLeft.length();++offset)
		{
			if(stringLeft.charAt(offset) != ' ')
				break;
		}
		
		StringBuffer sVal = new StringBuffer();
		for(;offset<stringLeft.length();++offset)
		{
			char curChar = stringLeft.charAt(offset);
			
			if(curChar == ' ')
				break;
			
			sVal.append(curChar);
		}
		
		// set new left string
		stringLeft = stringLeft.substring(offset);
		
		return sVal.toString();
	}
	
	private String totalString;
	private String stringLeft;
}