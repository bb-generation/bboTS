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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import at.bbgen.ejts3serverquery.EJTS3ServerQuery;
import at.bbgen.ejts3serverquery.EJTS3ServerQueryException;


/**
 * TSConnection is the class used for all communications with the Teamspeak3 
 * server.<br><br>
 * It does not connect on creation. If you want to conenct manually, use
 * connect(). Otherwise the current connection status is checked at the
 * beginning of every method and - if needed - a new connection is
 * established.<br>
 * Please use the disconnect() method to disconnect from the server.
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 * 
 */
public class TSConnection
{
	/**
	 * Initializes all internal data structures and all libraries used for
	 * connecting to the server.
	 * It does NOT connect to the server (use connect()).
	 * 
	 * @param tsHost					Hostname or IP of the TS3 server. (e.g. example.com or 127.0.0.1)
	 * @param tsPort					Serverquery Port of the TS3 server. (e.g. 10011)
	 * @param tsUsername			Username of the TS3 Serverquery Account
	 * @param tsPassword			Password of the TS3 Serverquery Account
	 * @param tsVirtualServer	TS3 VServer ID
	 */
	public TSConnection(String tsHost, int tsPort, String tsUsername, String tsPassword, int tsVirtualServer)
	{
		super();
		this.tsHost = tsHost;
		this.tsPort = tsPort;
		this.tsUsername = tsUsername;
		this.tsPassword = tsPassword;
		this.tsVirtualServer = tsVirtualServer;
		
		ts3Query = new EJTS3ServerQuery();
	}
	
	/**
	 * Connects to the TS3 server. Connection information is provided by the constructor.
	 * The function connects to the TS3 Server, tries to login, selects the TS3 VServer
	 * and sets the display name.
	 * 
	 * @throws TSConnectionException	Will be thrown if any error occurred. (e.g. host not reachable, login data invalid, vserver not selectable, display name not settable)
	 */
	public synchronized void connect() throws TSConnectionException
	{
		try
		{
			ts3Query.connectTS3Query(tsHost, tsPort);
			ts3Query.loginTS3(tsUsername, tsPassword);
			ts3Query.selectVirtualServer(tsVirtualServer);
			ts3Query.setDisplayName("bblack ops team switcher");
		} catch(EJTS3ServerQueryException e)
		{
			throw new TSConnectionException("Error while trying to connect to TS3 server: "+e.getMessage());
		}
		
	}

	/**
	 * Disconnects from the TS3 Server.
	 * 
	 * @throws TSConnectionException Will be thrown if disconnect failed.
	 */
	public synchronized void disconnect() throws TSConnectionException
	{
		if(ts3Query == null)
			throw new TSConnectionException("Error while trying to disconnect: ts3Query == null");
		try
		{
			ts3Query.closeTS3Connection();
		} catch (EJTS3ServerQueryException e)
		{
			throw new TSConnectionException("Error while trying to disconnect: "+e.getMessage());
		}
	}
	
