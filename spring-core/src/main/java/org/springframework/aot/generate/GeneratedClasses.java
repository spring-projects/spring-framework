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
import java.util.function.Consumer;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A managed collection of generated classes. This class is stateful so the
 * same instance should be used for all class generation.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see GeneratedClass
 */
public class GeneratedClasses {

	private final ClassNameGenerator classNameGenerator;

	private final List<GeneratedClass> classes;

	private final Map<Owner, GeneratedClass> classesByOwner;

	/**
	 * Create a new instance using the specified naming conventions.
	 * @param classNameGenerator the class name generator to use
	 */
	public GeneratedClasses(ClassNameGenerator classNameGenerator) {
		this(classNameGenerator, new ArrayList<>(), new ConcurrentHashMap<>());
	}

	private GeneratedClasses(ClassNameGenerator classNameGenerator,
			List<GeneratedClass> classes, Map<Owner, GeneratedClass> classesByOwner) {
		Assert.notNull(classNameGenerator, "'classNameGenerator' must not be null");
		this.classNameGenerator = classNameGenerator;
		this.classes = classes;
		this.classesByOwner = classesByOwner;
	}

	/**
	 * Prepare a {@link GeneratedClass} for the specified {@code featureName}
	 * targeting the specified {@code component}.
	 * @param featureName the name of the feature to associate with the generated class
	 * @param component the target component
	 * @return a {@link Builder} for further configuration
	 */
	public Builder forFeatureComponent(String featureName, Class<?> component) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		Assert.notNull(component, "'component' must not be null");
		return new Builder(featureName, component);
	}

	/**
	 * Prepare a {@link GeneratedClass} for the specified {@code featureName}
	 * and no particular component. This should be used for high-level code
	 * generation that are widely applicable and for entry points.
	 * @param featureName the name of the feature to associate with the generated class
	 * @return a {@link Builder} for further configuration
	 */
	public Builder forFeature(String featureName) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		return new Builder(featureName, null);
	}

	/**
	 * Write the {@link GeneratedClass generated classes} using the given
	 * {@link GeneratedFiles} instance.
	 * @param generatedFiles where to write the generated classes
	 * @throws IOException on IO error
	 */
	public void writeTo(GeneratedFiles generatedFiles) throws IOException {
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		List<GeneratedClass> generatedClasses = new ArrayList<>(this.classes);
		generatedClasses.sort(Comparator.comparing(GeneratedClass::getName));
		for (GeneratedClass generatedClass : generatedClasses) {
			generatedFiles.addSourceFile(generatedClass.generateJavaFile());
		}
	}

	GeneratedClasses withName(String name) {
		return new GeneratedClasses(this.classNameGenerator.usingFeatureNamePrefix(name),
				this.classes, this.classesByOwner);
	}

	private record Owner(String id, String className) {

	}

	public class Builder {

		private final String featureName;

		@Nullable
		private final Class<?> target;


		Builder(String featureName, @Nullable Class<?> target) {
			this.target = target;
			this.featureName = featureName;
		}

		/**
		 * Generate a new {@link GeneratedClass} using the specified type
		 * customizer.
		 * @param typeSpecCustomizer a customizer for the {@link TypeSpec.Builder}
		 * @return a new {@link GeneratedClass}
		 */
		public GeneratedClass generate(Consumer<TypeSpec.Builder> typeSpecCustomizer) {
			Assert.notNull(typeSpecCustomizer, "'typeSpecCustomizer' must not be null");
			return createGeneratedClass(typeSpecCustomizer);
		}


		/**
		 * Get or generate a new {@link GeneratedClass} for the specified {@code id}.
		 * @param id a unique identifier
		 * @param typeSpecCustomizer a customizer for the {@link TypeSpec.Builder}
		 * @return a {@link GeneratedClass} instance
		 */
		public GeneratedClass getOrGenerate(String id,
				Consumer<TypeSpec.Builder> typeSpecCustomizer) {
			Assert.hasLength(id, "'id' must not be empty");
			Assert.notNull(typeSpecCustomizer, "'typeSpecCustomizer' must not be null");
			Owner owner = new Owner(id, GeneratedClasses.this.classNameGenerator
					.getClassName(this.target, this.featureName));
			return GeneratedClasses.this.classesByOwner.computeIfAbsent(owner,
					key -> createGeneratedClass(typeSpecCustomizer));
		}

		private GeneratedClass createGeneratedClass(Consumer<TypeSpec.Builder> typeSpecCustomizer) {
			ClassName className = GeneratedClasses.this.classNameGenerator
					.generateClassName(this.target, this.featureName);
			GeneratedClass generatedClass = new GeneratedClass(typeSpecCustomizer, className);
			GeneratedClasses.this.classes.add(generatedClass);
			return generatedClass;
		}

	}

}
