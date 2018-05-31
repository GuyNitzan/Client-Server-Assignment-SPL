package bluffer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import com.google.gson.Gson;

import protocol.ProtocolCallback;
import reactor.StringMessage;
import textgame.GameRoom;
import textgame.GameSession;
import textgame.GameRoom.RoomClient;

public class BlufferSession extends GameSession {
	public class QuestionAnswerPair {
		int id; /* Identifier */
		String questionText; /* Question */
		String realAnswer; /* Real Answer */
	}

	private class BluffedResponse {
		int id; /* Identifier */
		int ref_count; /* Ref count; those who chose this answer */
		String nick; /* Originator's nick-name */
		String bogusAnswer; /* Bogus answer proposed */

		ArrayList<String> selectors; /* Those who selected this answer */
	}

	static final int REF_INCR = 0;
	static final int REF_DECR = 1;

	static final int CHOICES_COUNT = 99; /* No limit to displayed choices */
	static final int POINTS_FOR_CORRECT_ANSWER = 10;
	static final int POINTS_FOR_SELECTED_BLUFF = 5;

	static final int SESSION_OFFLINE = 0;
	static final int SESSION_STARTING = 1;
	static final int SESSION_PRESENTING_QUESTION = 2;
	static final int SESSION_COLLECTING_BLUFFS = 3;
	static final int SESSION_PRESENTING_BLUFFS = 4;
	static final int SESSION_GATHERING_RESPONSES = 5;
	static final int SESSION_RANKING = 6;

	private int currentState = SESSION_OFFLINE;

	private ArrayList<QuestionAnswerPair> questionSet = null;
	private ArrayList<BluffedResponse> responseSet = null;

	/* Currently presented choices */
	private ArrayList<BluffedResponse> presentedChoices = null;
	/* Current question being presented */
	private QuestionAnswerPair currentQuestion = null;
	private int qrefs = 0;

	public BlufferSession(GameRoom owner) {
		super(owner);
	}

	private void loadQuestionSet() throws FileNotFoundException {
		// Load questions from JSON file...
		questionSet = new ArrayList<QuestionAnswerPair>();

		BufferedReader reader;

		reader = new BufferedReader(
				new FileReader("users/studs/bsc/2015/guynitz/Downloads/ReactorServer-Maven/bluffer.json"));
		Data data = new Gson().fromJson(reader, Data.class);

		for (int i = 0; i < data.questions.length; i++) {
			QuestionAnswerPair q = new QuestionAnswerPair();
			q.id = i;
			q.questionText = data.questions[i].questionText;
			q.realAnswer = data.questions[i].realAnswer;
			questionSet.add(i, q);
		}

		room.broadcastMessage("Room Admin", "The questions have been loaded successfuly");
	}

	private ArrayList<BluffedResponse> getSomeBluffsAndAnswer(QuestionAnswerPair realQuestion, int maxBlufSize) {
		if (responseSet == null)
			return null;

		Collections.shuffle(responseSet);

		ArrayList<BluffedResponse> tmp = new ArrayList<BluffedResponse>();
		int min_ct = (maxBlufSize < responseSet.size() ? maxBlufSize : responseSet.size());
		for (int i = 0; i < min_ct; ++i)
			tmp.add(responseSet.get(i));

		BluffedResponse real = new BluffedResponse();
		real.id = realQuestion.id;
		real.nick = null;
		real.ref_count = 0;
		real.bogusAnswer = realQuestion.realAnswer;
		real.selectors = null;

		tmp.add(real);
		Collections.shuffle(tmp);

		return tmp;
	}

	private synchronized void modifyQrefs(int num, int direction) {
		if (direction == REF_DECR) {
			if (qrefs < num) {
				System.err.println("Attempting to decrement refs by greater value!");
				return;
			}

			qrefs -= num;
		} else if (direction == REF_INCR) {
			qrefs += num;
		}
	}

