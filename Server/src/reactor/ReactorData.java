package reactor;

import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;

public class ReactorData<T> {

	private ExecutorService executor;
	private Selector selector;
	private ServerProtocolFactory<T> protocolFactory;
	private TokenizerFactory<T> tokenizerFactory;
	
	public ReactorData(ExecutorService executor, Selector selector,
						ServerProtocolFactory<T> _protocolFactory,
						TokenizerFactory<T> _tokenizerFactory)
	{
		this.executor = executor;
		this.selector = selector;
		this.protocolFactory = _protocolFactory;
		this.tokenizerFactory = _tokenizerFactory;
	}
	
	public ExecutorService getExecutor()
	{
		return executor;
	}
	
	public Selector getSelector()
	{
		return selector;
	}
	
	public ServerProtocolFactory<T> getProtocolMaker()
	{
		return protocolFactory;
	}
	
	public TokenizerFactory<T> getTokenizerMaker()
	{
		return tokenizerFactory;
	}
}
