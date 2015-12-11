package org.opennms.core.wsman;

/**
 * Generic WS-Man related exception.
 *
 * @author jwhite
 */
public class WSManException extends RuntimeException {
    private static final long serialVersionUID = -2894934806760355903L;

    public WSManException(String message) {
        super(message);
    }

    public WSManException(String message, Throwable cause) {
        super(message, cause);
    }
}
