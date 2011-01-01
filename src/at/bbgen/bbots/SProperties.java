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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * SProperties handles the general configuration.<br><br>
 * It uses a java .properties file to read the configuration for the service.<br>
 * This configuration includes many values, such as the Teamspeak server data
 * and so on.
 * 
 * It is also possible to set multiple black ops servers, as seen in the example below.
 * 
 * <h2>Example</h2>
 * 
 * <pre>{@code
 * # every black ops server to be watched must be labelled by the service.
 * # this label can be choosen by yourself.
 * # The following example shows the configuration for 2 servers:
 * # One called Ranked and one called Unranked
 * BOServerNames = Ranked Unranked
 * 
 * ## Teamspeak Configuration ##
 * # One service instance can only be connected to exactly one ts3 server.
 * # The following arguments give details on how to connect.
 * TSServerHost = 127.0.0.1
 * # The following describes the Serverquery Port
 * # This property is NOT the Port which you will enter in your TS3 client.
 * TSServerPort = 10011
 * # The next one is the ts3 VServer ID to be used. If you're not sure, try
 * # using the "serverlist" query command.
 * TSVServerID = 1
 * 
 * # the next two properties describe the data for the ServerQuery Account
 * # If you don't want to use the serveradmin account, you can also create
 * # another one. But this is rather difficult and will not be explained here.
 * TSUsername = serveradmin
 * TSPassword = MySecretP4ssword
 * 
 * # The service will scan every X milliseconds if something has changed
 * TSScanningInterval = 15000
 * 
 * 
 * ### Black Ops Servers ###
 * ## Ranked ##
 * # This service instance will listen for users on the descibed channels.
 * # Warning: Every TeamX channel has also to be on this list!
 * Ranked.ListeningChannels = 10 11 12 13
 * # The following describe where to switch the users, if they are in Team X
 * # Team0 represents users which are currently connecting or all users during
 * # the pre-match time.
 * Ranked.Team0 = 11
 * Ranked.Team1 = 12
 * Ranked.Team2 = 13
 * 
 * # The following describe the login data for the black ops server.
 * # These are the same as you may have entered in your Black Ops RCON tool
 * Ranked.BOServerHost = 10.11.12.13
 * Ranked.BOServerPort = 1330
 * Ranked.BOServerPassword = MyBOP4ssword
 * 
 * # The following property describe how often (milliseconds) the black ops
 * # server is queried for the user list.
 * Ranked.BOScanningInterval = 15000
 * 
 * # The next property sets how many recognized players have to play on a server to get switched
 * Ranked.MinimumSwitchingPlayers = 3
 * 
 * ## Unranked ##
 * Unranked.ListeningChannels = 20 21 22 23
 * # If you don't set the Team0 property, users who are currently connecting
 * # or users during the pre-match time, will not be switched.
 * Unranked.Team1  = 22
 * Unranked.Team2  = 23
 * 
 * Unranked.BOServerHost     = 30.31.32.33
 * Unranked.BOServerPort     = 5453
 * Unranked.BOServerPassword = AnotherP4ssword
 * 
 * Unranked.BOScanningInterval = 25000
 * Unranked.MinimumSwitchingPlayers = 3
 * 
 * }</pre>
 * 
 * @author Bernhard Eder <bbots@bbgen.net>
 *
 */
public class SProperties
{
	/**
	 * Initializes all local data. Does not parse the properties file.
	 * @param filename filename for the java .properties configuration file.
	 */
	public SProperties(String filename)
	{
		this.filename = filename;
		listeningChannels = new HashMap<String, List<Integer>>();
		teamChannels = new HashMap<String, ArrayList<Integer>>();
		boServerHost = new HashMap<String, String>();
		boServerPort = new HashMap<String, Integer>();
		boServerPassword = new HashMap<String, String>();
		boMinimumPlayers = new HashMap<String, Integer>();
		tsServerHost = null;
		tsServerPort = -1;
		tsVServerID = -1;
		tsUsername = null;
		tsPassword = null;
		tsScanInterval = -1;
		boScanInterval = new HashMap<String, Integer>();
		serverNames = new LinkedList<String>();
	}

