/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

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
	@Nullable String suggestedClassName();

}
