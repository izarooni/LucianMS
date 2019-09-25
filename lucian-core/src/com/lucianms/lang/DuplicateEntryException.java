package com.lucianms.lang;

/**
 * @author izarooni
 */
public class DuplicateEntryException extends RuntimeException {

    public DuplicateEntryException() {
        super();
    }

    public DuplicateEntryException(String message) {
        super(message);
    }
}
