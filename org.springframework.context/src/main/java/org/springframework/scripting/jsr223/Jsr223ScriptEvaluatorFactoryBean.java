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
 * {@link FactoryBean} implementation allowing for use of JSR-223 based
 * {@link ScriptEvaluator} within Spring XML configuration files.
 *
 * @author Costin Leau
 * @author Chris Beams
 * @since 3.1.1
 */
class Jsr223ScriptEvaluatorFactoryBean implements InitializingBean, BeanClassLoaderAware, FactoryBean<Object> {

	/**
	 * Enumeration specifying the policy for how often the script should be evaluated
	 * during the lifetime of this factory bean.
	 */
	public enum EvaluationPolicy {
		/**
		 * Evaluate the script once and only once, regardless of whether the script has
		 * been modified.
		 */
		ONCE,

		/**
		 * Evaluate the script if it has been modified since the last time it was evaluated.
		 */
		IF_MODIFIED,

		/**
		 * Evaluate the script every time {@code getObject} is invoked.
		 */
		ALWAYS;
	}

	private ClassLoader classLoader;
	private ScriptSource script;
	private Jsr223ScriptEvaluator evaluator;
	private String language, extension;
	private Map<String, Object> arguments;
	private EvaluationPolicy evaluationPolicy = EvaluationPolicy.ALWAYS;
	private final Object monitor = new Object();
	private volatile boolean evaluated;
	private Object result = null;
	private boolean runAtStartup = false;

	public Object getObject() {
		switch (evaluationPolicy) {
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
	 * Method for post-processing script arguments. Useful for enhancing (adding) new
	 * arguments to scripts being executed. The default implementation is a no-op.
	 *
	 * @param arguments arguments provided to the script
	 */
	protected void postProcess(Map<String, Object> arguments) {

	}

	public Class<Object> getObjectType() {
		return Object.class;
	}

	public boolean isSingleton() {
		return EvaluationPolicy.ONCE.equals(evaluationPolicy);
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * @param script the script to be evaluated
	 */
	public void setScriptSource(ScriptSource script) {
		this.script = script;
	}

	/**
	 * @param language the language of the script to be evaluated.
	 * @see Jsr223ScriptEvaluator#setLanguage
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * @param extension the extension of the script to be evaluated.
	 * @see Jsr223ScriptEvaluator#setExtension
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * Set the {@link EvaluationPolicy} for the script.
	 */
	public void setEvaluationPolicy(EvaluationPolicy evaluationPolicy) {
		this.evaluationPolicy = evaluationPolicy;
	}

	/**
	 * @param arguments the arguments to provide to the script
	 */
	public void setArguments(Map<String, Object> arguments) {
		this.arguments = arguments;
	}

	/**
	 * Return whether the script should be evaluated immediately upon container startup.
	 */
	public boolean isRunAtStartup() {
		return runAtStartup;
	}

	/**
	 * Indicate whether the {@linkplain #setScriptSource script} should be evaluated
	 * immediately upon container startup. If false, evaluation will occur only when
	 * {@link #getObject()} is called. The default is {@code false}.
	 */
	public void setRunAtStartup(boolean runAtStartup) {
		this.runAtStartup = runAtStartup;
	}
}
