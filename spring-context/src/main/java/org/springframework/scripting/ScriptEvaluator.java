/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Spring's strategy interface for evaluating a script.
 *
 * <p>Aside from language-specific implementations, Spring also ships
 * a version based on the standard {@code javax.script} package (JSR-223):
 * {@link org.springframework.scripting.support.StandardScriptEvaluator}.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 */
public interface ScriptEvaluator {

	/**
	 * Evaluate the given script.
	 * @param script the ScriptSource for the script to evaluate
	 * @return the return value of the script, if any
	 * @throws ScriptCompilationException if the evaluator failed to read,
	 * compile or evaluate the script
	 */
	@Nullable Object evaluate(ScriptSource script) throws ScriptCompilationException;

	/**
	 * Evaluate the given script with the given arguments.
	 * @param script the ScriptSource for the script to evaluate
	 * @param arguments the key-value pairs to expose to the script,
	 * typically as script variables (may be {@code null} or empty)
	 * @return the return value of the script, if any
	 * @throws ScriptCompilationException if the evaluator failed to read,
	 * compile or evaluate the script
	 */
	@Nullable Object evaluate(ScriptSource script, @Nullable Map<String, Object> arguments) throws ScriptCompilationException;

}
