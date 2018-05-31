package TPCthreadperclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import TPCprotocol.AsyncServerProtocol;
import TPCprotocol.ProtocolCallback;

public class ConnectionHandler<T> implements ProtocolCallback<StringMessage>, Runnable {
	private BufferedReader in;
	private PrintWriter out;
	Socket clientSocket;
	AsyncServerProtocol<StringMessage> protocol;

	public ConnectionHandler(Socket acceptedSocket, ServerProtocolFactory factory) {
		in = null;
		out = null;
		clientSocket = acceptedSocket;
		protocol = factory.create(this);

		System.out.println("Accepted connection from client!");
		System.out.println("The client is from: " + acceptedSocket.getInetAddress() + ":" + acceptedSocket.getPort());
	}

	public void run() {
		String msg;

		try {
			initialize();
		} catch (IOException e) {
			System.out.println("Error in initializing I/O");
		}

		try {
			process();
		} catch (IOException e) {
			System.out.println("Error in I/O");
		}

		System.out.println("Connection closed - bye bye...");
		close();

	}

	public void process() throws IOException {
		String msg;

		//blocking readline, but we don't give a fuck cause it has it own thread.
		while ((msg = in.readLine()) != null) {
			System.out.println("Received \"" + msg + "\" from client");

			protocol.processMessage(new StringMessage(msg), this);
			if (protocol.isEnd(new StringMessage(msg)))
				break;
		}
	}

	// Starts listening
	public void initialize() throws IOException {
		// Initialize I/O
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
		out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
		System.out.println("I/O initialized");
	}

	// Closes the connection
	public void close() {
		try {
			if (in != null) {
				in.close();
			}

			if (out != null) {
				out.close();
			}

			clientSocket.close();
		} catch (IOException e) {
			System.out.println("Exception in closing I/O");
		}
	}

	@Override
	public void sendMessage(StringMessage msg) throws IOException {
		out.println(msg.getMessage());
		out.flush();
	}
}