/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NestedRuntimeException;

/**
 * Exception to be thrown on script compilation failure.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ScriptCompilationException extends NestedRuntimeException {

	private final @Nullable ScriptSource scriptSource;


	/**
	 * Constructor for ScriptCompilationException.
	 * @param msg the detail message
	 */
	public ScriptCompilationException(String msg) {
		super(msg);
		this.scriptSource = null;
	}

	/**
	 * Constructor for ScriptCompilationException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying script compiler API)
	 */
	public ScriptCompilationException(String msg, Throwable cause) {
		super(msg, cause);
		this.scriptSource = null;
	}

	/**
	 * Constructor for ScriptCompilationException.
	 * @param scriptSource the source for the offending script
	 * @param msg the detail message
	 * @since 4.2
	 */
	public ScriptCompilationException(ScriptSource scriptSource, String msg) {
		super("Could not compile " + scriptSource + ": " + msg);
		this.scriptSource = scriptSource;
	}

	/**
	 * Constructor for ScriptCompilationException.
	 * @param scriptSource the source for the offending script
	 * @param cause the root cause (usually from using an underlying script compiler API)
	 */
	public ScriptCompilationException(ScriptSource scriptSource, Throwable cause) {
		super("Could not compile " + scriptSource, cause);
		this.scriptSource = scriptSource;
	}

	/**
	 * Constructor for ScriptCompilationException.
	 * @param scriptSource the source for the offending script
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying script compiler API)
	 */
	public ScriptCompilationException(ScriptSource scriptSource, String msg, Throwable cause) {
		super("Could not compile " + scriptSource + ": " + msg, cause);
		this.scriptSource = scriptSource;
	}


	/**
	 * Return the source for the offending script.
	 * @return the source, or {@code null} if not available
	 */
	public @Nullable ScriptSource getScriptSource() {
		return this.scriptSource;
	}

}
