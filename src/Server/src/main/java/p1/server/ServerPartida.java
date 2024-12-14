package p1.server;

public class ServerPartida {
    protected static final int ROWS = 3;
    protected static final int COLS = 3;

    protected char[][] board = new char[ROWS][COLS];
    protected int r,c;
    protected int espais,result;
    protected boolean finished;

    public ServerPartida() {
        result = -1; // Valor per defecte
        finished = false;
        espais = ROWS*COLS;
        for (int i=0; i<ROWS; i++) {
            for (int j=0; j<COLS; j++) {
                board[i][j] = ' ';
            }
        }
        //printBoard();
    }

    public int getResult() {
        return result;
    }

    public boolean isMove(String position) {
        // Comprovacio guio entre indexos
        if (position.charAt(1) != '-') {
            return false;
        }

        // Comprovacio indexos en les dimensions del taulell
        try {
            r = Integer.parseInt(position.substring(0, 1));
            c = Integer.parseInt(position.substring(2, 3));
            return !(r < 0 || c < 0 || r > ROWS-1 || c > ROWS-1);
        } catch (Exception e) {
            // Client no ha enviat enters per definir la posicio al taulell
            return false;
        }
    }

    protected boolean isFree(int i, int j) {
        return board[i][j] == ' ';
    }

    protected void move(int i, int j, char c) {
        board[i][j] = c;
        espais--;
        computeFinished();
    }

    public boolean setMove(char player) {
        boolean lliure = isFree(r,c);
        if (lliure) {
            move(r,c,player);
        }
        return lliure;
    }

    private String computeLine(char c1, char c2, char c3) {
        return String.valueOf(c1) + String.valueOf(c2) + String.valueOf(c3);
    }

    public boolean isFinished() {
        return finished;
    }

    private void computeFinished() {
        // Comprovem les 8 possibilitats
        for (int pos=0; pos<8; pos++) {
            String line = "";
            switch (pos) {
                case 0:
                    // Horitzontal superior
                    line = computeLine(board[0][0], board[0][1], board[0][2]);
                    break;
                case 1:
                    // Horitzontal central
                    line = computeLine(board[1][0], board[1][1], board[1][2]);
                    break;
                case 2:
                    // Horitzontal inferior
                    line = computeLine(board[2][0], board[2][1], board[2][2]);
                    break;
                case 3:
                    // Vertical esquerra
                    line = computeLine(board[0][0], board[1][0], board[2][0]);
                    break;
                case 4:
                    // Vertical central
                    line = computeLine(board[0][1], board[1][1], board[2][1]);
                    break;
                case 5:
                    // Vertical dreta
                    line = computeLine(board[0][2], board[1][2], board[2][2]);
                    break;
                case 6:
                    // Diagonal principal
                    line = computeLine(board[0][0], board[1][1], board[2][2]);
                    break;
                case 7:
                    // Diagonal secundaria
                    line = computeLine(board[0][2], board[1][1], board[2][0]);
                    break;
            }

            // 'O' player wins (cas Client VS Servidor guanya Servidor)
            if (line.equals("OOO")) {
                result = 0;
                finished = true;
                return;
            }

            // 'X' player wins (cas Client VS Servidor guanya Client)
            if (line.equals("XXX")) {
                result = 1;
                finished = true;
                return;
            }
        }

        // Si no hi ha guanyador i no es pot seguir jugant, es un empat
        if (espais == 0) {
            result = 2;
            finished = true;
        } else {
            finished = false;
        }
    }

    // To get the board in the format
    /*  --- --- --- 
       | O | X | X |
       |-----------|
       | O | O | O |
       |-----------|
       | O | X | X |
        --- --- ---  */
    @Override
    public String toString() {
        String s = "";
        s += " --- --- --- \n";
        s += "| " + board[0][0] + " | " + board[0][1] + " | " + board[0][2] + " |\n";
        s += "|-----------|\n";
        s += "| " + board[1][0] + " | " + board[1][1] + " | " + board[1][2] + " |\n";
        s += "|-----------|\n";
        s += "| " + board[2][0] + " | " + board[2][1] + " | " + board[2][2] + " |\n";
        s += " --- --- --- \n";
        return s;
    }
}