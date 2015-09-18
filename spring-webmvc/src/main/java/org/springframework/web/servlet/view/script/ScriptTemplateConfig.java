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

package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import javax.script.ScriptEngine;

/**
 * Interface to be implemented by objects that configure and manage a
 * JSR-223 {@link ScriptEngine} for automatic lookup in a web environment.
 * Detected and used by {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public interface ScriptTemplateConfig {

	/**
	 * Return the {@link ScriptEngine} to use by the views.
	 */
	ScriptEngine getEngine();

	/**
	 * Return the engine name that will be used to instantiate the {@link ScriptEngine}.
	 */
	String getEngineName();

	/**
	 * Return whether to use a shared engine for all threads or whether to create
	 * thread-local engine instances for each thread.
	 */
	Boolean isSharedEngine();

	/**
	 * Return the scripts to be loaded by the script engine (library or user provided).
	 */
	String[] getScripts();

	/**
	 * Return the object where the render function belongs (optional).
	 */
	String getRenderObject();

	/**
	 * Return the render function name (mandatory).
	 */
	String getRenderFunction();

	/**
	 * Return the content type to use for the response.
	 * @since 4.2.1
	 */
	String getContentType();

	/**
	 * Return the charset used to read script and template files.
	 */
	Charset getCharset();

	/**
	 * Return the resource loader path(s) via a Spring resource location.
	 */
	String getResourceLoaderPath();

}
