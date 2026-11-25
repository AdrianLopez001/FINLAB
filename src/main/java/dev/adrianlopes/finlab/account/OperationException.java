package dev.adrianlopes.finlab.account;

/**
 * An operation that was refused. The message is shown to the user.
 */
public class OperationException extends RuntimeException {

    public OperationException(String message) {
        super(message);
    }
}
