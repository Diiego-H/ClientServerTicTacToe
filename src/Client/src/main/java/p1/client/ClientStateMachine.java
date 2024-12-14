package p1.client;

public class ClientStateMachine {
    // Enum representing the states
    public enum State {
        HELLO_READY,
        HANDLE_HELLO_READY_ERROR,
        PLAY_ADMIT,
        HANDLE_PLAY_ADMIT_ERROR,
        PLAYING,
        HANDLE_PLAYING_ERROR,
        END
    }

    // Enum representing the actions
    public enum Action {
        HELLO_READY_ERROR_RECEIVED,
        ERROR_MESSAGE_PRINTED,
        READY_RECEIVED,
        ADMIT_RECEIVED,
        PLAY_ADMIT_ERROR_RECEIVED,
        PLAYING_ERROR_RECEIVED,
        ACTION_RECEIVED,
        RESULT_RECEIVED,
        PLAY_AGAIN
    }

    private State currentState;

    // Constructor to initialize the state machine with an initial state
    public ClientStateMachine() {
        this.currentState = State.HELLO_READY;
    }

    // Method to perform an action and transition to the next state
    public void performAction(Action action) {
        switch (currentState) {

            case HELLO_READY:
                if (action == Action.READY_RECEIVED) {
                    currentState = State.PLAY_ADMIT;
                } else if (action == Action.HELLO_READY_ERROR_RECEIVED) {
                    currentState = State.HANDLE_HELLO_READY_ERROR;
                } else {
                    System.out.println("Invalid action for state HELLO_READY.");
                }
                break;

            case HANDLE_HELLO_READY_ERROR:
                if (action == Action.ERROR_MESSAGE_PRINTED) {
                    currentState = State.HELLO_READY;
                } else {
                    System.out.println("Invalid action for state HANDLE_HELLO_READY_ERROR.");
                }
                break;

            case PLAY_ADMIT:
                if (action == Action.ADMIT_RECEIVED) {
                    currentState = State.PLAYING;
                } else if (action == Action.PLAY_ADMIT_ERROR_RECEIVED) {
                    currentState = State.HANDLE_PLAY_ADMIT_ERROR;
                } else {
                    System.out.println("Invalid action for state PLAY_ADMIT.");
                }
                break;

            case HANDLE_PLAY_ADMIT_ERROR:
                if (action == Action.ERROR_MESSAGE_PRINTED) {
                    currentState = State.PLAY_ADMIT;
                } else {
                    System.out.println("Invalid action for state HANDLE_PLAY_ADMIT_ERROR.");
                }
                break;

            case PLAYING:
                if (action == Action.PLAYING_ERROR_RECEIVED) {
                    currentState = State.HANDLE_PLAYING_ERROR;
                } else if (action == Action.ACTION_RECEIVED) {
                    // Ens quedem al mateix estat.
                } else if (action == Action.RESULT_RECEIVED) {
                    currentState = State.END;
                } else {
                    System.out.println("Invalid action for state PLAYING.");
                }
                break;

            case HANDLE_PLAYING_ERROR:
                if (action == Action.ERROR_MESSAGE_PRINTED) {
                    currentState = State.PLAYING;
                } else {
                    System.out.println("Invalid action for state HANDLE_PLAYING_ERROR.");
                }
                break;

            case END:
                if (action == Action.PLAY_AGAIN) {
                    currentState = State.PLAY_ADMIT;
                } else {
                    System.out.println("Invalid action for state HANDLE_PLAYING_ERROR.");
                }
                break;

            default:
                break;
        }
    }

    // Method to get the current state
    public State getCurrentState() {
        return currentState;
    }
}