	/**
	 * Parses the java .properties file as given by the constructor.
	 * @throws SPropertiesException Thrown if anything goes wrong or illegal data is inputted.
	 */
	public void parseProperties() throws SPropertiesException
	{
		InputStream is;
		try
		{
			is = new FileInputStream(filename);
		} catch (FileNotFoundException e1)
		{
			throw new SPropertiesException("Properties file not found ("+filename+").");
		}

		
		Properties props = new java.util.Properties();
		
		try
		{
			props.load(is);
			
			String serverNames = props.getProperty("BOServerNames");
			if(serverNames == null)
				throw new SPropertiesException(getMandatoryExceptionString("BOServerNames"));
			
			String[] servers = serverNames.split(" ");
			
			for(String server : servers)
			{
				this.serverNames.add(server);
				
				/***** ListeningChannels *****/
				String lChannels = props.getProperty(server+".ListeningChannels");
				if(lChannels != null)
				{
					String[] listChannels = lChannels.split(" ");
					if(listChannels.length > 0)
					{
						LinkedList<Integer> lchans = new LinkedList<Integer>(); 
						for(String channel : listChannels)
						{
							int iChannel = -1;
							try
							{
								iChannel = Integer.parseInt(channel);
							} catch (NumberFormatException e)
							{
								throw new SPropertiesException("Setting '"+server+".ListeningChannels' contains a non numeric channel ID: "+e.getMessage());
							}
							lchans.add(iChannel);
						}
						listeningChannels.put(server, lchans);
					}
					else
						throw new SPropertiesException("Setting '"+server+".ListeningChannels' has to contain at least one channel.");
				}
				else
					throw new SPropertiesException(getMandatoryExceptionString(server+".ListeningChannels"));
				
				
				
				/***** Team[0,1,2] *****/
				// iterate through team channel list
				ArrayList<Integer> teamChans = new ArrayList<Integer>();
				for(int i=0;i<3;++i)
					teamChans.add(-1);
				for(int i=0;i<3;++i)
				{
					String tChannel = props.getProperty(server+".Team"+i);
					if(tChannel != null)
					{
						int iChannel = -1;
						try
						{
							iChannel = Integer.parseInt(tChannel);
						} catch (NumberFormatException e)
						{
							throw new SPropertiesException("Setting '"+server+".Team"+i+"' contains a non numeric channel ID: "+e.getMessage());
						}
						teamChans.set(i, iChannel);
					}
					else if(i > 0) // only Team1 and Team2 properties are mandatory
						throw new SPropertiesException(getMandatoryExceptionString(server+".Team"+i));
				}
				teamChannels.put(server, teamChans);
				

				/***** BOServerHost *****/
				String sboServerHost = props.getProperty(server+".BOServerHost");
				if(sboServerHost != null)
				{
					boServerHost.put(server, sboServerHost);
				}
				else
					throw new SPropertiesException(getMandatoryExceptionString(server+".BOServerHost"));
				
				

				/***** BOServerPort *****/
				String sboServerPort = props.getProperty(server+".BOServerPort");
				if(sboServerPort != null)
				{
					int iboServerPort = -1;
					try
					{
						iboServerPort = Integer.parseInt(sboServerPort);
					} catch (NumberFormatException e)
					{
						throw new SPropertiesException("Setting '"+server+".BOServerPort' contains a non numeric channel ID: "+e.getMessage());
					}
					boServerPort.put(server, iboServerPort);
				}
				else
					throw new SPropertiesException(getMandatoryExceptionString(server+".BOServerPort"));
				
				

				/***** BOServerPassword *****/
				String sboServerPassword = props.getProperty(server+".BOServerPassword");
				if(sboServerPassword != null)
				{
					boServerPassword.put(server, sboServerPassword);
				}
				else
					throw new SPropertiesException(getMandatoryExceptionString(server+".BOServerPassword"));
				
				

				/***** BOScanningInterval *****/
				String sscanboInterval = props.getProperty(server+".BOScanningInterval");
				if(sscanboInterval != null)
				{
					int iscanboInterval = -1;
					try
					{
						iscanboInterval = Integer.parseInt(sscanboInterval);
					} catch (NumberFormatException e)
					{
						throw new SPropertiesException("Setting '"+server+".BOScanningInterval' contains a non numeric channel ID: "+e.getMessage());
					}
					boScanInterval.put(server, iscanboInterval);
				}
				else
					throw new SPropertiesException(getMandatoryExceptionString(server+".BOScanningInterval"));
				
				
				/***** MinimumSwitchingPlayers *****/
				String sMinPlr = props.getProperty(server+".MinimumSwitchingPlayers");
				if(sMinPlr != null)
				{
					int iminPlr = -1;
					try
					{
						iminPlr = Integer.parseInt(sMinPlr);
					} catch (NumberFormatException e)
					{
						throw new SPropertiesException("Setting '"+server+".MinimumSwitchingPlayers' contains a non numeric channel ID: "+e.getMessage());
					}
					boMinimumPlayers.put(server, iminPlr);
				}
				else
					throw new SPropertiesException(getMandatoryExceptionString(server+".MinimumSwitchingPlayers"));
				
			}

			

			/***** TSServerHost *****/
			String stsServerHost = props.getProperty("TSServerHost");
			if(stsServerHost != null)
			{
				tsServerHost = stsServerHost;
			}
			else
				throw new SPropertiesException(getMandatoryExceptionString("TSServerHost"));
			
			
			/***** TSServerPort *****/
			String stsServerPort = props.getProperty("TSServerPort");
			if(stsServerPort != null)
			{
				int itsServerPort = -1;
				try
				{
					itsServerPort = Integer.parseInt(stsServerPort);
				} catch (NumberFormatException e)
				{
					throw new SPropertiesException("Setting 'TSServerPort' contains a non numeric channel ID: "+e.getMessage());
				}
				tsServerPort = itsServerPort;
			}
			else
				throw new SPropertiesException(getMandatoryExceptionString("TSServerPort"));
			
			
			/***** TSVServerID *****/
			String stsVServerID = props.getProperty("TSVServerID");
			if(stsVServerID != null)
			{
				int itsVServerID = -1;
				try
				{
					itsVServerID = Integer.parseInt(stsVServerID);
				} catch (NumberFormatException e)
				{
					throw new SPropertiesException("Setting 'TSVServerID' contains a non numeric channel ID: "+e.getMessage());
				}
				tsVServerID = itsVServerID;
			}
			else
				throw new SPropertiesException(getMandatoryExceptionString("TSVServerID"));
			
			
			/***** TSUsername *****/
			String stsUsername = props.getProperty("TSUsername");
			if(stsUsername != null)
			{
				tsUsername = stsUsername;
			}
			else
				throw new SPropertiesException(getMandatoryExceptionString("TSUsername"));
			
			
			/***** TSPassword *****/
			String stsPassword = props.getProperty("TSPassword");
			if(stsPassword != null)
			{
				tsPassword = stsPassword;
			}
			else
				throw new SPropertiesException(getMandatoryExceptionString("TSPassword"));
			
			
			/***** TSScanningInterval *****/
			String sscanInterval = props.getProperty("TSScanningInterval");
			if(sscanInterval != null)
			{
				int iscanInterval = -1;
				try
				{
					iscanInterval = Integer.parseInt(sscanInterval);
				} catch (NumberFormatException e)
				{
					throw new SPropertiesException("Setting 'TSScanningInterval' contains a non numeric channel ID: "+e.getMessage());
				}
				tsScanInterval = iscanInterval;
			}
			else
				throw new SPropertiesException(getMandatoryExceptionString("TSScanningInterval"));
			
			
	  } catch (IOException e)
		{
			throw new SPropertiesException("Error while parsing config file: "+e.getMessage());
		} finally
	  {
	  	try { is.close(); } catch(IOException e) { }
	  }
	}
	
