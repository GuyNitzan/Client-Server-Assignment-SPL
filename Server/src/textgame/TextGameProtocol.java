package textgame;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import protocol.AsyncServerProtocol;
import protocol.ProtocolCallback;
import reactor.ConnectionHandler;
import reactor.StringMessage;

public class TextGameProtocol implements AsyncServerProtocol<StringMessage> {
	private String playerName = null;
	private String currentRoom = null;
	private ConnectionHandler _handler = null;
	private boolean closing = false;

	public TextGameProtocol(ConnectionHandler handler) {
		_handler = handler;
	}

	public String getRoom() {
		return currentRoom;
	}

	private void sendErr(String command, String reason, ProtocolCallback<StringMessage> callback) {
		try {
			String err = "SYSMSG " + command + " FAILED";
			if (reason != null)
				err += " [" + reason + "]";

			callback.sendMessage(new StringMessage(err));
		} catch (java.io.IOException ex) {
			System.out.println("Sending error message failed");
			ex.printStackTrace();
		}
	}

	private void sendErr(String command, ProtocolCallback<StringMessage> callback) {
		sendErr(command, null, callback);
	}

	private void sendAck(String command, String msg, ProtocolCallback<StringMessage> callback) {
		try {
			String ack = "SYSMSG " + command + " ACCEPTED";
			if (msg != null)
				ack += "[" + msg + "]";

			callback.sendMessage(new StringMessage(ack));
		} catch (java.io.IOException ex) {
			System.out.println("Sending acknowledgement failed");
			ex.printStackTrace();
		}
	}

	private void sendAck(String command, ProtocolCallback<StringMessage> callback) {
		sendAck(command, null, callback);
	}

	@Override
	public void processMessage(StringMessage msg, ProtocolCallback<StringMessage> callback) {
		if (closing)
			return;

		// TODO:
		// 1. Process the message.
		// 2. Send back a response if needed.

		// Only process per-user vars. Game session responses are pushed to the session to manage
		String[] args = msg.getMessage().split(" ");
		args[0] = args[0].toUpperCase();

		// Rebuild response without first argument in case we have a sentence
		String paramsOnly = "";
		for (int i = 1; i < args.length; ++i) {
			paramsOnly += args[i];
			if ((i + 1) < args.length)
				paramsOnly += " ";
		}

		if (args[0].equals("NICK")) {
			if (args.length != 2) {
				sendErr(args[0], "Invalid number of arguments", callback);
				return;
			}

			//in case the client will change his nickname:
			if (currentRoom != null) {
				GameRoom room = GameMaster.findRoom(currentRoom);
				if (room == null) {
					System.err.println("Bogus room: " + currentRoom);
					currentRoom = null;
				} else {
					if (room.isPlaying()) {
						sendErr(args[0], "Cant do that while game is in progress", callback);
						return;
					}

					room.renameClient(callback, playerName, args[1]);
					playerName = args[1];
					return;
				}
			}

			playerName = args[1];
			sendAck(args[0], callback);

			return;
		}

		if (args[0].equals("JOIN")) {
			if (args.length != 2) {
				sendErr(args[0], "Invalid number of arguments", callback);
				return;
			}

			if (playerName == null) {
				sendErr(args[0], "You did not specify a nickname", callback);
				return;
			}

			//return true only if the client was added to the room
			if (GameMaster.addClientToRoom(callback, playerName, args[1])) {
				currentRoom = args[1];
				sendAck(args[0], callback);
			} else
				sendErr(args[0], callback);
			return;
		}

		if (args[0].equals("STARTGAME")) {
			if (args.length != 2) {
				sendErr(args[0], "Invalid number of arguments", callback);
				return;
			}

			if (currentRoom == null) {
				sendErr(args[0], "You need to join a room to start a game!", callback);
				return;
			}

			//return the room according to the name.
			GameRoom room = GameMaster.findRoom(currentRoom);
			if (room == null) {
				sendErr(args[0], "Error finding current room. Join another room and retry.", callback);
				return;
			}

			if (room.isPlaying()) {
				sendErr(args[0], "A game is already in session in the current room", callback);
				return;
			}

			if (!room.startGame(args[1])) {
				sendErr(args[0], callback);
				return;
			}

			sendAck(args[0], callback);
			return;
		}

		if (args[0].equals("LISTGAMES")) {
			sendAck(args[0], GameMaster.getSupportedGames(), callback);
			return;
		}

		//paramsOnly = the string after "MSG"
		if (args[0].equals("MSG")) {
			if (currentRoom == null)
				sendErr(args[0], "You have not joined a room!", callback);
			else {
				GameRoom myRoom = GameMaster.findRoom(currentRoom);
				if (myRoom != null) {
					myRoom.broadcastMessage(playerName, paramsOnly);
					sendAck(args[0], callback);
				} else
					sendErr(args[0], callback);
			}

			return;
		}

		if (args[0].equals("QUIT")) {
			if (closing)
				return;

			closing = true;
			if (currentRoom != null) {
				GameRoom room = GameMaster.findRoom(currentRoom);
				if (room != null) {
					room.removeClient(callback, playerName);
					currentRoom = null;
					playerName = null;
				}
			}

			sendAck(args[0], callback);
			return;
		}

		/* For other commands, let the current game deal with it. */

		Boolean failed = true;
		Boolean rejected = false;
		GameRoom myRoom = null;

		if (currentRoom != null)
			myRoom = GameMaster.findRoom(currentRoom);
		if (myRoom != null && myRoom.isPlaying()) {
			GameSession game = myRoom.getCurrentGame();
			if (game.processMessage(playerName, args[0], paramsOnly)) {
				sendAck(args[0], callback);
				failed = false;
			} else
				rejected = true;
		}

		if (failed) {
			if (!rejected)
				sendErr(args[0], "Unknown command", callback);
			else
				sendErr(args[0], "Your command was rejected", callback);

			System.out.println(args[0] + " command received. No known path. [" + msg.getMessage() + "]");
		}
	}

	@Override
	public boolean isEnd(StringMessage msg) {
		// TODO:
		// 1. Check if termination signal is received.
		// 2. If (1) return true.
		if (msg.getMessage().equals("QUIT")) {
			closing = true;

			try {
				ByteBuffer end = Charset.forName("UTF-8").newEncoder().encode(CharBuffer.wrap("SYSMSG QUIT ACCEPTED"));
				_handler.addOutData(end);
				_handler.write();
			} catch (Exception ex) {
				System.out.println("Client is closing, but an exception occured. Dropped?");
			}
		}
		return closing;
	}

	@Override
	public boolean shouldClose() {
		return closing;
	}

	@Override
	public void connectionTerminated() {
		if (closing)
			return;

		closing = true;
		if (currentRoom != null) {
			GameRoom room = GameMaster.findRoom(currentRoom);

			if (room != null)
				room.removeClient(_handler, playerName);

			currentRoom = null;
			playerName = null;
		}
	}
}
