/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

/**
 * Common operations for dealing with a JSR-223 {@link ScriptEngine}.
 *
 * @author Juergen Hoeller
 * @since 4.2.2
 */
public abstract class StandardScriptUtils {

	/**
	 * Retrieve a {@link ScriptEngine} from the given {@link ScriptEngineManager}
	 * by name, delegating to {@link ScriptEngineManager#getEngineByName} but
	 * throwing a descriptive exception if not found or if initialization failed.
	 * @param scriptEngineManager the ScriptEngineManager to use
	 * @param engineName the name of the engine
	 * @return a corresponding ScriptEngine (never {@code null})
	 * @throws IllegalArgumentException if no matching engine has been found
	 * @throws IllegalStateException if the desired engine failed to initialize
	 */
	public static ScriptEngine retrieveEngineByName(ScriptEngineManager scriptEngineManager, String engineName) {
		ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
		if (engine == null) {
			Set<String> engineNames = new LinkedHashSet<>();
			for (ScriptEngineFactory engineFactory : scriptEngineManager.getEngineFactories()) {
				List<String> factoryNames = engineFactory.getNames();
				if (factoryNames.contains(engineName)) {
					// Special case: getEngineByName returned null but engine is present...
					// Let's assume it failed to initialize (which ScriptEngineManager silently swallows).
					// If it happens to initialize fine now, alright, but we really expect an exception.
					try {
						engine = engineFactory.getScriptEngine();
						engine.setBindings(scriptEngineManager.getBindings(), ScriptContext.GLOBAL_SCOPE);
					}
					catch (Throwable ex) {
						throw new IllegalStateException("Script engine with name '" + engineName +
								"' failed to initialize", ex);
					}
				}
				engineNames.addAll(factoryNames);
			}
			throw new IllegalArgumentException("Script engine with name '" + engineName +
					"' not found; registered engine names: " + engineNames);
		}
		return engine;
	}

	static Bindings getBindings(Map<String, Object> bindings) {
		return (bindings instanceof Bindings ? (Bindings) bindings : new SimpleBindings(bindings));
	}

}
