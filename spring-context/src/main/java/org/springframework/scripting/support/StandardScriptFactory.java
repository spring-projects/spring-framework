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

import java.io.IOException;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.scripting.ScriptFactory} implementation based
 * on the JSR-223 script engine abstraction (as included in Java 6+).
 * Supports JavaScript, Groovy, JRuby and other JSR-223 compliant engines.
 *
 * <p>Typically used in combination with a
 * {@link org.springframework.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's javadoc for a configuration example.
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see ScriptFactoryPostProcessor
 */
public class StandardScriptFactory implements ScriptFactory, BeanClassLoaderAware {

	private final String scriptEngineName;

	private final String scriptSourceLocator;

	private final Class<?>[] scriptInterfaces;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private volatile ScriptEngine scriptEngine;


	/**
	 * Create a new StandardScriptFactory for the given script source.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 */
	public StandardScriptFactory(String scriptSourceLocator) {
		this(null, scriptSourceLocator, (Class<?>[]) null);
	}

	/**
	 * Create a new StandardScriptFactory for the given script source.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param scriptInterfaces the Java interfaces that the scripted object
	 * is supposed to implement
	 */
	public StandardScriptFactory(String scriptSourceLocator, Class<?>... scriptInterfaces) {
		this(null, scriptSourceLocator, scriptInterfaces);
	}

	/**
	 * Create a new StandardScriptFactory for the given script source.
	 * @param scriptEngineName the name of the JSR-223 ScriptEngine to use
	 * (explicitly given instead of inferred from the script source)
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 */
	public StandardScriptFactory(String scriptEngineName, String scriptSourceLocator) {
		this(scriptEngineName, scriptSourceLocator, (Class<?>[]) null);
	}

	/**
	 * Create a new StandardScriptFactory for the given script source.
	 * @param scriptEngineName the name of the JSR-223 ScriptEngine to use
	 * (explicitly given instead of inferred from the script source)
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param scriptInterfaces the Java interfaces that the scripted object
	 * is supposed to implement
	 */
	public StandardScriptFactory(String scriptEngineName, String scriptSourceLocator, Class<?>... scriptInterfaces) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptEngineName = scriptEngineName;
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	@Override
	public Class<?>[] getScriptInterfaces() {
		return this.scriptInterfaces;
	}

	@Override
	public boolean requiresConfigInterface() {
		return false;
	}


	/**
	 * Load and parse the script via JSR-223's ScriptEngine.
	 */
	@Override
	public Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		Object script = evaluateScript(scriptSource);

		if (!ObjectUtils.isEmpty(actualInterfaces)) {
			boolean adaptationRequired = false;
			for (Class<?> requestedIfc : actualInterfaces) {
				if (script instanceof Class ? !requestedIfc.isAssignableFrom((Class<?>) script) :
						!requestedIfc.isInstance(script)) {
					adaptationRequired = true;
				}
			}
			if (adaptationRequired) {
				script = adaptToInterfaces(script, scriptSource, actualInterfaces);
			}
		}

		if (script instanceof Class) {
			Class<?> scriptClass = (Class<?>) script;
			try {
				return scriptClass.newInstance();
			}
			catch (InstantiationException ex) {
				throw new ScriptCompilationException(
						scriptSource, "Could not instantiate script class: " + scriptClass.getName(), ex);
			}
			catch (IllegalAccessException ex) {
				throw new ScriptCompilationException(
						scriptSource, "Could not access script constructor: " + scriptClass.getName(), ex);
			}
		}

		return script;
	}

	protected Object evaluateScript(ScriptSource scriptSource) {
		try {
			if (this.scriptEngine == null) {
				this.scriptEngine = retrieveScriptEngine(scriptSource);
				if (this.scriptEngine == null) {
					throw new IllegalStateException("Could not determine script engine for " + scriptSource);
				}
			}
			return this.scriptEngine.eval(scriptSource.getScriptAsString());
		}
		catch (Exception ex) {
			throw new ScriptCompilationException(scriptSource, ex);
		}
	}

	protected ScriptEngine retrieveScriptEngine(ScriptSource scriptSource) {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager(this.beanClassLoader);

		if (this.scriptEngineName != null) {
			return StandardScriptUtils.retrieveEngineByName(scriptEngineManager, this.scriptEngineName);
		}

		if (scriptSource instanceof ResourceScriptSource) {
			String filename = ((ResourceScriptSource) scriptSource).getResource().getFilename();
			if (filename != null) {
				String extension = StringUtils.getFilenameExtension(filename);
				if (extension != null) {
					ScriptEngine engine = scriptEngineManager.getEngineByExtension(extension);
					if (engine != null) {
						return engine;
					}
				}
			}
		}

		return null;
	}

	protected Object adaptToInterfaces(Object script, ScriptSource scriptSource, Class<?>... actualInterfaces) {
		Class<?> adaptedIfc;
		if (actualInterfaces.length == 1) {
			adaptedIfc = actualInterfaces[0];
		}
		else {
			adaptedIfc = ClassUtils.createCompositeInterface(actualInterfaces, this.beanClassLoader);
		}

		if (adaptedIfc != null) {
			if (!(this.scriptEngine instanceof Invocable)) {
				throw new ScriptCompilationException(scriptSource,
						"ScriptEngine must implement Invocable in order to adapt it to an interface: " +
								this.scriptEngine);
			}
			Invocable invocable = (Invocable) this.scriptEngine;
			if (script != null) {
				script = invocable.getInterface(script, adaptedIfc);
			}
			if (script == null) {
				script = invocable.getInterface(adaptedIfc);
				if (script == null) {
					throw new ScriptCompilationException(scriptSource,
							"Could not adapt script to interface [" + adaptedIfc.getName() + "]");
				}
			}
		}

		return script;
	}

	@Override
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		return null;
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		return scriptSource.isModified();
	}


	@Override
	public String toString() {
		return "StandardScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}

}
