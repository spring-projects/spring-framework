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

package org.springframework.web.servlet.view.script;

/**
 * Exception thrown when an error occurs during script template rendering.
 *
 * <p>It does not print the java stacktrace in the logs, since it is not useful
 * in this script context.
 *
 * @author Sebastien Deleuze
 * @since 4.2.2
 */
public class ScriptRenderException extends RuntimeException {

	private static final long serialVersionUID = 421565510962788082L;


	/**
	 * Constructs a new script rendering exception with the specified detail message.
	 */
	public ScriptRenderException(String msg) {
		super(msg);
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
