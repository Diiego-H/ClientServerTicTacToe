package p1.client;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TicTacToeBoard {
    private char[][] board;

    public TicTacToeBoard() {
        // Initialize the board
        board = new char[3][3];
        for (char[] row : board) {
            Arrays.fill(row, ' ');
        }
    }

    public String boardToString() {
        String s = "\n    0   1   2  \n";
        s += "   --- --- --- \n";
        s += "0 | " + board[0][0] + " | " + board[0][1] + " | " + board[0][2] + " |\n";
        s += "  |-----------|\n";
        s += "1 | " + board[1][0] + " | " + board[1][1] + " | " + board[1][2] + " |\n";
        s += "  |-----------|\n";
        s += "2 | " + board[2][0] + " | " + board[2][1] + " | " + board[2][2] + " |\n";
        s += "   --- --- --- \n";
        return s;
    }

    public void makeMove(String move, char player) {
        int row = Character.getNumericValue(move.charAt(0));
        int col = Character.getNumericValue(move.charAt(2));

        if (isValidMove(row, col)) {
            board[row][col] = player;
        } else {
            System.out.println("Invalid move. Try again.");
        }
    }

    public boolean moveIsKnown(String moviment) {
        // Creem un pattern
        Pattern r = Pattern.compile("[012]-[012]");

        // Creem un matcher
        Matcher m = r.matcher(moviment);

        // Si el moviment té el format correcte i és dins del tauler
        return m.matches();
    }

    public boolean cellIsEmpty(int row, int col) {
        return board[row][col] == ' ';
    }

    private boolean isValidMove(int row, int col) {
        return row >= 0 && row < 3 && col >= 0 && col < 3;
    }
}