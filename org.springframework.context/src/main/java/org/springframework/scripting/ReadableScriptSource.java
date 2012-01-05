/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;
import java.io.Reader;

/**
 * Extension to {@link ScriptSource} that provides a {@link Reader} implementation
 * for the target source (suitable for streaming).
 * 
 * @author Costin Leau
 */
public interface ReadableScriptSource extends ScriptSource {

	/**
	 * Retrieves the script source text as {@link Reader}.
	 * 
	 * @return reader for the underlying script
	 * @throws IOException if script retrieval failed
	 */
	Reader getScriptAsReader() throws IOException;

	/**
	 * Determines a name for the underlying script.
	 * 
	 * @return the suggested script name, or <code>null</code> if none available
	 */
	String suggestedScriptName();

	/**
	 * Indicate whether the underlying script data has been modified since
	 * the last time {@link #getScriptAsString()} or {@link #getScriptAsReader() } was called.
	 * Returns <code>true</code> if the script has not been read yet.
	 * @return whether the script data has been modified
	 */
	boolean isModified();

}
