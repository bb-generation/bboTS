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
 * Represents a properties user.<br>
 * This is used to allocate a Black Ops user (identified by their GUID)<br>
 * to a Teamspeak3 User (identified by their Teamspeak3 UID).<br>
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class PUser implements Serializable, Cloneable
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Initialized all local data
	 * @param tsUID Teamspeak UID
	 * @param boGUID Black Ops GUID
	 */
	public PUser(String tsUID, int boGUID)
	{
		this.tsUID = tsUID;
		this.boGUID = boGUID;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		PUser pUser = (PUser)super.clone();
		pUser.tsUID = tsUID;
		pUser.boGUID = boGUID;
		return pUser;
	}
	
	/**
	 * Returns the Teamspeak UID
	 * @return teamspeak UID
	 */
	public String getTsUID()
	{
		return tsUID;
	}
	
	/**
	 * Sets the Teamspeak UID
	 * @param tsUID teamspeak UID
	 */
	public void setTsUID(String tsUID)
	{
		this.tsUID = tsUID;
	}
	
	/**
	 * Returns the Black Ops GUID
	 * @return Black Ops GUID
	 */
	public int getBoGUID()
	{
		return boGUID;
	}
	
	/**
	 * Sets the Black Ops GUID
	 * @param boGUID Black Ops GUID
	 */
	public void setBoGUID(int boGUID)
	{
		this.boGUID = boGUID;
	}

	private String tsUID;
	private int boGUID;
}