	/**
	 * message when property is mandatory but not found
	 * @param property Property name
	 * @return String which represents an unfound mandatory property
	 */
	private String getMandatoryExceptionString(String property)
	{
		return "Mandatory Setting '"+property+"' not found in properties file("+filename+").";
	}
	
	/**
	 * Gets the ScanningInterval for the Black Ops server <i>server</i>
	 * @param server Label of the bo server
	 * @return ScanningInterval for <i>server</i>
	 */
	public int getBoScanInterval(String server)
	{
		Integer iscan = boScanInterval.get(server);
		if(iscan != null)
			return iscan;
		else
			return 15000; //default value
	}
	
	/**
	 * Gets the ScanningInterval for the Teamspeak3 server
	 * @return ScanningInterval for the TS server
	 */
	public int getTsScanInterval()
	{
		return tsScanInterval;
	}

	/**
	 * Gets the Username of the ServerQuery Account
	 * @return TS username
	 */
	public String getTsUsername()
	{
		return tsUsername;
	}

	/**
	 * Gets the Password of the ServerQuery Account
	 * @return TS password
	 */
	public String getTsPassword()
	{
		return tsPassword;
	}

	/**
	 * Gets the TS VServer ID of the Teamspeak Server.
	 * This can be obtained by using the <i>serverlist</i> ServerQuery command.
	 * @return TS VServer ID
	 */
	public int getTsVServerID()
	{
		return tsVServerID;
	}

