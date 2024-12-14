import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import p1.server.ai.ServerPartidaAI;

public class ServerPartidaTest {

    // Acces a variables sense visibilitat publica
    public class InnerServerPartida extends ServerPartidaAI {
        public void fillBoard() {
            for (int i=0; i<ROWS; i++) {
                for (int j=0; j<COLS; j++) {
                    move(i,j,'-');
                }
            }
        }

        public boolean play(int i, int j, char c) {
            this.r = i;
            this.c = j;
            return setMove(c);
        }
    }

    @Test
    public void moves_test() {
        InnerServerPartida partida = new InnerServerPartida();

        // Moviments incorrectes
        assertFalse(partida.isMove("1_0"));
        assertFalse(partida.isMove("a-0"));
        assertFalse(partida.isMove("5-0"));
        assertFalse(partida.isMove("0-b"));
        assertFalse(partida.isMove("0-6"));

        // Moviments correctes
        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++) {
                assertTrue(partida.isMove(String.valueOf(i) + "-" + String.valueOf(j)));
            }
        }
    }

    @Test
    public void set_move_test() {
        InnerServerPartida partida = new InnerServerPartida();

        // Moviments valids
        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++) {
                assertTrue(partida.play(i,j,'O'));
            }
        }

        // Moviment invalids (repetits)
        for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++) {
                assertFalse(partida.play(i,j,'X'));
            }
        }
    }

    @Test
    public void finished_test() {
        InnerServerPartida partida = new InnerServerPartida();

        // Partida inacabada
        assertFalse(partida.isFinished());

        // Partida acabada (empat)
        partida.fillBoard();
        assertTrue(partida.isFinished());
        assertEquals(2, partida.getResult());   // 2 = empat

        // Partida acabada (guanya client)
        partida = new InnerServerPartida();
        assertTrue(partida.play(0,0,'X'));
        assertTrue(partida.play(1,1,'X'));
        assertTrue(partida.play(2,2,'X'));
        assertTrue(partida.isFinished());
        assertEquals(1, partida.getResult());   // 1 = client

        // Partida acabada (guanya servidor)
        partida = new InnerServerPartida();
        assertTrue(partida.play(0,2,'O'));
        assertTrue(partida.play(1,1,'O'));
        assertTrue(partida.play(2,0,'O'));
        assertTrue(partida.isFinished());
        assertEquals(0, partida.getResult());   // 0 = servidor
    }

    @Test
    public void exhaustive_finished_test() {
        char[] chars = {'O','X'};
        int[][][] winning_positions = {
            // Horitzontals
            {{0,0},{0,1},{0,2}},
            {{1,0},{1,1},{1,2}},
            {{2,0},{2,1},{2,2}},

            // Verticals
            {{0,0},{1,0},{2,0}},
            {{0,1},{1,1},{2,1}},
            {{0,2},{1,2},{2,2}},

            // Diagonals
            {{0,0},{1,1},{2,2}},
            {{0,2},{1,1},{2,0}}
        };

        for (int i=0; i<2; i++) {
            char c = chars[i];
            for (int[][] positions : winning_positions) {
                InnerServerPartida partida = new InnerServerPartida();
                for (int[] pos : positions) {
                    assertFalse(partida.isFinished());
                    assertTrue(partida.play(pos[0], pos[1], c));
                }

                // Estat guanyador
                assertTrue(partida.isFinished());
                assertEquals(i, partida.getResult());
            }
        }
    }

    @Test
    public void ai_test() {
        InnerServerPartida partida = new InnerServerPartida();
        partida.play(0,0,'X');
        partida.play(1,1,'O');
        
        /**  --- --- --- 
            | X |   |   |
            |-----------|
            |   | O |   |
            |-----------|
            |   |   |   |
            --- --- ---   */

        partida.play(0,1,'X');

        /**  --- --- --- 
            | X | X |   |
            |-----------|
            |   | O |   |
            |-----------|
            |   |   |   |
            --- --- ---   */

        // IA ha de bloquejar
        assertEquals("0-2", partida.getMove());

        /**  --- --- --- 
            | X | X | O |
            |-----------|
            |   | O |   |
            |-----------|
            |   |   |   |
            --- --- ---   */        

        partida.play(1,0,'X');

        /**  --- --- --- 
            | X | X | O |
            |-----------|
            | X | O |   |
            |-----------|
            |   |   |   |
            --- --- ---   */            

        // IA guanya
        assertEquals("2-0", partida.getMove());
        assertTrue(partida.isFinished());
        assertEquals(0, partida.getResult());   // 0 = servidor

        /**  --- --- --- 
            | X | X | O |
            |-----------|
            | X | O |   |
            |-----------|
            | O |   |   |
            --- --- ---   */  
    }
}
