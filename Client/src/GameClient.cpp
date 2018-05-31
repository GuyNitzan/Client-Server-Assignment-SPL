#include <stdlib.h>
#include <deque>
#include "../include/GameClient.h"
#include "../include/connectionHandler.h"

boost::mutex mx_guard_command_queue;
std::deque<std::string> command_queue;

bool stop_socket_io = false;
bool game_stopped = false;

std::string read_one_command() {
	boost::lock_guard<boost::mutex> lock(mx_guard_command_queue);

	if (command_queue.size() == 0)
		return std::string("");
	std::string result = command_queue.front();

	command_queue.pop_front();
	return result;
}

void insert_one_command(std::string command) {

	boost::lock_guard<boost::mutex> lock(mx_guard_command_queue);
	command_queue.push_back(command);
}

void process_message(std::string msg) {
	if (!msg.compare(0, 20, "SYSMSG QUIT ACCEPTED")) {
		game_stopped = true;
		stop_socket_io = true;
		std::cout << "Termination signal received. Closing..." << std::endl;
	}
}

void display_message(std::string msg) {
	std::cout << ">" << msg << std::endl;
}


void handle_socket_out(ConnectionHandler *conn) {
	while (!stop_socket_io) {
		std::string command = read_one_command();
		int len = command.length();

		if (len == 0) {
			boost::this_thread::sleep_for(boost::chrono::seconds(1));
			continue;
		}

		if (!conn->sendLine(command)) {
			std::cout << "Connection dropped. Aborting...\n" << std::endl;
			break;
		}
	}
}

//getting the respons from the server and print it
void handle_socket_in(ConnectionHandler *conn) {
	while (!stop_socket_io) {
		std::string response;
		int len;

		if (!conn->getLine(response)) {
			std::cout << "Connection dropped. Aborting...\n" << std::endl;
			break;
		}

		len = response.length();
		//remove the /n??
		response.resize(len - 1);

		display_message(response);
		//checking if finished
		process_message(response);
	}
}

void handle_stdin() {
	while (true) {
		const short bufsize = 1024;
		char buf[bufsize];

		//Dispatch a message from stdin
		std::cin.getline(buf, bufsize);
		std::string line(buf);

		int len = line.length();
		if (len)
			insert_one_command(line);

		boost::this_thread::sleep_for(boost::chrono::milliseconds(100));
	}
}

int main(int argc, char *argv[]) {
	if (argc < 3) {
		std::cerr << "Usage: " << argv[0] << " host port" << std::endl
				<< std::endl;
		return -1;
	}

	std::string host = argv[1];
	short port = atoi(argv[2]);

	ConnectionHandler conn(host, port);
	if (!conn.connect()) {
		std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
		return 1;
	}

	boost::thread main_session_thread(handle_stdin);
	boost::thread sock_in_thread(handle_socket_in, &conn);
	boost::thread sock_out_thread(handle_socket_out, &conn);

	main_session_thread.join();
	sock_in_thread.join();
	sock_out_thread.join();

	return 0;
}
