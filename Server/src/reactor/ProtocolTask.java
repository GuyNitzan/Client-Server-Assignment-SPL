package reactor;

import java.nio.ByteBuffer;
import protocol.*;
import tokenizer.*;

/**
 * This class supplies some data to the protocol, which then processes the data,
 * possibly returning a reply. This class is implemented as an executor task.
 * 
 */
public class ProtocolTask<T> implements Runnable {

	private final ServerProtocol<T> _protocol;
	private final MessageTokenizer<T> _tokenizer;

	/*
	 * Include the callback. The callback should be an instance of
	 * ConnectionHandler or at least have a handle to a unique ConnectionHandler
	 * so _handler is unused here.
	 */
	private final ProtocolCallback<T> _callback;

	public ProtocolTask(final ServerProtocol<T> protocol, final MessageTokenizer<T> tokenizer,
			final ProtocolCallback<T> callback)
	{
		this._protocol = protocol;
		this._tokenizer = tokenizer;
		this._callback = callback;
	}

	// we synchronize on ourselves, in case we are executed by several threads
	// from the thread pool.
	public synchronized void run()
	{
		// go over all complete messages and process them.
		while (_tokenizer.hasMessage())
		{
			T msg = _tokenizer.nextMessage();

			/* The callback shall handle sending messages and such */
			if (this._protocol.isEnd(msg)) break;
			this._protocol.processMessage(msg, _callback);
		}
	}

	public void addBytes(ByteBuffer b) {
		_tokenizer.addBytes(b);
	}
}