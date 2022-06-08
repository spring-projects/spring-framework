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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * A managed collection of generated classes.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see GeneratedClass
 */
public class GeneratedClasses implements ClassGenerator {

	private final ClassNameGenerator classNameGenerator;

	private final Map<Owner, GeneratedClass> classes = new ConcurrentHashMap<>();


	public GeneratedClasses(ClassNameGenerator classNameGenerator) {
		Assert.notNull(classNameGenerator, "'classNameGenerator' must not be null");
		this.classNameGenerator = classNameGenerator;
	}


	@Override
	public GeneratedClass getOrGenerateClass(JavaFileGenerator javaFileGenerator,
			Class<?> target, String featureName) {

		Assert.notNull(javaFileGenerator, "'javaFileGenerator' must not be null");
		Assert.notNull(target, "'target' must not be null");
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Owner owner = new Owner(javaFileGenerator, target.getName(), featureName);
		return this.classes.computeIfAbsent(owner,
				key -> new GeneratedClass(javaFileGenerator,
						this.classNameGenerator.generateClassName(target, featureName)));
	}

	/**
	 * Write generated Spring {@code .factories} files to the given
	 * {@link GeneratedFiles} instance.
	 * @param generatedFiles where to write the generated files
	 * @throws IOException on IO error
	 */
	public void writeTo(GeneratedFiles generatedFiles) throws IOException {
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		List<GeneratedClass> generatedClasses = new ArrayList<>(this.classes.values());
		generatedClasses.sort(Comparator.comparing(GeneratedClass::getName));
		for (GeneratedClass generatedClass : generatedClasses) {
			generatedFiles.addSourceFile(generatedClass.generateJavaFile());
		}
	}

	private record Owner(JavaFileGenerator javaFileGenerator, String target,
			String featureName) {

	}

}
