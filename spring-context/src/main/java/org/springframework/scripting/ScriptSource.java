/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scripting;

import java.io.IOException;

import org.springframework.lang.Nullable;

/**
 * Interface that defines the source of a script.
 * Tracks whether the underlying script has been modified.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface ScriptSource {

	/**
	 * Retrieve the current script source text as String.
	 * @return the script text
	 * @throws IOException if script retrieval failed
	 */
	String getScriptAsString() throws IOException;

	/**
	 * Indicate whether the underlying script data has been modified since
	 * the last time {@link #getScriptAsString()} was called.
	 * Returns {@code true} if the script has not been read yet.
	 * @return whether the script data has been modified
	 */
	boolean isModified();

	/**
	 * Determine a class name for the underlying script.
	 * @return the suggested class name, or {@code null} if none available
	 */
	@Nullable
	String suggestedClassName();

}
