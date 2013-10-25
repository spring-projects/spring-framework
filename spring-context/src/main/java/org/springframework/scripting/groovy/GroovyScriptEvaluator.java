package org.springframework.scripting.groovy;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;

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

	private ClassLoader classLoader;


	/**
	 * Construct a new GroovyScriptEvaluator.
	 */
	public GroovyScriptEvaluator() {
	}

	/**
	 * Construct a new GroovyScriptEvaluator.
	 * @param classLoader the ClassLoader to use as a parent for the {@link GroovyShell}
	 */
	public GroovyScriptEvaluator(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	public Object evaluate(ScriptSource script, Map<String, Object> arguments) {
		GroovyShell groovyShell = new GroovyShell(this.classLoader, new Binding(arguments));
		try {
			String filename = (script instanceof ResourceScriptSource ?
					((ResourceScriptSource) script).getResource().getFilename() : null);
			if (filename != null) {
				return groovyShell.evaluate(script.getScriptAsString(), filename);
			}
			else {
				return groovyShell.evaluate(script.getScriptAsString());
			}
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script", ex);
		}
		catch (CompilationFailedException ex) {
			throw new ScriptCompilationException(script, "Evaluation failure", ex);
		}
	}

}
