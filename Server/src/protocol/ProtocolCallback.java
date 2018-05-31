package protocol;

public interface ProtocolCallback <T> {
	/**
	* An interface that bridges between the protocol and the server.
	* The server should implement this interface, and pass an instance of it along with any message.
	* The instance passed should be unique to each TCP connection.
	* It therefore allows the protocol to identify the sending user between
	* messages, and reply at any point in time.
	* *
	@param <T> type of message that the protocol handles .
	*/
	void sendMessage (T msg) throws java.io.IOException;
}
