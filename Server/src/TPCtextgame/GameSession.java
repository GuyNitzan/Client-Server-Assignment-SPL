package TPCtextgame;

public class GameSession extends Thread
{
	protected GameRoom room=null;
	
	public GameSession(GameRoom owner)
	{
		room = owner;
	}
	
	public synchronized boolean processMessage(String nick, String orig_command, String msg)
	{
		return false;
	}
	
	public void run()
	{
		//do stuff
		room.stopGame(this);
	}
}