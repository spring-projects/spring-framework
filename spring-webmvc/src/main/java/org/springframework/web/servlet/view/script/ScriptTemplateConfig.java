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

import org.springframework.core.io.ResourceLoader;

/**
 * Interface to be implemented by objects that configure and manage a
 * {@link ScriptEngine} for automatic lookup in a web environment.
 * Detected and used by {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public interface ScriptTemplateConfig {

	ScriptEngine getEngine();

	String getRenderObject();

	String getRenderFunction();

	Charset getCharset();

	ResourceLoader getResourceLoader();

}
