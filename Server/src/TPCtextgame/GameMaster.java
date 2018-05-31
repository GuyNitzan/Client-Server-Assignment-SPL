package TPCtextgame;

import java.util.ArrayList;

import TPCprotocol.ProtocolCallback;

public class GameMaster
{
	private static ArrayList<GameRoom> rooms = null;
	
	public static void initialize()
	{
		rooms = new ArrayList<GameRoom>();
	}
	
	public static ArrayList<GameRoom> getRooms()
	{
		return rooms;
	}
	
	public static boolean addClientToRoom(ProtocolCallback client, String client_nick, String room_name)
	{
		createRoom(room_name);
		
		GameRoom room = findRoom(room_name);
		if (room == null)
		{
			System.err.println("Error: Could not find room after createRoom() called!");
			return false;
		}
		
		return room.addClient(client, client_nick);
	}
	
	public static boolean removeClientFromRoom(ProtocolCallback client, String client_nick, String room_name)
	{
		GameRoom room = findRoom(room_name);
		if (room == null)
		{
			return false;
		}
		
		return room.removeClient(client, client_nick);
	}
	
	public static boolean renameClient(ProtocolCallback client, String old_nick, String new_nick, String room_name)
	{
		GameRoom room = findRoom(room_name);
		if (room == null)
		{
			return false;
		}
		
		return room.renameClient(client, old_nick, new_nick);
	}
	
	public static GameRoom findRoom(String roomName)
	{
		if (rooms == null) return null;
		
		for (GameRoom room: rooms)
		{
			if (room.getName().equals(roomName))
				return room;
		}
		
		return null;
	}
	
	public static void createRoom(String roomName)
	{
		if (findRoom(roomName) != null)
			return;
		
		if (rooms == null)
			rooms = new ArrayList<GameRoom>();
		
		GameRoom room = new GameRoom(roomName);
		rooms.add(room);
	}
	
	public static String getSupportedGames()
	{
		return "BLUFFER";
	}
}