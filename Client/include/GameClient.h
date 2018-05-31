
#ifndef ECHOCLIENT_ECHOCLIENT_H_
#define ECHOCLIENT_ECHOCLIENT_H_

#include <stdlib.h>
#include <boost/locale.hpp>
#include <boost/thread.hpp>
#include <boost/ref.hpp>
#include "connectionHandler.h"
#include "utf8.h"
#include "encoder.h"
#include <boost/algorithm/string.hpp>

class echoClient {

	public:

	int main (int argc, char *argv[]);
	void readLineFromConsole(ConnectionHandler connectionHandler);
	void readLineFromNetwork(ConnectionHandler connectionHandler);
};



#endif /* ECHOCLIENT_ECHOCLIENT_H_ */
