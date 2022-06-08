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

import java.util.Collection;
import java.util.Collections;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;

/**
 * Generates new {@link GeneratedClass} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedMethods
 */
public interface ClassGenerator {

	/**
	 * Get or generate a new {@link GeneratedClass} for a given java file
	 * generator, target and feature name.
	 * @param javaFileGenerator the java file generator
	 * @param target the target of the newly generated class
	 * @param featureName the name of the feature that the generated class
	 * supports
	 * @return a {@link GeneratedClass} instance
	 */
	GeneratedClass getOrGenerateClass(JavaFileGenerator javaFileGenerator,
			Class<?> target, String featureName);


	/**
	 * Strategy used to generate the java file for the generated class.
	 * Implementations of this interface are included as part of the key used to
	 * identify classes that have already been created and as such should be
	 * static final instances or implement a valid
	 * {@code equals}/{@code hashCode}.
	 */
	@FunctionalInterface
	interface JavaFileGenerator {

		/**
		 * Generate the file {@link JavaFile} to be written.
		 * @param className the class name of the file
		 * @param methods the generated methods that must be included
		 * @return the generated files
		 */
		JavaFile generateJavaFile(ClassName className, GeneratedMethods methods);

		/**
		 * Return method names that must not be generated.
		 * @return the reserved method names
		 */
		default Collection<String> getReservedMethodNames() {
			return Collections.emptySet();
		}

	}

}
