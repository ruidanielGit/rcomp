1. Understand the Shared Board Protocol (SBP):

-The SBP is an application protocol that facilitates data exchanges between the Shared Board App and the Shared Board Server network applications.
-It is TCP-based, meaning that a TCP connection must be established before any data exchange can occur.
-The protocol follows the client-server model, where the client application (Shared Board App) initiates a TCP connection with the server application (Shared Board Server), which accepts the connection.
-Once the TCP connection is established, both the client and server applications can initiate data exchanges by sending requests and receiving responses.
-Each request must have a corresponding response, and they share a common message format.

2. Design the system architecture:

-The system consists of two main components: the Shared Board App and the Shared Board Server.
-The Shared Board Server is responsible for maintaining the Shared Board, which is a table of cells capable of holding text or images.
-The Shared Board Server stores all data in memory (non-persistent) during this stage of the project.
-The initial state of the board is a default size of five lines by five columns, with all cells initially empty.
-The Shared Board App acts as the client and interacts with the Shared Board Server using the SBP.
-The Shared Board App's main purpose is to manage the Shared Board by performing actions such as posting plain text or images into specific cells.
-Additionally, the Shared Board Server also functions as an HTTP server, providing a view-only rendering of the board in a web browser.

3. Components of the system:

-Shared board app communicates with Shared Board Server (using SBP) to manage the shared board.
-The Shared Board Server maintains the Shared Board, handles requests from the client, and provides a view-only rendering of the board via an HTTP server.

4. Interaction between the components:

-The shared board initiates the TCP connection with the Shared Board Server to establish connection.
-The Shared Board Server accepts incoming connection requests from the shared board app.
-Now both of them can send requests and receive responses.
-The shared board app can send commands to post plain text and perform actions such as resizing.
-The shared board server receives the requests, proccess them, updates the shared board accordingly, and sends back responses.

5. Resizing and cell management:

-(I would prefer) Auto resizing of the cells and columns.
-Decide how the shared board server will specify the target cell when posting the text (maybe just do it on the first free position).

6. HTTP server functionality:

-The shared board server will serve the shared board via a http server.
-Consider using AJAX (Asynchronous JavaScript and XML) to enable a view-only rendering of the board in a web browser without the need for page reloads.

7. Shared Board representation:

-Will use a list of lists, or arrays, of Strings.
-Implement mechanisms to handle concurrent access to the Shared Board data structures, ensuring thread-safety.

8. Implement Shared Board Server in Java































