CFLAGS:=-c -Wall -Weffc++ -g
LDFLAGS:=-lboost_system -lboost_locale -lboost_thread

all: bin/encoder.o Client
	g++ -o bin/run bin/encoder.o bin/connectionHandler.o bin/Client.o $(LDFLAGS) 

Client: bin/connectionHandler.o bin/Client.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/Client.o: src/GameClient.cpp
	g++ $(CFLAGS) -o bin/Client.o src/GameClient.cpp
	
bin/encoder.o: src/encoder/encoder.cpp
	g++ $(CFLAGS) -o bin/encoder.o src/encoder/encoder.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
	