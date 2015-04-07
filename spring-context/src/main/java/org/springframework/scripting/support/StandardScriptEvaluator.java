/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.IOException;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code javax.script} (JSR-223) based implementation of Spring's {@link ScriptEvaluator}
 * strategy interface.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 * @see ScriptEngine#eval(String)
 */
public class StandardScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

	private volatile ScriptEngineManager scriptEngineManager;

	private String language;


	/**
	 * Construct a new StandardScriptEvaluator.
	 */
	public StandardScriptEvaluator() {
	}

	/**
	 * Construct a new StandardScriptEvaluator.
	 * @param classLoader the class loader to use for script engine detection
	 */
	public StandardScriptEvaluator(ClassLoader classLoader) {
		this.scriptEngineManager = new ScriptEngineManager(classLoader);
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.scriptEngineManager = new ScriptEngineManager(classLoader);
	}

	/**
	 * Set the name of language meant for evaluation the scripts (e.g. "Groovy").
	 */
	public void setLanguage(String language) {
		this.language = language;
	}


	@Override
	public Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	public Object evaluate(ScriptSource script, Map<String, Object> arguments) {
		ScriptEngine engine = getScriptEngine(script);
		Bindings bindings = (!CollectionUtils.isEmpty(arguments) ? new SimpleBindings(arguments) : null);
		try {
			return (bindings != null ? engine.eval(script.getScriptAsString(), bindings) :
					engine.eval(script.getScriptAsString()));
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script", ex);
		}
		catch (ScriptException ex) {
			throw new ScriptCompilationException(script, "Evaluation failure", ex);
		}
	}

	/**
	 * Obtain the JSR-223 ScriptEngine to use for the given script.
	 * @param script the script to evaluate
	 * @return the ScriptEngine (never {@code null})
	 */
	protected ScriptEngine getScriptEngine(ScriptSource script) {
		if (this.scriptEngineManager == null) {
			this.scriptEngineManager = new ScriptEngineManager();
		}
		if (StringUtils.hasText(this.language)) {
			ScriptEngine engine = this.scriptEngineManager.getEngineByName(this.language);
			if (engine == null) {
				throw new IllegalStateException("No matching engine found for language '" + this.language + "'");
			}
			return engine;
		}
		else if (script instanceof ResourceScriptSource) {
			Resource resource = ((ResourceScriptSource) script).getResource();
			String extension = StringUtils.getFilenameExtension(resource.getFilename());
			if (extension == null) {
				throw new IllegalStateException(
						"No script language defined, and no file extension defined for resource: " + resource);
			}
			ScriptEngine engine = this.scriptEngineManager.getEngineByExtension(extension);
			if (engine == null) {
				throw new IllegalStateException("No matching engine found for file extension '" + extension + "'");
			}
			return engine;
		}
		else {
			throw new IllegalStateException(
					"No script language defined, and no resource associated with script: " + script);
		}
	}

}
