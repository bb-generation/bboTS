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
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import at.bbgen.lborc.BOUser;
import at.bbgen.lborc.RConnection;
import at.bbgen.lborc.RConnectionException;

/**
 * BOConnection scans periodically for the player list.
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class BOConnection
{
	/**
	 * Initializes all local data.
	 * @param boServerAddress BO server address
	 * @param boServerPort RCon port
	 * @param boServerPassword RCon password
	 * @param boServerTimer timer period
	 * 
	 * @throws BOConnectionException Will be thrown if anything goes wrong.
	 */
	public BOConnection(InetAddress boServerAddress, int boServerPort, String boServerPassword, int boServerTimer) throws BOConnectionException
	{
		this.boServerPassword = boServerPassword;
		this.boServerAddress = boServerAddress;
		this.boServerPort = boServerPort;
		this.boServerTimer = boServerTimer;
		synchronized(this)
		{
			boUsers = new HashMap<Integer, BOUser>();
		}
		try
		{
			boServerWorker = new RConnection(this.boServerAddress, this.boServerPort, this.boServerPassword, 1000);
		} catch (RConnectionException e)
		{
			throw new BOConnectionException("Error while trying to instance BOServerWorker: "+e.getMessage());
		}
		
		//requestTimer = new Timer();
		timerEnabled = false;
	}
	
	/**
	 * Stops all local threads and timers
	 */
	public void stopAll()
	{
		disableTimer();
	}
	
	/**
	 * Initializes the internal BO Connection
	 */
	public void startBOWorker()
	{
		//boServerWorker.start();
	}
	
	/**
	 * Enables the timer, which scans periodically for new users
	 */
	public void enableTimer()
	{
		if(timerEnabled) return;
		timerEnabled = true;
		
		requestTimer = new Timer();
		requestTimer.scheduleAtFixedRate(new TimerTask()
		{
			public void run()
			{
				if(boServerWorker != null)
					refreshTeamStatus();
			}
		}, boServerTimer, boServerTimer);
	}
	
	private void refreshTeamStatus()
	{
		try
		{
			List<BOUser> newList = boServerWorker.sendTeamStatusRequest();
			HashMap<Integer, BOUser> newBoUsers = new HashMap<Integer, BOUser>();
			for(BOUser boUser : newList)
				newBoUsers.put(Integer.valueOf(boUser.getGuid()), boUser);
			synchronized(this)
			{
				boUsers = new HashMap<Integer, BOUser>(newBoUsers);
			}
		} catch (RConnectionException e)
		{
			System.out.println("Timer: Error while trying to sendRequest(): "+e.getMessage());
		}
	}

	/**
	 * Stops the timer. See {@link #enableTimer()}
	 */
	public void disableTimer()
	{
		if(!timerEnabled) return;
		
		requestTimer.cancel();
		timerEnabled = false;
	}

	/**
	 * Callback function as defined in {@link BOServerWorkerAction}
	 * The internal Black Ops connection will call it, when there is a new user list available
	 */
	/*@Override
	public void commitBOUsers(List<BOUser> users)
	{
		HashMap<Integer, BOUser> newBoUsers = new HashMap<Integer, BOUser>();
		for(BOUser user : users)
		{
			newBoUsers.put(Integer.valueOf(user.getGuid()), user);
		}
		synchronized(this)
		{
			boUsers = newBoUsers;
		}
	}*/
	
	/**
	 * Returns a Black Ops user with the given GUID
	 * @param guid Black Ops GUID (unique belong all black ops users)
	 * @return A Black Ops User instance will the given data
	 */
	public BOUser getUser(int guid)
	{
		//return new BOUser(1, null, 50386892, null, 1, -1, -1);
		
		BOUser retUser;
		synchronized(this)
		{
			BOUser boUser = boUsers.get(guid);
			if(boUser != null)
			{
				try
				{
					retUser = (BOUser) boUser.clone();
				} catch (CloneNotSupportedException e)
				{
					return null;
				}
			}
			else
				return null;
		}
		return retUser;
	}

	private boolean timerEnabled;
	private HashMap<Integer, BOUser> boUsers;
	private int boServerTimer;
	private Timer requestTimer;
	private RConnection boServerWorker;
	private String boServerPassword;
	private InetAddress boServerAddress;
	private int boServerPort;
}