	/**
	 * Retrieves a list of all clients.
	 * This client list does not have to be real-time!
	 * 
	 * @return list of all clients connected to the TS3 Server
	 * @throws TSConnectionException Will be thrown if the client list can not be retrieved or if the server sends illegal characters.
	 */
	public synchronized List<TSUser> getClientList() throws TSConnectionException
	{
		if(ts3Query == null)
			throw new TSConnectionException("ts3query == null");
		
		if(!ts3Query.isConnected())
			connect();
		
		LinkedList<TSUser> tsUserList = new LinkedList<TSUser>();
		
		Vector<HashMap<String, String>> dataClientList;
		try
		{
			dataClientList = ts3Query.getList(EJTS3ServerQuery.LISTMODE_CLIENTLIST, "-uid");
		} catch (EJTS3ServerQueryException e1)
		{
			throw new TSConnectionException("Error while trying to get client list: "+e1.getMessage());
		}
		if(dataClientList  == null)
			throw new TSConnectionException("Error while trying to get client list.");
		
		for(HashMap<String, String> user : dataClientList)
		{
			TSUser tsUser = new TSUser();
			
			String sClientId = user.get("clid");
			if(sClientId == null)
				throw new TSConnectionException("Error while trying to get client list: some client does not have a client id.");
			int clientId;
			try
			{
				clientId = Integer.parseInt(sClientId);
			} catch(NumberFormatException e)
			{
				throw new TSConnectionException("Error while trying to convert clientId ("+sClientId+") to Integer: "+e.getMessage());
			}
			tsUser.setClientId(clientId);
			
			String sClientNickname = user.get("client_nickname");
			if(sClientNickname != null)
			{
				tsUser.setClientNickname(sClientNickname);
			}
			
			String sClientUID = user.get("client_unique_identifier");
			if(sClientUID != null)
			{
				tsUser.setClientUniqueId(sClientUID);
			}
			
			String sChannelID = user.get("cid");
			if(sChannelID != null)
			{
				int channelId;
				try
				{
					channelId = Integer.parseInt(sChannelID);
				} catch(NumberFormatException e)
				{
					throw new TSConnectionException("Error while trying to convert channelId ("+sChannelID+") to Integer: "+e.getMessage());
				}
				tsUser.setChannelId(channelId);
			}
			
			
			if(sClientUID != null && sClientUID.length() == 28) // only add client if there is a UID
			{
				tsUserList.add(tsUser);
			}
		}
		
		return tsUserList;
	}
	
	/**
	 * Moves a list of users from their current channels to another Channel (channelId).
	 *
	 * @param tsUsers		a list of users to be moved
	 * @param channelId	the ID of the destination channel
	 * @param password	the password of the destination channel or {@code null} if no password needed.
	 * @throws TSConnectionException Will be thrown if at least one user could not be moved.
	 */
	public synchronized void moveUserList(List<TSUser> tsUsers, int channelId, String password) throws TSConnectionException
	{
		if(ts3Query == null)
			throw new TSConnectionException("ts3query == null");
		
		if(!ts3Query.isConnected())
			connect();
		
		
		List<Integer> clientIDs = new LinkedList<Integer>();
		for(TSUser tsUser : tsUsers)
			clientIDs.add(tsUser.getClientId());

		try
		{
			ts3Query.moveClientList(clientIDs, channelId, password);
		} catch(EJTS3ServerQueryException e)
		{
			throw new TSConnectionException("Warning: Error while trying to move clients: "+e.getMessage());
		}
		
	}
	
	/**
	 * Moves a user to a new channel (channelId).
	 * 
	 * @param tsUser		The User to be moved
	 * @param channelId	ID of the destination channel
	 * @param password	the password of the destination channel or {@code null} if no password needed.
	 * @throws TSConnectionException Will be thrown if the user could not be moved.
	 */
	public synchronized void moveUser(TSUser tsUser, int channelId, String password) throws TSConnectionException
	{
		if(ts3Query == null)
			throw new TSConnectionException("ts3query == null");
		
		if(!ts3Query.isConnected())
			connect();
		
		try
		{
			ts3Query.moveClient(tsUser.getClientId(), channelId, password);
		} catch(EJTS3ServerQueryException e)
		{
			System.out.println("Warning: Error while trying to move client "+tsUser.getClientNickname()+" to channel "+channelId+": "+e.getMessage());
		}
	}
	
	/**
	 * Returns a String containing the last error from the TS3 connection
	 * 
	 * @return last error message
	 */
	/*private synchronized String getErrorString()
	{
		String error = ts3Query.getLastError();
		if (error != null)
		{
			System.out.println(error);
			if (ts3Query.getLastErrorPermissionID() != -1)
			{
				HashMap<String, String> permInfo = ts3Query.getPermissionInfo(ts3Query.getLastErrorPermissionID());
				if (permInfo != null)
				{
					System.out.println("Missing Permission: " + permInfo.get("permname"));
				}
			}
		}
		return error;
	}*/

	private EJTS3ServerQuery ts3Query;
	private String tsHost;
	private int tsPort;
	
	private String tsUsername;
	private String tsPassword;
	private int tsVirtualServer;
}

