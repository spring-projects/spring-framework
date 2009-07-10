package org.springframework.ui.binding;

/**
 * Thrown by a BindingFactory when no binding to a property exists.
 * @author Keith Donald
 * @since 3.0
 * @see BindingFactory#getBinding(String)
 */
@SuppressWarnings("serial")
public class NoSuchBindingException extends RuntimeException {
	
	/** 
	 * Creates a new no such binding exception.
	 * @param property the requested property for which there is no binding
	 */
	public NoSuchBindingException(String property) {
		super("No binding to property '" + property + "' exists");
	}
}
