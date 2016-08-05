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

import java.util.Map;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scripting.ScriptSource;

/**
 * FactoryBean for easy declarative use of JSR-223 based {@link ScriptEvaluator}.
 * 
 * @author Costin Leau
 */
class Jsr223ScriptEvaluatorFactoryBean implements InitializingBean, BeanClassLoaderAware, FactoryBean<Object> {

	public enum EvaluationType {
		ONCE, IF_MODIFIED, ALWAYS;
	}

	private ClassLoader classLoader;
	private ScriptSource script;
	private Jsr223ScriptEvaluator evaluator;
	private String language, extension;
	private Map<String, Object> arguments;
	private EvaluationType evaluation = EvaluationType.ALWAYS;
	private final Object monitor = new Object();
	private volatile boolean evaluated;
	private Object result = null;
	private boolean runAtStartup = false;

	public Object getObject() {
		switch (evaluation) {
		case ONCE:
			if (!evaluated) {
				synchronized (monitor) {
					if (!evaluated) {
						evaluated = true;
						result = evaluator.evaluate(script, arguments);
					}
				}
			}
			return result;
		case IF_MODIFIED:
			// isModified is synchronized so only one thread will see the update
			if (script.isModified()) {
				result = evaluator.evaluate(script, arguments);
			}
			return result;
		default:
			return evaluator.evaluate(script, arguments);
		}
	}

	public void afterPropertiesSet() {
		evaluator = new Jsr223ScriptEvaluator(classLoader);
		evaluator.setLanguage(language);
		evaluator.setExtension(extension);

		postProcess(arguments);

		if (runAtStartup) {
			getObject();
		}
	}

	/**
	 * Method for post-processing arguments. Useful for enhancing (adding) new arguments to scripts
	 * being executed.
	 * 
	 * @param arguments
	 */
	protected void postProcess(Map<String, Object> arguments) {

	}

	public Class<Object> getObjectType() {
		return Object.class;
	}

	public boolean isSingleton() {
		return EvaluationType.ONCE.equals(evaluation);
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * @param resource The resource to set.
	 */
	public void setScriptSource(ScriptSource script) {
		this.script = script;
	}

	/**
	 * @param language The language to set.
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @param extension The extension to set.
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * Sets the way the script is modified.
	 * 
	 * @param singleton
	 */
	public void setEvaluate(EvaluationType evaluation) {
		this.evaluation = evaluation;
	}

	/**
	 * @param arguments The arguments to set.
	 */
	public void setArguments(Map<String, Object> arguments) {
		this.arguments = arguments;
	}

	/**
	 * Indicates whether the script gets executed once the factory bean initializes or not (default).
	 * 
	 * @return true if the script runs or not during startup
	 */
	public boolean isRunAtStartup() {
		return runAtStartup;
	}

	/**
	 * @param runAtStartup The runStartUp to set.
	 */
	public void setRunAtStartup(boolean runAtStartup) {
		this.runAtStartup = runAtStartup;
	}
}