package TPCtextgame;

import java.io.IOException;
import java.util.ArrayList;

import TPCbluffer.BlufferSession;
import TPCprotocol.ProtocolCallback;
import TPCthreadperclient.StringMessage;

public class GameRoom
{
	public class RoomClient
	{
		public ProtocolCallback<StringMessage> io_handle;
		public String nick;
	};
	
	private boolean sessionIsPlaying;
	private String  roomName;
	private GameSession currentSession = null;
	private ArrayList<RoomClient> clients = null;
	
	public GameRoom(String name)
	{
		roomName = name;
	}
	
	public String getGameName()
	{
		return "Unnamed";
	}
	
	public boolean isPlaying()
	{
		return sessionIsPlaying;
	}
	
	public GameSession getCurrentGame()
	{
		return currentSession;
	}
	
	public boolean startGame(String gameName)
	{
		if (gameName.equals("BLUFFER"))
		{
			if (currentSession == null)
			{
				currentSession = new BlufferSession(this);
				currentSession.start();
				
				sessionIsPlaying = true;
				
				return true;
			}
			
			return false;
		}
		
		return false;
	}
	
	public void stopGame(GameSession caller)
	{
		if (caller == currentSession)
			currentSession = null;
		
		sessionIsPlaying = false;
	}
	
	public String getName()
	{
		return roomName;
	}
	
	public RoomClient getNamedClient(String nick)
	{
		if (clients == null) return null;
		
		for (RoomClient client: clients)
		{
			if (client.nick.equalsIgnoreCase(nick))
				return client;
		}
		
		return null;
	}
	
	public boolean addClient(ProtocolCallback client, String nick)
	{
		RoomClient rc = getNamedClient(nick);
		
		if (rc != null && rc.io_handle != client) return false;
		
		RoomClient rc2 = new RoomClient();
		rc2.io_handle = client;
		rc2.nick = nick;
		
		if (clients == null) clients = new ArrayList<RoomClient>();
		clients.add(rc2);
		return true;
	}
	
	public boolean removeClient(ProtocolCallback client, String nick)
	{
		RoomClient rc = getNamedClient(nick);
		
		if (rc == null) return false;
		if (rc.io_handle != client) return false;
		
		clients.remove(rc);
		rc = null;
		return true;
	}
	
	public boolean renameClient(ProtocolCallback client, String old_nick, String new_nick)
	{
		RoomClient rc = getNamedClient(old_nick);
		
		if (rc == null) return false;
		if (rc.io_handle != client) return false;
		
		rc.nick = new_nick;
		return true;
	}
	
	public ArrayList<ProtocolCallback<StringMessage>> getClientHandles()
	{
		if (clients == null) return null;
		
		ArrayList<ProtocolCallback<StringMessage>> handles = new ArrayList<ProtocolCallback<StringMessage>>();
		for (RoomClient rc : clients)
			handles.add(rc.io_handle);
		
		return handles;
	}
	
	public ArrayList<RoomClient> getRoomClients()
	{
		return clients;
	}

	public int getActiveClientCount()
	{
		if (clients == null)
			return 0;
		
		return clients.size();
	}
	
	public void broadcastMessage(String src, String msg)
	{
		if (clients == null) return;
		
		for (RoomClient client: clients)
		{
			if (client.nick.equals(src)) continue;
			
			try
			{
				client.io_handle.sendMessage(new StringMessage("USRMSG [" + src + "] " + msg));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}