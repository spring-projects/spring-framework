/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.view.script;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by objects that configure and manage a
 * JSR-223 {@link ScriptEngine} for automatic lookup in a web environment.
 * Detected and used by {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ScriptTemplateConfig {

	/**
	 * Return the {@link ScriptEngine} to use by the views.
	 */
	@Nullable
	ScriptEngine getEngine();

	/**
	 * Return the engine supplier that will be used to instantiate the {@link ScriptEngine}.
	 * @since 5.2
	 */
	@Nullable
	Supplier<ScriptEngine> getEngineSupplier();

	/**
	 * Return the engine name that will be used to instantiate the {@link ScriptEngine}.
	 */
	@Nullable
	String getEngineName();

	/**
	 * Return whether to use a shared engine for all threads or whether to create
	 * thread-local engine instances for each thread.
	 */
	@Nullable
	Boolean isSharedEngine();

	/**
	 * Return the scripts to be loaded by the script engine (library or user provided).
	 */
	@Nullable
	String[] getScripts();

	/**
	 * Return the object where the render function belongs (optional).
	 */
	@Nullable
	String getRenderObject();

	/**
	 * Return the render function name (optional). If not specified, the script templates
	 * will be evaluated with {@link ScriptEngine#eval(String, Bindings)}.
	 */
	@Nullable
	String getRenderFunction();

	/**
	 * Return the charset used to read script and template files.
	 */
	@Nullable
	Charset getCharset();

	/**
	 * Return the resource loader path(s) via a Spring resource location.
	 */
	@Nullable
	String getResourceLoaderPath();

}
