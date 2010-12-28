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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A team switcher instance which switches users playing on <b>one</b> Black Ops server.
 * 
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class BOTeamSwitcher
{
	/**
	 * initializes all local data, but does not connect to anything.
	 * 
	 * @param sProperties service properties class
	 * @param userProperties user properties class
	 * @param tsConnection connection to the TS server
	 * @param servername Label of this Black Ops server instance
	 */
	public BOTeamSwitcher(SProperties sProperties, UserProperties userProperties, TSConnection tsConnection, String servername)
	{
		this.sProperties = sProperties;
		this.userProperties = userProperties;
		this.servername = servername;
		boConnection = null;
		this.tsConnection = tsConnection;
		scanTimer = null;
	}
	
	/**
	 * Initializes and connects
	 * 
	 * @throws BOTeamSwitcherException Thrown if anyting goes wrong
	 */
	public void init() throws BOTeamSwitcherException
	{
		try
		{
			InetAddress boServerHost = InetAddress.getByName(sProperties.getBoServerHost(servername));
			
			boConnection = new BOConnection(boServerHost, sProperties.getBoServerPort(servername), sProperties.getBoServerPassword(servername), sProperties.getBoScanInterval(servername));
			
			scanTimer = new Timer();
			scanTimer.scheduleAtFixedRate(new TimerTask()
			{
				public void run()
				{
					timerCall();
				}
			}, sProperties.getTsScanInterval(), sProperties.getTsScanInterval());
			
			boConnection.startBOWorker();
			
		} catch (UnknownHostException e)
		{
			throw new BOTeamSwitcherException("Error while trying to resolve Black Ops Host Server: "+e.getMessage());
		} catch (BOConnectionException e)
		{
			throw new BOTeamSwitcherException("Error while trying to start Black Ops Connection Interface: "+e.getMessage());
		}
	}
	
	/**
	 * Stops this service and all threads created by it
	 */
	public void stopService()
	{
		scanTimer.cancel();
		
		try
		{
			tsConnection.disconnect();
		} catch (TSConnectionException e)
		{
			System.out.println("Error while trying to disconnect from TS3 Connection: "+e.getMessage());
		}
		
		boConnection.stopAll();
	}

	/**
	 * Will be called every TSScanningInterval seconds
	 * Does all the magical switching.
	 */
	private void timerCall()
	{
		try
		{
			List<TSUser> tsUsers = tsConnection.getClientList();
			List<TSUser> tsUsersI = new LinkedList<TSUser>(); // interesting users = users with known guid=tsUID reference
			for(TSUser tsUser : tsUsers)
			{
				if(userProperties.getUser(tsUser.getClientUniqueId()) != null && sProperties.isListeningChannel(servername, tsUser.getChannelId()))
					tsUsersI.add(tsUser);
			}
			
			if(tsUsersI.isEmpty())
			{
				boConnection.disableTimer();
				return;
			}
			else
			{
				boConnection.enableTimer();
			}

			
			ArrayList<LinkedList<TSUser>> moveList = new ArrayList<LinkedList<TSUser>>();
			for(int i=0;i<3;++i)
				moveList.add(new LinkedList<TSUser>());
			
			int userPlayingCount = 0;
			
			for(TSUser tsUser : tsUsersI)
			{
				BOUser boUser = null;
				String tsUID = tsUser.getClientUniqueId();
				int guid = userProperties.getUser(tsUID).getBoGUID();
				boUser = boConnection.getUser(guid);
				if(boUser == null)
					continue;
				
				int curChannel = tsUser.getChannelId();
				int newChannel = -1;
				try
				{
					newChannel = sProperties.getTeamChannels(servername, boUser.getTeam());
					
					userPlayingCount++;
					
					if(newChannel != -1 && newChannel != curChannel)
					{
						if(boUser.getTeam() < 3)
							moveList.get(boUser.getTeam()).add(tsUser);
					}
				} catch (SPropertiesException e)
				{
					System.out.println("Error while trying to retrieve team channel: "+e.getMessage());
				}
			}
			
			// don't switch if only less than 3 players are playing
			if(userPlayingCount < 3)
				return;
			
			for(int i=0;i<3;++i)
			{
				int newChannel;
				try
				{
					newChannel = sProperties.getTeamChannels(servername, i);
					if(newChannel == -1)
						continue;
					
					if(moveList.get(i).size() < 1)
						continue; // nothing to move
					
					tsConnection.moveUserList(moveList.get(i), newChannel, null); // TODO: implement password
					
				} catch (SPropertiesException e)
				{
					System.out.println("Error while trying to retrieve team channel: "+e.getMessage());
				}
			}
			
		} catch (TSConnectionException e)
		{
			System.out.println("Error while trying to move users: "+e.getMessage());
		}
		
	}
	
	private String servername;
	private Timer scanTimer;
	private BOConnection boConnection;
	private TSConnection tsConnection;
	private SProperties sProperties;
	private UserProperties userProperties;
}
