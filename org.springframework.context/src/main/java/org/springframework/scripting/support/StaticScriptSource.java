/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;

/**
 * Static implementation of the
 * {@link org.springframework.scripting.ScriptSource} interface,
 * encapsulating a given String that contains the script source text.
 * Supports programmatic updates of the script String.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class StaticScriptSource implements ScriptSource {

	private String script;

	private boolean modified;

	private String className;


	/**
	 * Create a new StaticScriptSource for the given script.
	 * @param script the script String
	 */
	public StaticScriptSource(String script) {
		setScript(script);
	}

	/**
	 * Create a new StaticScriptSource for the given script.
	 * @param script the script String
	 * @param className the suggested class name for the script
	 * (may be <code>null</code>)
	 */
	public StaticScriptSource(String script, String className) {
		setScript(script);
		this.className = className;
	}

	/**
	 * Set a fresh script String, overriding the previous script.
	 * @param script the script String
	 */
	public synchronized void setScript(String script) {
		Assert.hasText(script, "Script must not be empty");
		this.modified = !script.equals(this.script);
		this.script = script;
	}


	public synchronized String getScriptAsString() {
		this.modified = false;
		return this.script;
	}

	public synchronized boolean isModified() {
		return this.modified;
	}

	public String suggestedClassName() {
		return this.className;
	}


	@Override
	public String toString() {
		return "static script" + (this.className != null ? " [" + this.className + "]" : "");
	}

}
