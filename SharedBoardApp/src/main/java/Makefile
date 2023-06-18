P1=TcpChatCli
P2=TcpChatSrv

SRC_FILES=$(P2).java $(P1).java SBPMessage.java
CLASS_FILES=$(SRC_FILES:.java=.class)

all: $(CLASS_FILES)

%.class: %.java
	javac $<

clean:
	rm -f $(CLASS_FILES)