	/**
	 * Gets the Port of the Teamspeak ServerQuery
	 * @return TS ServerQuery Port
	 */
	public int getTsServerPort()
	{
		return tsServerPort;
	}

	/**
	 * Gets the Host of the Teamspeak ServerQuery
	 * @return TS Serverquery Host
	 */
	public String getTsServerHost()
	{
		return tsServerHost;
	}

	/**
	 * Gets the Password of the Black Ops server <i>server</i>
	 * @param server Label of the Black Ops server
	 * @return Server Password of the Black Ops Server
	 */
	public String getBoServerPassword(String server)
	{
		String bop = boServerPassword.get(server);
		if(bop != null)
			return bop;
		else
			return "INVALID_PASSWORD";
	}

	/**
	 * Gets the Port of the Black Ops server <i>server</i>
	 * @param server Label of the Black Ops Server
	 * @return Server Port of the Black Ops Server
	 */
	public int getBoServerPort(String server)
	{
		Integer bosp = boServerPort.get(server);
		if(bosp != null)
			return bosp;
		else
			return -1; //INVALID PORT
	}

	/**
	 * Gets the Host of the Black Ops server <i>server</i>
	 * @param server Label of the Black Ops Server
	 * @return Host of the Black Ops Server
	 */
	public String getBoServerHost(String server)
	{
		String bosh = boServerHost.get(server);
		if(bosh != null)
			return bosh;
		else
			return "INVALID_HOST";
	}

	/**
	 * Returns a list of all listening channels for the Black Ops Server <i>server</i>
	 * @param server Label of the Black Ops Server
	 * @return List of all listening Channels
	 */
	public List<Integer> getListeningChannels(String server)
	{
		List<Integer> lchans = listeningChannels.get(server);
		if(lchans != null)
			return lchans;
		else
			return new ArrayList<Integer>(); // empty listening channels
	}
	
	/**
	 * Checks if a given channel ID is a listening channel for <i>server</i>
	 * @param server Label of the Black Ops Server
	 * @param channel Teamspeak channel ID
	 * @return True if <i>channel</i> is a listening channel for <i>server</i>, otherwise false.
	 */
	public boolean isListeningChannel(String server, int channel)
	{
		List<Integer> lchans = listeningChannels.get(server);
		if(lchans != null)
			return lchans.contains(channel);
		else
			return false;
	}

	/**
	 * Gets the channel ID for the Team identified by <i>team</i>
	 * @param server Label of the Black Ops Server
	 * @param team Team ID. Must be 0 (connecting, pre-match), 1 (Team 1) or 2 (Team 2) 
	 * @return the Teamspeak channel ID of the given team or -1 if not set.
	 * @throws SPropertiesException Will be thrown if <i>team</i> is greater than 2
	 */
	public int getTeamChannels(String server, int team) throws SPropertiesException
	{
		if(team > 2)
		{
			throw new SPropertiesException("Invalid channel ID: "+team);
		}
		ArrayList<Integer> tchans = teamChannels.get(server);
		if(tchans != null)
			return tchans.get(team);
		else
			return -1; //INVALID team channel
	}
	
	/**
	 * Gets the minimum recognized players to play on a server to enable switching
	 * 
	 * @param server Label of the Black Ops Server
	 * @return Minimum player count to enable switching
	 */
	public int getMinimumPlayers(String server)
	{
		Integer minPlr = boMinimumPlayers.get(server);
		if(minPlr == null)
			return 3;
		else
			return minPlr;
	}
	
	/**
	 * Returns a list of all Black Ops Server Labels
	 * @return list of all Black Ops server labels
	 */
	public List<String> getServerNames()
	{
		return serverNames;
	}

	private List<String> serverNames;
	private Map<String, Integer> boScanInterval;
	private int tsScanInterval;
	private String tsUsername;
	private String tsPassword;
	private int tsVServerID;
	private int tsServerPort;
	private String tsServerHost;
	private Map<String, String> boServerPassword;
	private Map<String, Integer> boServerPort;
	private Map<String, String> boServerHost;
	private Map<String, List<Integer>> listeningChannels;
	private Map<String, ArrayList<Integer>> teamChannels;
	private Map<String, Integer> boMinimumPlayers;
	private String filename;
}
