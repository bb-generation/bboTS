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

import java.io.Serializable;

/**
 * A TSUser object represents a Teamspeak3 User.
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class TSUser implements Serializable, Cloneable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Initialized internal data
	 */
	public TSUser()
	{
		super();
	}

	/**
	 * Initializes internal data with provided parameters.
	 * @param clientId the current client ID of the Teamspeak user
	 * @param clientNickname the current nickname of the Teamspeak user
	 */
	public TSUser(int clientId, String clientNickname)
	{
		super();
		this.clientId = clientId;
		this.clientNickname = clientNickname;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		TSUser tsUser = (TSUser)super.clone();
		tsUser.clientId = clientId;
		tsUser.clientNickname = clientNickname;
		tsUser.clientUniqueId = clientUniqueId;
		return tsUser;
	}
	
	/**
	 * sets the current client ID
	 * @param clientId new client ID
	 */
	public void setClientId(int clientId)
	{
		this.clientId = clientId;
	}

	/**
	 * sets the nickname
	 * @param clientNickname the new nickname
	 */
	public void setClientNickname(String clientNickname)
	{
		this.clientNickname = clientNickname;
	}

	/**
	 * Returns the unique Teamspeak3 UID of the Teamspeak3 User.
	 * Warning: Users can change their UID, so don't expect too much stability.
	 * @return Teamspeak UID
	 */
	public String getClientUniqueId()
	{
		return clientUniqueId;
	}
	
	/**
	 * sets the client UID
	 * @param clientUniqueId the UID to be set
	 */
	public void setClientUniqueId(String clientUniqueId)
	{
		this.clientUniqueId = clientUniqueId;
	}
	
	/**
	 * returns the current client ID.
	 * This will change if the user reconnects or the server goes down.
	 * @return current client ID.
	 */
	public int getClientId()
	{
		return clientId;
	}
	
	/**
	 * returns the current nickname of the user.
	 * Warning: Nicknames are not unique and should not be used for authentication
	 * @return current nickname
	 */
	public String getClientNickname()
	{
		return clientNickname;
	}

	/**
	 * returns the ID of the channel which the user is currently in.
	 * @return channel ID of the user
	 */
	public int getChannelId()
	{
		return channelId;
	}

	/**
	 * sets the ID of the channel which the user is currently in
	 * @param channelId new channel ID of the user
	 */
	public void setChannelId(int channelId)
	{
		this.channelId = channelId;
	}



	private int clientId;
	private int channelId;

	private String clientNickname;
	private String clientUniqueId;
}
