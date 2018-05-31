package TPCthreadperclient;

/* StringMessage stub */
public class StringMessage {
	String __the_msg = "";

	public StringMessage(String msg) {
		__the_msg = msg;
	}

	public String getMessage() {
		return __the_msg;
	}

	@Override
	public String toString() {
		return __the_msg;
	}
}
