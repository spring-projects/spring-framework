/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * Exception to be thrown on script compilation failure.
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class ScriptCompilationException extends NestedRuntimeException {

	@Nullable
	private ScriptSource scriptSource;


	/**
	 * Constructor for ScriptCompilationException.
	 * @param msg the detail message
	 */
	public ScriptCompilationException(String msg) {
		super(msg);
	}

	/**
	 * Constructor for ScriptCompilationException.
	 * @param msg the detail message
	 * @param cause the root cause (usually from using an underlying script compiler API)
	 */
	public ScriptCompilationException(String msg, Throwable cause) {
		super(msg, cause);
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
	@Nullable
	public ScriptSource getScriptSource() {
		return this.scriptSource;
	}

}
