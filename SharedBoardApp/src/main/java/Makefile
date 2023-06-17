P1=TcpChatCli
P2=TcpChatSrv
P3=SharedBoardHttpServer

SRC_FILES=$(P2).java $(P1).java $(P3).java SBPMessage.java
CLASS_FILES=$(SRC_FILES:.java=.class)

all: $(CLASS_FILES)

%.class: %.java
	javac $<

clean:
	rm -f $(CLASS_FILES)
