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

package org.springframework.scripting.jsr223;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scripting.ReadableScriptSource;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ScriptEvaluator} implementation that delegates to an underlying
 * JSR-233/javax.scripting {@link ScriptEngine} to evaluate scripts. The particular
 * {@code ScriptEngine} to use is {@linkplain #determineScriptEngine determined} based
 * based on the language/extension of the specified script.
 *
 * @author Costin Leau
 * @author Chris Beams
 * @since 3.1.1
 */
public class Jsr223ScriptEvaluator implements ScriptEvaluator {

	private final Log log = LogFactory.getLog(getClass());

	private String language;
	private String extension;
	private ClassLoader classLoader;


	/**
	 * Construct a new {@code Jsr223ScriptEvaluator} instance, allowing the underlying
	 * JSR-223 {@link ScriptEngineManager} to determine the class loader to use.
	 * @see ScriptEngineManager#ScriptEngineManager()
	 */
	public Jsr223ScriptEvaluator() {
		this(null);
	}

	/**
	 * Construct a new {@code Jsr223ScriptEvaluator} instance.
	 *
	 * @param classLoader class loader to use when constructing underlying JSR-223
	 * {@link ScriptEngineManager} (may be {@code null}).
	 * @see #determineScriptEngine(ScriptSource, Map)
	 * @see ScriptEngineManager#ScriptEngineManager(ClassLoader)
	 */
	public Jsr223ScriptEvaluator(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Object evaluate(ScriptSource script) {
		return evaluate(script, Collections.<String, Object> emptyMap());
	}

	public Object evaluate(ScriptSource script, Map<String, Object> arguments) {
		ScriptEngine engine = determineScriptEngine(script, arguments);

		Bindings bindings = (!CollectionUtils.isEmpty(arguments) ?
				new SimpleBindings(arguments) : null);

		try {
			Reader scriptAsReader = (script instanceof ReadableScriptSource ?
					((ReadableScriptSource) script).getScriptAsReader() : null);

			if (bindings == null) {
				return (scriptAsReader == null ?
						engine.eval(script.getScriptAsString()) : engine.eval(scriptAsReader));
			}
			else {
				return (scriptAsReader == null ?
						engine.eval(script.getScriptAsString(), bindings) :
						engine.eval(scriptAsReader, bindings));
			}
		} catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script", ex);
		} catch (ScriptException ex) {
			throw new ScriptCompilationException(script, ex);
		}
	}

	/**
	 * Determine the JSR-223 {@link ScriptEngine} to be used for evaluating the given
	 * script based on the {@linkplain #setExtension extension} or
	 * {@linkplain #setLanguage language} of the script. If the script is a
	 * {@link ReadableScriptSource}, the extension will be discovered automatically.
	 * @param script the script to be evaluated
	 * @param arguments arguments provided to the script (ignored in this implementation)
	 * @throws IllegalArgumentException if language/extension have not been specified and
	 * no extension can be automatically discovered
	 * @throws IllegalArgumentException if no {@code ScriptEngine} associated with the
	 * given language/extension can be found
	 * @return the {@code ScriptEngine} to be used for the given language
	 */
	protected ScriptEngine determineScriptEngine(ScriptSource script, Map<String, Object> arguments) {
		ScriptEngineManager engineManager = new ScriptEngineManager(this.classLoader);
		ScriptEngine engine = null;

		if (StringUtils.hasText(language)) {
			engine = engineManager.getEngineByName(language);
		}
		else {
			if (!StringUtils.hasText(extension) && script instanceof ReadableScriptSource) {
				extension = StringUtils.getFilenameExtension(((ReadableScriptSource) script).suggestedScriptName());
			}
			Assert.hasText(extension, "no language or extension specified or detected");
			engine = engineManager.getEngineByExtension(extension);
		}

		Assert.notNull(engine, String.format("No suitable engine found for %s", 
				(StringUtils.hasText(language) ? "language " + language : "extension " + extension)));

		if (log.isDebugEnabled()) {
			ScriptEngineFactory factory = engine.getFactory();
			log.debug(String.format("Using ScriptEngine %s (%s), language %s (%s)",
					factory.getEngineName(), factory.getEngineVersion(),
					factory.getLanguageName(), factory.getLanguageVersion()));
		}

		return engine;
	}

	/**
	 * Set the script file extension, to be used when determining the JSR-223
	 * {@link ScriptEngine} to be used in evaluating the script. This property is not
	 * required if {@link #setLanguage} has been called or if the script provided is an
	 * implementation of {@link ReadableScriptSource}.
	 *
	 * @param extension the extension of the script, e.g. ".rb" for Ruby, ".js" for
	 * JavaScript, etc
	 * @see #determineScriptEngine
	 * @see ScriptEngineManager#getEngineByExtension
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * Set the script file extension, to be used when determining the JSR-223
	 * {@link ScriptEngine} to be used in evaluating the script. This property is not
	 * required if {@link #setExtension} has been called or if the script provided is an
	 * implementation of {@link ReadableScriptSource}.
	 *
	 * @param language the language to set
	 * @see #determineScriptEngine
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
}
