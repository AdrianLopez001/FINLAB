package dev.adrianlopes.finlab.account;

/**
 * The same form arrived twice (double click, back button + resend).
 * The first copy was already applied, so this one is ignored.
 */
public class DuplicateOperationException extends OperationException {

    public DuplicateOperationException() {
        super("This operation was already processed.");
    }
}
