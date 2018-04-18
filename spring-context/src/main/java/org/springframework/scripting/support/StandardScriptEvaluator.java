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

package org.springframework.scripting.support;

import java.io.IOException;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
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

	@Nullable
	private String engineName;

	@Nullable
	private volatile Bindings globalBindings;

	@Nullable
	private volatile ScriptEngineManager scriptEngineManager;


	/**
	 * Construct a new {@code StandardScriptEvaluator}.
	 */
	public StandardScriptEvaluator() {
	}

	/**
	 * Construct a new {@code StandardScriptEvaluator} for the given class loader.
	 * @param classLoader the class loader to use for script engine detection
	 */
	public StandardScriptEvaluator(ClassLoader classLoader) {
		this.scriptEngineManager = new ScriptEngineManager(classLoader);
	}

	/**
	 * Construct a new {@code StandardScriptEvaluator} for the given JSR-223
	 * {@link ScriptEngineManager} to obtain script engines from.
	 * @param scriptEngineManager the ScriptEngineManager (or subclass thereof) to use
	 * @since 4.2.2
	 */
	public StandardScriptEvaluator(ScriptEngineManager scriptEngineManager) {
		this.scriptEngineManager = scriptEngineManager;
	}


	/**
	 * Set the name of the language meant for evaluating the scripts (e.g. "Groovy").
	 * <p>This is effectively an alias for {@link #setEngineName "engineName"},
	 * potentially (but not yet) providing common abbreviations for certain languages
	 * beyond what the JSR-223 script engine factory exposes.
	 * @see #setEngineName
	 */
	public void setLanguage(String language) {
		this.engineName = language;
	}

	/**
	 * Set the name of the script engine for evaluating the scripts (e.g. "Groovy"),
	 * as exposed by the JSR-223 script engine factory.
	 * @since 4.2.2
	 * @see #setLanguage
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * Set the globally scoped bindings on the underlying script engine manager,
	 * shared by all scripts, as an alternative to script argument bindings.
	 * @since 4.2.2
	 * @see #evaluate(ScriptSource, Map)
	 * @see javax.script.ScriptEngineManager#setBindings(Bindings)
	 * @see javax.script.SimpleBindings
	 */
	public void setGlobalBindings(Map<String, Object> globalBindings) {
		Bindings bindings = StandardScriptUtils.getBindings(globalBindings);
		this.globalBindings = bindings;
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager != null) {
			scriptEngineManager.setBindings(bindings);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			scriptEngineManager = new ScriptEngineManager(classLoader);
			this.scriptEngineManager = scriptEngineManager;
			Bindings bindings = this.globalBindings;
			if (bindings != null) {
				scriptEngineManager.setBindings(bindings);
			}
		}
	}


	@Override
	@Nullable
	public Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	@Nullable
	public Object evaluate(ScriptSource script, @Nullable Map<String, Object> argumentBindings) {
		ScriptEngine engine = getScriptEngine(script);
		try {
			if (CollectionUtils.isEmpty(argumentBindings)) {
				return engine.eval(script.getScriptAsString());
			}
			else {
				Bindings bindings = StandardScriptUtils.getBindings(argumentBindings);
				return engine.eval(script.getScriptAsString(), bindings);
			}
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script for ScriptEngine", ex);
		}
		catch (ScriptException ex) {
			throw new ScriptCompilationException(script, new StandardScriptEvalException(ex));
		}
	}

	/**
	 * Obtain the JSR-223 ScriptEngine to use for the given script.
	 * @param script the script to evaluate
	 * @return the ScriptEngine (never {@code null})
	 */
	protected ScriptEngine getScriptEngine(ScriptSource script) {
		ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
		if (scriptEngineManager == null) {
			scriptEngineManager = new ScriptEngineManager();
			this.scriptEngineManager = scriptEngineManager;
		}

		if (StringUtils.hasText(this.engineName)) {
			return StandardScriptUtils.retrieveEngineByName(scriptEngineManager, this.engineName);
		}
		else if (script instanceof ResourceScriptSource) {
			Resource resource = ((ResourceScriptSource) script).getResource();
			String extension = StringUtils.getFilenameExtension(resource.getFilename());
			if (extension == null) {
				throw new IllegalStateException(
						"No script language defined, and no file extension defined for resource: " + resource);
			}
			ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
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
