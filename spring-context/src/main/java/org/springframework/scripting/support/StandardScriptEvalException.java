/*
 * Copyright 2002-2015 the original author or authors.
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
