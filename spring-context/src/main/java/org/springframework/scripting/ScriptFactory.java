/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.scripting;

import java.io.IOException;

/**
 * Script definition interface, encapsulating the configuration
 * of a specific script as well as a factory method for
 * creating the actual scripted Java {@code Object}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see #getScriptSourceLocator
 * @see #getScriptedObject
 */
public interface ScriptFactory {

	/**
	 * Return a locator that points to the source of the script.
	 * Interpreted by the post-processor that actually creates the script.
	 * <p>Typical supported locators are Spring resource locations
	 * (such as "file:C:/myScript.bsh" or "classpath:myPackage/myScript.bsh")
	 * and inline scripts ("inline:myScriptText...").
	 * @return the script source locator
	 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor#convertToScriptSource
	 * @see org.springframework.core.io.ResourceLoader
	 */
	String getScriptSourceLocator();

	/**
	 * Return the business interfaces that the script is supposed to implement.
	 * <p>Can return {@code null} if the script itself determines
	 * its Java interfaces (such as in the case of Groovy).
	 * @return the interfaces for the script
	 */
	Class<?>[] getScriptInterfaces();

	/**
	 * Return whether the script requires a config interface to be
	 * generated for it. This is typically the case for scripts that
	 * do not determine Java signatures themselves, with no appropriate
	 * config interface specified in {@code getScriptInterfaces()}.
	 * @return whether the script requires a generated config interface
	 * @see #getScriptInterfaces()
	 */
	boolean requiresConfigInterface();

	/**
	 * Factory method for creating the scripted Java object.
	 * <p>Implementations are encouraged to cache script metadata such as
	 * a generated script class. Note that this method may be invoked
	 * concurrently and must be implemented in a thread-safe fashion.
	 * @param scriptSource the actual ScriptSource to retrieve
	 * the script source text from (never {@code null})
	 * @param actualInterfaces the actual interfaces to expose,
	 * including script interfaces as well as a generated config interface
	 * (if applicable; may be {@code null})
	 * @return the scripted Java object
	 * @throws IOException if script retrieval failed
	 * @throws ScriptCompilationException if script compilation failed
	 */
	Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException;

	/**
	 * Determine the type of the scripted Java object.
	 * <p>Implementations are encouraged to cache script metadata such as
	 * a generated script class. Note that this method may be invoked
	 * concurrently and must be implemented in a thread-safe fashion.
	 * @param scriptSource the actual ScriptSource to retrieve
	 * the script source text from (never {@code null})
	 * @return the type of the scripted Java object, or {@code null}
	 * if none could be determined
	 * @throws IOException if script retrieval failed
	 * @throws ScriptCompilationException if script compilation failed
	 * @since 2.0.3
	 */
	Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException;

	/**
	 * Determine whether a refresh is required (e.g. through
	 * ScriptSource's {@code isModified()} method).
	 * @param scriptSource the actual ScriptSource to retrieve
	 * the script source text from (never {@code null})
	 * @return whether a fresh {@link #getScriptedObject} call is required
	 * @since 2.5.2
	 * @see ScriptSource#isModified()
	 */
	boolean requiresScriptedObjectRefresh(ScriptSource scriptSource);

}
