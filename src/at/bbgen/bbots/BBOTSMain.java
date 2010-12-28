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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Main class for bboTS.<br><br>
 * 
 * Command Line arguments: [config-file] [user-file]
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class BBOTSMain {

	/**
	 * Starts the bboTS service
	 * 
	 * @param args command Line Arguments. See {@link BBOTSMain}
	 */
	public static void main(String[] args)
	{
		SProperties sProperties = null;
		UserProperties userProperties = null;
		List<BOTeamSwitcher> bots = new LinkedList<BOTeamSwitcher>();
		TSConnection tsConnection = null;
		
		String configFileName = "config.properties";
		String userFileName = "users.properties";
		
		if(args.length > 0)
			configFileName = args[0];
		if(args.length > 1)
			userFileName = args[1];
		
		try
		{
			sProperties = new SProperties(configFileName);
			sProperties.parseProperties();
			userProperties = new UserProperties(userFileName);
			userProperties.parseUsers();
			
			tsConnection = new TSConnection(sProperties.getTsServerHost(), sProperties.getTsServerPort(), sProperties.getTsUsername(), sProperties.getTsPassword(), sProperties.getTsVServerID());
			tsConnection.connect();

			for(String server : sProperties.getServerNames())
			{
				BOTeamSwitcher bbots = new BOTeamSwitcher(sProperties, userProperties, tsConnection, server);
				bbots.init();
				bots.add(bbots);
			}
			
			System.out.println("bboTS is up and running...");
			
		} catch (BOTeamSwitcherException e)
		{
			System.out.println("Error while trying to init BBOTS System: "+e.getMessage());
			return;
		} catch (SPropertiesException e)
		{
			System.out.println("Error while trying to init Properties System: "+e.getMessage());
			return;
		} catch (UserPropertiesException e)
		{
			System.out.println("Error while trying to init User Config System: "+e.getMessage());
			return;
		} catch (TSConnectionException e)
		{
			System.out.println("Error while trying to init TS Connection: "+e.getMessage());
			return;
		}
		
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		
		try
		{
			stdin.readLine();
		} catch(IOException e) { }
		
		for(BOTeamSwitcher bbots : bots)
		{
			bbots.stopService();
		}
		
	}

}