	private void blockUntilAllAnswer(int max_ans) {
		while (qrefs > 0) {
			try {
				Thread.sleep(1000);

				/* Check for dropped clients */
				int active = room.getActiveClientCount();
				if (active < max_ans) {
					System.out.println(String.valueOf(max_ans - active) + " clients have dropped!");
					modifyQrefs(max_ans - active, REF_DECR);

					max_ans = active;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void rankCurrentQuestion(ArrayList<RoomClient> handles) {
		BluffedResponse realAnswer = null;
		for (BluffedResponse resp : presentedChoices) {
			if (resp.nick == null) {
				realAnswer = resp;
				break;
			}
		}

		if (realAnswer == null) {
			// Throw a fit
			for (RoomClient rc : handles) {
				try {
					rc.io_handle
							.sendMessage(new StringMessage("GAMEMSG Error processing results for this round. Sorry."));
				} catch (IOException ex) {
					ex.printStackTrace();
					// Do nothing...
				}
			}

			return;
		}

		String summaryString = "Summary: ";

		for (RoomClient rc : handles) {
			// Calculate total points...
			int points = 0;
			for (BluffedResponse resp : presentedChoices) {
				if (resp.nick != null && resp.nick.equals(rc.nick)) {
					points += (resp.ref_count * POINTS_FOR_SELECTED_BLUFF);
					break;
				}
			}

			if (realAnswer.selectors != null && realAnswer.selectors.contains(rc.nick))
				points += POINTS_FOR_CORRECT_ANSWER;

			summaryString += rc.nick + " " + String.valueOf(points) + "pts, ";
		}

		summaryString += "Correct Ans: " + realAnswer.bogusAnswer;

		for (RoomClient rc : handles) {
			try {
				rc.io_handle.sendMessage(new StringMessage("GAMEMSG " + summaryString));
			} catch (IOException ex) {
				ex.printStackTrace();
				// Do nothing...
			}
		}
	}

	private boolean submitAnswer(String nick, String msg) {
		// Check if this is a do-over...
		for (BluffedResponse bluff : presentedChoices) {
			if (bluff.selectors != null && bluff.selectors.contains(nick))
				return false;
		}

		try {
			int tmp = 0;
			tmp = Integer.parseInt(msg);

			if (tmp >= presentedChoices.size())
				return false;

			BluffedResponse selected = presentedChoices.get(tmp);
			if (selected.nick != null && selected.nick.equals(nick))
				return false;

			selected.ref_count++;

			if (selected.selectors == null)
				selected.selectors = new ArrayList<String>();
			selected.selectors.add(nick);

			try {
				String status = (selected.nick == null ? "Correct! +10pts" : "Wrong!");
				RoomClient rcl = room.getNamedClient(nick);

				if (rcl != null)
					rcl.io_handle.sendMessage(new StringMessage("GAMEMSG " + status));
			} catch (Exception ex) {
			}

			modifyQrefs(1, REF_DECR);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	private boolean submitBluff(String nick, String msg) {
		BluffedResponse bluff = new BluffedResponse();
		bluff.id = currentQuestion.id;
		bluff.ref_count = 0;
		bluff.selectors = null;
		bluff.nick = nick;
		bluff.bogusAnswer = msg.toLowerCase();

		if (responseSet == null) {
			responseSet = new ArrayList<BluffedResponse>();
			responseSet.add(bluff);

			modifyQrefs(1, REF_DECR);
			return true;
		}

		// No do-overs!
		for (BluffedResponse _bluff : responseSet) {
			if (_bluff.nick.equals(nick))
				return false;
		}

		responseSet.add(bluff);
		modifyQrefs(1, REF_DECR);
		return true;
	}

	@Override
	public synchronized boolean processMessage(String nick, String orig_command, String msg) {
		if (currentState == SESSION_COLLECTING_BLUFFS || (currentState == SESSION_PRESENTING_QUESTION && qrefs > 0)) {
			if (orig_command.equals("TXTRESP")) {
				return submitBluff(nick, msg);
			}
		}

		if (currentState == SESSION_GATHERING_RESPONSES || (currentState == SESSION_PRESENTING_BLUFFS && qrefs > 0)) {
			if (orig_command.equals("SELECTRESP")) {
				return submitAnswer(nick, msg);
			}
		}

		return false;
	}

	@Override
	public void run() {
		room.broadcastMessage("Room Admin", "BLUFFER GAME STARTED. Good Luck!!");

		currentState = SESSION_OFFLINE;
		try {
			loadQuestionSet();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Allow bluff selection
		currentState = SESSION_STARTING;
		qrefs = 0;

		//array of all connections
		ArrayList<ProtocolCallback<StringMessage>> handles = room.getClientHandles();
		if (handles == null) {
			System.err.println("Room is suddenly empty during a game! Did everyone drop?");
			currentState = SESSION_OFFLINE;
			room.stopGame(this);
			return;
		}

		for (QuestionAnswerPair question : questionSet) {
			responseSet = null;
			presentedChoices = null;
			currentQuestion = null;

			currentState = SESSION_PRESENTING_QUESTION;
			qrefs = 0;

			//asking the question
			for (ProtocolCallback<StringMessage> _handle : handles) {
				try {
					_handle.sendMessage(new StringMessage("ASKTXT " + question.questionText));
					//count each client that i asked
					qrefs++;
				} catch (IOException e) {
					System.err.println("Could not push question to a client.");
					e.printStackTrace();
				}
			}

			currentQuestion = question;
			currentState = SESSION_COLLECTING_BLUFFS;
			blockUntilAllAnswer(qrefs);

			currentState = SESSION_PRESENTING_BLUFFS;
			qrefs = 0;

			if (room.getActiveClientCount() != handles.size()) {
				// Someone dropped
				handles = room.getClientHandles();

				if (handles == null) {
					System.err.println("Room is suddenly empty during a game! Did everyone drop?");
					currentState = SESSION_OFFLINE;
					room.stopGame(this);

					return;
				}
			}

			presentedChoices = this.getSomeBluffsAndAnswer(question, CHOICES_COUNT);
			String msgString = "";

			for (int i = 0; i < presentedChoices.size(); ++i)
				msgString += String.valueOf(i) + ". " + presentedChoices.get(i).bogusAnswer + " ";

			for (ProtocolCallback<StringMessage> _handle : handles) {
				try {
					_handle.sendMessage(new StringMessage("ASKCHOICES " + msgString));
					qrefs++;
				} catch (IOException e) {
					System.err.println("Could not push question to a client.");
					e.printStackTrace();
				}
			}

			currentState = SESSION_GATHERING_RESPONSES;
			blockUntilAllAnswer(qrefs);

			currentState = SESSION_RANKING;
			rankCurrentQuestion(room.getRoomClients());
		}

		currentState = SESSION_OFFLINE;
		room.stopGame(this);
		room.broadcastMessage("", "GAMEMSG BLUFFER ENDED");
	}
}