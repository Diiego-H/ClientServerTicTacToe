# ClientServerTicTacToe
University project: Client-server model example. TicTacToe game coded in Java following a custom communication protocol.

## Single player
The server waits for a client to connect. Clients are handled in multiple threads and the server uses the Minimax algorithm to play against them.

## 2 players
The server waits for 2 clients to connect. When it receives 2 connection requests, it starts a thread to handle the match as a proxy between the players.
