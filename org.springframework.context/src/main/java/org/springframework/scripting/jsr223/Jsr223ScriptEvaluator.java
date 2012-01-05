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
 * Jsr233/javax.scripting implementation of {@link ScriptEvaluator}. 
 * 
 * @author Costin Leau
 */
public class Jsr223ScriptEvaluator implements ScriptEvaluator {

	private final Log log = LogFactory.getLog(getClass());

	private String language;
	private String extension;
	private ClassLoader classLoader;


	/**
	 * Constructs a new <code>Jsr223ScriptEvaluator</code> instance.
	 */
	public Jsr223ScriptEvaluator() {
		this(null);
	};

	/**
	 * Constructs a new <code>Jsr223ScriptEvaluator</code> instance.
	 *
	 * @param classLoader class loader to use
	 */
	public Jsr223ScriptEvaluator(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public Object evaluate(ScriptSource script) {
		return evaluate(script, Collections.<String, Object> emptyMap());
	}

	public Object evaluate(ScriptSource script, Map<String, Object> arguments) {
		ScriptEngine engine = discoverEngine(script, arguments);

		Bindings bindings = (!CollectionUtils.isEmpty(arguments) ? new SimpleBindings(arguments) : null);

		try {
			Reader scriptAsReader = (script instanceof ReadableScriptSource ? ((ReadableScriptSource) script).getScriptAsReader() : null);

			if (bindings == null) {
				return (scriptAsReader == null ? engine.eval(script.getScriptAsString()) : engine.eval(scriptAsReader));
			}
			else {
				return (scriptAsReader == null ? engine.eval(script.getScriptAsString(), bindings) : engine.eval(scriptAsReader, bindings));
			}
		} catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script", ex);
		} catch (ScriptException ex) {
			throw new ScriptCompilationException(script, ex);
		}
	}

	protected ScriptEngine discoverEngine(ScriptSource script, Map<String, Object> arguments) {
		ScriptEngineManager engineManager = new ScriptEngineManager(classLoader);
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

		Assert.notNull(engine, "No suitable engine found for "
				+ (StringUtils.hasText(language) ? "language " + language : "extension " + extension));

		if (log.isDebugEnabled()) {
			ScriptEngineFactory factory = engine.getFactory();
			log.debug(String.format("Using ScriptEngine %s (%s), language %s (%s)", factory.getEngineName(),
					factory.getEngineVersion(), factory.getLanguageName(), factory.getLanguageVersion()));
		}

		return engine;
	}

	/**
	 * Sets the extension of the language meant for evaluation the scripts.. 
	 * 
	 * @param extension The extension to set.
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * Sets the name of language meant for evaluation the scripts.
	 * 
	 * @param language The language to set.
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
}