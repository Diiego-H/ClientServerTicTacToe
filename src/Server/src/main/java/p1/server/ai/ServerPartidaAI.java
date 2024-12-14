package p1.server.ai;

import p1.server.ServerPartida;

public class ServerPartidaAI extends ServerPartida {

    public String getMove() {
        // Best position to move (MiniMax)
        computeBestMove();
        move(r,c,'O');
        return Integer.toString(r) + "-" + Integer.toString(c);
    }

    private void unmove(int i, int j) {
        board[i][j] = ' ';
        espais++;
        finished = false;
    }

    /**
     * MiniMax amb poda Alfa-Beta per trobar el millor moviment possible pel Servidor. 
     * La profunditat màxima en aquest joc és de 9 jugades, així que no restringirem
     * l'exploració. La poda ens ajudarà a donar respostes més ràpides al Client.
     */
    private void computeBestMove() {
        int bestValue = Integer.MIN_VALUE;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (isFree(i, j)) {
                    move(i,j,'O');
                    int value = min(bestValue, Integer.MAX_VALUE);
                    unmove(i,j);
                    if (value > bestValue) {
                        bestValue = value;
                        r = i;
                        c = j;
                    }
                }
            }
        }
    }

    private int max(int alpha, int beta) {
        // Node terminal
        if (isFinished()) {
            return evaluateBoard();
        }

        int highest = Integer.MIN_VALUE;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (isFree(i, j)) {
                    move(i,j,'O');
                    highest = Math.max(highest, min(alpha,beta));
                    unmove(i,j);
                    if (highest >= beta) {
                        return highest;
                    }
                    alpha = Math.max(alpha, highest);
                }
            }
        }
        return highest;
    }

    private int min(int alpha, int beta) {
        // Node terminal
        if (isFinished()) {
            return evaluateBoard();
        }

        int lowest = Integer.MAX_VALUE;
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (isFree(i, j)) {
                    move(i,j,'X');
                    lowest = Math.min(lowest, max(alpha,beta));
                    unmove(i,j);
                    if (lowest <= alpha) {
                        return lowest;
                    }
                    beta = Math.min(beta, lowest);
                }
            }
        }
        return lowest;
    }

    private int evaluateBoard() {
        // Avaluacio segons el resultat
        switch (result) {
            // W: 'O'
            case 0:
                return 1;

            // W: 'X'
            case 1:
                return -1;
            
            // Draw
            default:
                return 0;
        }
    }
}