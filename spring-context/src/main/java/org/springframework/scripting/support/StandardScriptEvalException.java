/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.support;

import javax.script.ScriptException;

/**
 * Exception decorating a {@link javax.script.ScriptException} coming out of
 * JSR-223 script evaluation, i.e. a {@link javax.script.ScriptEngine#eval}
 * call or {@link javax.script.Invocable#invokeMethod} /
 * {@link javax.script.Invocable#invokeFunction} call.
 *
 * <p>This exception does not print the Java stacktrace, since the JSR-223
 * {@link ScriptException} results in a rather convoluted text output.
 * From that perspective, this exception is primarily a decorator for a
 * {@link ScriptException} root cause passed into an outer exception.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.2.2
 */
@SuppressWarnings("serial")
public class StandardScriptEvalException extends RuntimeException {

	private final ScriptException scriptException;


	/**
	 * Construct a new script eval exception with the specified original exception.
	 */
	public StandardScriptEvalException(ScriptException ex) {
		super(ex.getMessage());
		this.scriptException = ex;
	}


	public final ScriptException getScriptException() {
		return this.scriptException;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
