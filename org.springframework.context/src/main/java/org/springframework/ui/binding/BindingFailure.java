package org.springframework.ui.binding;

import java.util.Map;

public interface BindingFailure {

    /**
     * The code identifying the type of failure.
     * This code can be used to resolve the failure message if no explicit {@link #getMessage() message} is configured.
     */
	String getCode();

	/**
	 * The severity of the failure, which measures the impact of the failure on the user.
	 */
	Severity getSeverity();

    /**
     * An map of arguments that can be used as named parameters in resolvable messages associated with this failure.
     * Each constraint defines a set of arguments that are specific to it. For example, a length
     * constraint might define arguments of "min" and "max" of Integer values. In the message bundle, you then might see
     * "length=The ${label} field value must be between ${min} and ${max}". Returns an empty map if no arguments are present.
     */	
	Map<String, Object> getArguments();

	/**
	 * The message summarizing this failure. May be a literal string or a resolvable message code. Can be null.
	 * If null, the failure message will be resolved using the failure code.
	 */
	String getDefaultMessage();
	
	/** 
	 * A map of details providing additional information about this failure. Each entry in this map is a failure detail
	 * item that has a name and value. The name uniquely identifies the failure detail and describes its purpose;
	 * for example, a "cause" or "recommendedAction". The value is the failure detail message, either a literal string or
	 * resolvable code. If resolvable, the detail code is resolved relative to this failure.
	 * Returns an empty map if no details are present.
	 */ 
	Map<String, String> getDetails();
}
