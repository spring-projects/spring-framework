package org.springframework.model.message;

/**
 * A factory for a default message to return if no message could be resolved.
 * Allows the message String to be created lazily, only when it is needed.
 * @author Keith Donald
 * @since 3.0
 * @see MessageBuilder
 */
public interface DefaultMessageFactory {
	
	/**
	 * Create the default message.
	 */
	String createDefaultMessage();
}
