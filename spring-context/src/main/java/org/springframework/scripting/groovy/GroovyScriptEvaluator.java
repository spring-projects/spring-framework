/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import java.io.IOException;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Groovy-based implementation of Spring's {@link ScriptEvaluator} strategy interface.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see GroovyShell#evaluate(String, String)
 */
public class GroovyScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

	private @Nullable ClassLoader classLoader;

	private CompilerConfiguration compilerConfiguration = new CompilerConfiguration();


	/**
	 * Construct a new GroovyScriptEvaluator.
	 */
	public GroovyScriptEvaluator() {
	}

	/**
	 * Construct a new GroovyScriptEvaluator.
	 * @param classLoader the ClassLoader to use as a parent for the {@link GroovyShell}
	 */
	public GroovyScriptEvaluator(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Set a custom compiler configuration for this evaluator.
	 * @since 4.3.3
	 * @see #setCompilationCustomizers
	 */
	public void setCompilerConfiguration(@Nullable CompilerConfiguration compilerConfiguration) {
		this.compilerConfiguration =
				(compilerConfiguration != null ? compilerConfiguration : new CompilerConfiguration());
	}

	/**
	 * Return this evaluator's compiler configuration (never {@code null}).
	 * @since 4.3.3
	 * @see #setCompilerConfiguration
	 */
	public CompilerConfiguration getCompilerConfiguration() {
		return this.compilerConfiguration;
	}

	/**
	 * Set one or more customizers to be applied to this evaluator's compiler configuration.
	 * <p>Note that this modifies the shared compiler configuration held by this evaluator.
	 * @since 4.3.3
	 * @see #setCompilerConfiguration
	 */
	public void setCompilationCustomizers(CompilationCustomizer... compilationCustomizers) {
		this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public @Nullable Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	public @Nullable Object evaluate(ScriptSource script, @Nullable Map<String, Object> arguments) {
		GroovyShell groovyShell = new GroovyShell(
				this.classLoader, new Binding(arguments), this.compilerConfiguration);
		try {
			String filename = (script instanceof ResourceScriptSource resourceScriptSource ?
					resourceScriptSource.getResource().getFilename() : null);
			if (filename != null) {
				return groovyShell.evaluate(script.getScriptAsString(), filename);
			}
			else {
				return groovyShell.evaluate(script.getScriptAsString());
			}
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access Groovy script", ex);
		}
		catch (GroovyRuntimeException ex) {
			throw new ScriptCompilationException(script, ex);
		}
	}

}
