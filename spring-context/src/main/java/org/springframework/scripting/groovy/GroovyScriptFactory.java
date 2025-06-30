/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.scripting.groovy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link org.springframework.scripting.ScriptFactory} implementation
 * for a Groovy script.
 *
 * <p>Typically used in combination with a
 * {@link org.springframework.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's javadoc for a configuration example.
 *
 * <p>Note: Spring 4.0 supports Groovy 1.8 and higher.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rod Johnson
 * @since 2.0
 * @see groovy.lang.GroovyClassLoader
 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor
 */
public class GroovyScriptFactory implements ScriptFactory, BeanFactoryAware, BeanClassLoaderAware {

	private final String scriptSourceLocator;

	private @Nullable GroovyObjectCustomizer groovyObjectCustomizer;

	private @Nullable CompilerConfiguration compilerConfiguration;

	private @Nullable GroovyClassLoader groovyClassLoader;

	private @Nullable Class<?> scriptClass;

	private @Nullable Class<?> scriptResultClass;

	private @Nullable CachedResultHolder cachedResult;

	private final Object scriptClassMonitor = new Object();

	private boolean wasModifiedForTypeCheck = false;


	/**
	 * Create a new GroovyScriptFactory for the given script source.
	 * <p>We don't need to specify script interfaces here, since
	 * a Groovy script defines its Java interfaces itself.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 */
	public GroovyScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
	}

	/**
	 * Create a new GroovyScriptFactory for the given script source,
	 * specifying a strategy interface that can create a custom MetaClass
	 * to supply missing methods and otherwise change the behavior of the object.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param groovyObjectCustomizer a customizer that can set a custom metaclass
	 * or make other changes to the GroovyObject created by this factory
	 * (may be {@code null})
	 * @see GroovyObjectCustomizer#customize
	 */
	public GroovyScriptFactory(String scriptSourceLocator, @Nullable GroovyObjectCustomizer groovyObjectCustomizer) {
		this(scriptSourceLocator);
		this.groovyObjectCustomizer = groovyObjectCustomizer;
	}

	/**
	 * Create a new GroovyScriptFactory for the given script source,
	 * specifying a strategy interface that can create a custom MetaClass
	 * to supply missing methods and otherwise change the behavior of the object.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param compilerConfiguration a custom compiler configuration to be applied
	 * to the GroovyClassLoader (may be {@code null})
	 * @since 4.3.3
	 * @see GroovyClassLoader#GroovyClassLoader(ClassLoader, CompilerConfiguration)
	 */
	public GroovyScriptFactory(String scriptSourceLocator, @Nullable CompilerConfiguration compilerConfiguration) {
		this(scriptSourceLocator);
		this.compilerConfiguration = compilerConfiguration;
	}

	/**
	 * Create a new GroovyScriptFactory for the given script source,
	 * specifying a strategy interface that can customize Groovy's compilation
	 * process within the underlying GroovyClassLoader.
	 * @param scriptSourceLocator a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * @param compilationCustomizers one or more customizers to be applied to the
	 * GroovyClassLoader compiler configuration
	 * @since 4.3.3
	 * @see CompilerConfiguration#addCompilationCustomizers
	 * @see org.codehaus.groovy.control.customizers.ImportCustomizer
	 */
	public GroovyScriptFactory(String scriptSourceLocator, CompilationCustomizer... compilationCustomizers) {
		this(scriptSourceLocator);
		if (!ObjectUtils.isEmpty(compilationCustomizers)) {
			this.compilerConfiguration = new CompilerConfiguration();
			this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
		}
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory clbf) {
			clbf.ignoreDependencyType(MetaClass.class);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (classLoader instanceof GroovyClassLoader gcl && (this.compilerConfiguration == null ||
				gcl.hasCompatibleConfiguration(this.compilerConfiguration))) {
			this.groovyClassLoader = gcl;
		}
		else {
			this.groovyClassLoader = buildGroovyClassLoader(classLoader);
		}
	}

	/**
	 * Return the GroovyClassLoader used by this script factory.
	 */
	public GroovyClassLoader getGroovyClassLoader() {
		synchronized (this.scriptClassMonitor) {
			if (this.groovyClassLoader == null) {
				this.groovyClassLoader = buildGroovyClassLoader(ClassUtils.getDefaultClassLoader());
			}
			return this.groovyClassLoader;
		}
	}

	/**
	 * Build a {@link GroovyClassLoader} for the given {@code ClassLoader}.
	 * @param classLoader the ClassLoader to build a GroovyClassLoader for
	 * @since 4.3.3
	 */
	protected GroovyClassLoader buildGroovyClassLoader(@Nullable ClassLoader classLoader) {
		return (this.compilerConfiguration != null ?
				new GroovyClassLoader(classLoader, this.compilerConfiguration) : new GroovyClassLoader(classLoader));
	}


	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	/**
	 * Groovy scripts determine their interfaces themselves,
	 * hence we don't need to explicitly expose interfaces here.
	 * @return {@code null} always
	 */
	@Override
	public Class<?> @Nullable [] getScriptInterfaces() {
		return null;
	}

	/**
	 * Groovy scripts do not need a config interface,
	 * since they expose their setters as public methods.
	 */
	@Override
	public boolean requiresConfigInterface() {
		return false;
	}


	/**
	 * Loads and parses the Groovy script via the GroovyClassLoader.
	 * @see groovy.lang.GroovyClassLoader
	 */
	@Override
	public @Nullable Object getScriptedObject(ScriptSource scriptSource, Class<?> @Nullable ... actualInterfaces)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				Class<?> scriptClassToExecute;
				this.wasModifiedForTypeCheck = false;

				if (this.cachedResult != null) {
					Object result = this.cachedResult.object;
					this.cachedResult = null;
					return result;
				}

				if (this.scriptClass == null || scriptSource.isModified()) {
					// New script content...
					this.scriptClass = getGroovyClassLoader().parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

					if (Script.class.isAssignableFrom(this.scriptClass)) {
						// A Groovy script, probably creating an instance: let's execute it.
						Object result = executeScript(scriptSource, this.scriptClass);
						this.scriptResultClass = (result != null ? result.getClass() : null);
						return result;
					}
					else {
						this.scriptResultClass = this.scriptClass;
					}
				}
				scriptClassToExecute = this.scriptClass;

				// Process re-execution outside the synchronized block.
				return executeScript(scriptSource, scriptClassToExecute);
			}
			catch (CompilationFailedException ex) {
				this.scriptClass = null;
				this.scriptResultClass = null;
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public @Nullable Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				if (this.scriptClass == null || scriptSource.isModified()) {
					// New script content...
					this.wasModifiedForTypeCheck = true;
					this.scriptClass = getGroovyClassLoader().parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

					if (Script.class.isAssignableFrom(this.scriptClass)) {
						// A Groovy script, probably creating an instance: let's execute it.
						Object result = executeScript(scriptSource, this.scriptClass);
						this.scriptResultClass = (result != null ? result.getClass() : null);
						this.cachedResult = new CachedResultHolder(result);
					}
					else {
						this.scriptResultClass = this.scriptClass;
					}
				}
				return this.scriptResultClass;
			}
			catch (CompilationFailedException ex) {
				this.scriptClass = null;
				this.scriptResultClass = null;
				this.cachedResult = null;
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		synchronized (this.scriptClassMonitor) {
			return (scriptSource.isModified() || this.wasModifiedForTypeCheck);
		}
	}


	/**
	 * Instantiate the given Groovy script class and run it if necessary.
	 * @param scriptSource the source for the underlying script
	 * @param scriptClass the Groovy script class
	 * @return the result object (either an instance of the script class
	 * or the result of running the script instance)
	 * @throws ScriptCompilationException in case of instantiation failure
	 */
	protected @Nullable Object executeScript(ScriptSource scriptSource, Class<?> scriptClass) throws ScriptCompilationException {
		try {
			GroovyObject groovyObj = (GroovyObject) ReflectionUtils.accessibleConstructor(scriptClass).newInstance();

			if (this.groovyObjectCustomizer != null) {
				// Allow metaclass and other customization.
				this.groovyObjectCustomizer.customize(groovyObj);
			}

			if (groovyObj instanceof Script script) {
				// A Groovy script, probably creating an instance: let's execute it.
				return script.run();
			}
			else {
				// An instance of the scripted class: let's return it as-is.
				return groovyObj;
			}
		}
		catch (NoSuchMethodException ex) {
			throw new ScriptCompilationException(
					"No default constructor on Groovy script class: " + scriptClass.getName(), ex);
		}
		catch (InstantiationException ex) {
			throw new ScriptCompilationException(
					scriptSource, "Unable to instantiate Groovy script class: " + scriptClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new ScriptCompilationException(
					scriptSource, "Could not access Groovy script constructor: " + scriptClass.getName(), ex);
		}
		catch (InvocationTargetException ex) {
			throw new ScriptCompilationException(
					"Failed to invoke Groovy script constructor: " + scriptClass.getName(), ex.getTargetException());
		}
	}


	@Override
	public String toString() {
		return "GroovyScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}


	/**
	 * Wrapper that holds a temporarily cached result object.
	 */
	private static class CachedResultHolder {

		public final @Nullable Object object;

		public CachedResultHolder(@Nullable Object object) {
			this.object = object;
		}
	}

}
