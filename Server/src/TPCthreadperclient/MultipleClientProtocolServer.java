package TPCthreadperclient;

import java.io.*;
import java.net.*;

import TPCprotocol.AsyncServerProtocol;
import TPCtextgame.TextGameProtocol;

interface ServerProtocolFactory<T> {
	AsyncServerProtocol<T> create(ConnectionHandler<T> _handler);
}

class MultipleClientProtocolServer implements Runnable {
	private ServerSocket serverSocket;
	private int listenPort;
	private ServerProtocolFactory<StringMessage> factory;

	public MultipleClientProtocolServer(int port, ServerProtocolFactory<StringMessage> p) {
		serverSocket = null;
		listenPort = port;
		factory = p;
	}

	public void run() {
		try {
			serverSocket = new ServerSocket(listenPort);
			System.out.println("Listening...");
		} catch (IOException e) {
			System.out.println("Cannot listen on port " + listenPort);
		}

		while (true) {
			try {
				ConnectionHandler<StringMessage> newConnection = new ConnectionHandler<StringMessage>(
						serverSocket.accept(), factory);
				//Thread Per Client!!
				new Thread(newConnection).start();
			} catch (IOException e) {
				System.out.println("Failed to accept on port " + listenPort);
			}
		}
	}

	// Closes the connection
	public void close() throws IOException {
		serverSocket.close();
	}

	public static void main(String[] args) throws IOException {
		int port = 8080;

		if (args.length > 0) {
			// Get port
			port = Integer.decode(args[0]).intValue();
		} else
			System.out.println("Warning: You should define the port to listen to. Using default(8080)");

		ServerProtocolFactory<StringMessage> protocolMaker = new ServerProtocolFactory<StringMessage>() {
			@SuppressWarnings("rawtypes")
			@Override
			public AsyncServerProtocol<StringMessage> create(ConnectionHandler _handler) {
				return new TextGameProtocol(_handler);
			}
		};

		MultipleClientProtocolServer server = new MultipleClientProtocolServer(port, protocolMaker);
		Thread serverThread = new Thread(server);
		serverThread.start();

		try {
			serverThread.join();
		} catch (InterruptedException e) {
			System.out.println("Server stopped");
		}
	}
}
