/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.scripting;

import java.util.Map;

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
	Object evaluate(ScriptSource script) throws ScriptCompilationException;

	/**
	 * Evaluate the given script with the given arguments.
	 * @param script the ScriptSource for the script to evaluate
	 * @param arguments the key-value pairs to expose to the script,
	 * typically as script variables (may be {@code null} or empty)
	 * @return the return value of the script, if any
	 * @throws ScriptCompilationException if the evaluator failed to read,
	 * compile or evaluate the script
	 */
	Object evaluate(ScriptSource script, Map<String, Object> arguments) throws ScriptCompilationException;

}
