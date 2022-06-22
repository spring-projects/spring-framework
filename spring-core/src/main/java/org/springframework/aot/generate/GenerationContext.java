/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.generate;

import org.springframework.aot.hint.ProxyHints;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.ResourceHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.SerializationHints;

/**
 * Central interface used for code generation.
 *
 * <p>A generation context provides:
 * <ul>
 * <li>Management of all {@link #getGeneratedClasses()} generated classes},
 * including naming convention support.</li>
 * <li>Central management of all {@link #getGeneratedFiles() generated files}.</li>
 * <li>Support for the recording of {@link #getRuntimeHints() runtime hints}.</li>
 * </ul>
 *
 * <p>If a dedicated round of code generation is required while processing, it
 * is possible to create a specialized context using {@link #withName(String)}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface GenerationContext {

	/**
	 * Return the {@link GeneratedClasses} being used by the context. Allows a
	 * single generated class to be shared across multiple AOT processors. All
	 * generated classes are written at the end of AOT processing.
	 * @return the generated classes
	 */
	GeneratedClasses getGeneratedClasses();

	/**
	 * Return the {@link GeneratedFiles} being used by the context. Used to
	 * write resource, java source or class bytecode files.
	 * @return the generated files
	 */
	GeneratedFiles getGeneratedFiles();

	/**
	 * Return the {@link RuntimeHints} being used by the context. Used to record
	 * {@link ReflectionHints reflection}, {@link ResourceHints resource},
	 * {@link SerializationHints serialization} and {@link ProxyHints proxy}
	 * hints so that the application can run as a native image.
	 * @return the runtime hints
	 */
	RuntimeHints getRuntimeHints();

	/**
	 * Return a new {@link GenerationContext} instance using the specified
	 * name to qualify generated assets for a dedicated round of code
	 * generation. If this name is already in use, a unique sequence is added
	 * to ensure the name is unique.
	 * @param name the name to use
	 * @return a specialized {@link GenerationContext} for the specified name
	 */
	GenerationContext withName(String name);

}
