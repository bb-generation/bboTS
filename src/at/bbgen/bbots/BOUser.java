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
 * Represents a Black Ops User.
 * 
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class BOUser implements Serializable, Cloneable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Initializes all local data
	 */
	public BOUser()
	{
		super();
	}
	
	/**
	 * Initialized the local data with the given values.
	 * @param id User ID. This will change if the map changes, user reconnects, ...
	 * @param name User name.
	 * @param guid Black Ops GUID
	 * @param ipAddress User IP Address
	 * @param team User team
	 * @param score User Score
	 * @param ping User Ping
	 */
	public BOUser(int id, String name, int guid, String ipAddress, int team,
			int score, int ping)
	{
		this.id = id;
		this.name = name;
		this.guid = guid;
		this.ipAddress = ipAddress;
		this.team = team;
		this.score = score;
		this.ping = ping;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		BOUser newUser = (BOUser) super.clone();
		newUser.id = id;
		newUser.name = name;
		newUser.guid = guid;
		newUser.ipAddress = ipAddress;
		newUser.team = team;
		newUser.score = score;
		newUser.ping = ping;
		return newUser;
	}
	
	
	public int getId()
	{
		return id;
	}
	public String getName()
	{
		return name;
	}
	public int getGuid()
	{
		return guid;
	}
	public String getIpAddress()
	{
		return ipAddress;
	}
	public int getTeam()
	{
		return team;
	}
	public int getScore()
	{
		return score;
	}
	public int getPing()
	{
		return ping;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setGuid(int guid)
	{
		this.guid = guid;
	}

	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	public void setTeam(int team)
	{
		this.team = team;
	}

	public void setScore(int score)
	{
		this.score = score;
	}

	public void setPing(int ping)
	{
		this.ping = ping;
	}
	
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("id="+id+" ");
		sb.append("name="+name+" ");
		sb.append("guid="+guid+" ");
		sb.append("ipAddress="+ipAddress+" ");
		sb.append("team="+team+" ");
		sb.append("score="+score+" ");
		sb.append("ping="+ping);
		return sb.toString();
	}

	private int id;
	private String name;
	private int guid;
	private String ipAddress;
	private int team;
	private int score;
	private int ping;
}