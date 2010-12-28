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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * UserProperties manages the user detailed information.<br><br>
 * This class parses a java .properties file which contains the allocation
 * for the users: Black Ops GUID -> Teamspeak UID<br>
 * It is not possible to allocate a black ops user multiple teamspeak identities!
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * # User A
 * 12345678 = ABRE1T4Z5U6O7JorzthORJZ4345De=
 * # User B
 * 87654321 = ABxggfs466HO7JorzthO8RJZ4dbD1=
 * # User C
 * 1034 = dD6Gz77R3rz67rfgU7ger3ug85gD1=
 * }</pre>
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class UserProperties
{
	/**
	 * Initializes local data.
	 * It does not parse the .properties file!
	 * 
	 * @param filename The filename where the user .properties file can be found.
	 */
	public UserProperties(String filename)
	{
		this.filename = filename;
		synchronized(this)
		{
			users = new HashMap<String, PUser>();
		}
	}
	
	/**
	 * Parses the .properties file with the given filename provided by the constructor.
	 * 
	 * @throws UserPropertiesException Thrown when anything has gone wrong.
	 */
	public void parseUsers() throws UserPropertiesException
	{
		HashMap<String, PUser> newUsers = new HashMap<String, PUser>();
		InputStream is;
		try
		{
			is = new FileInputStream(filename);
		} catch (FileNotFoundException e1)
		{
			throw new UserPropertiesException("Properties file not found ("+filename+").");
		}

		
		Properties props = new java.util.Properties();
		
		try
		{
			props.load(is);
			
			Enumeration<?> e = props.propertyNames();
			while(e.hasMoreElements())
			{
				String sguid = (String)e.nextElement();
				int guid = -1;
				try
				{
					guid = Integer.parseInt(sguid);
				} catch (NumberFormatException ee)
				{
					throw new UserPropertiesException("GUID contains non numeric values: "+ee.getMessage());
				}
				
				String tsUid = (String)props.getProperty(sguid);
				newUsers.put(tsUid, new PUser(tsUid, guid));
			}
		} catch (IOException e)
		{
			throw new UserPropertiesException("Error while trying to parse Users: "+e.getMessage());
		}
		finally
		{
			try { is.close(); } catch(IOException e) { }
		}
		
		synchronized(this)
		{
			users = newUsers;
		}
	}
	
	/**
	 * Returns a {@link PUser} for the specified Teamspeak UID
	 * @param tsUID the teamspeak UID to search for
	 * @return {@link PUser} or null if user could not be found
	 */
	public PUser getUser(String tsUID)
	{
		PUser fUser, retUser;
		synchronized(this)
		{
			fUser = users.get(tsUID);
			if(fUser == null)
				return null;
			try
			{
				retUser = (PUser)fUser.clone();
			} catch (CloneNotSupportedException e) { return null;	}
		}
		
		return retUser;
	}
	
	private HashMap<String, PUser> users;
	private String filename;
}
