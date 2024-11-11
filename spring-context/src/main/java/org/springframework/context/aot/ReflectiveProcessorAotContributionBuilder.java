/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.context.aot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflection;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Builder for an {@linkplain BeanFactoryInitializationAotContribution AOT
 * contribution} that detects the presence of {@link Reflective @Reflective} on
 * annotated elements and invoke the underlying {@link ReflectiveProcessor}
 * implementations.
 *
 * <p>Candidates can be provided explicitly or by scanning the classpath.
 *
 * @author Stephane Nicoll
 * @since 6.2
 * @see Reflective
 * @see RegisterReflection
 */
public class ReflectiveProcessorAotContributionBuilder {

	private static final ReflectiveRuntimeHintsRegistrar registrar = new ReflectiveRuntimeHintsRegistrar();

	private final Set<Class<?>> classes = new LinkedHashSet<>();


	/**
	 * Process the given classes by checking the ones that use {@link Reflective}.
	 * <p>A class is candidate if it uses {@link Reflective} directly or via a
	 * meta-annotation. Type, fields, constructors, methods and enclosed types
	 * are inspected.
	 * @param classes the classes to inspect
	 */
	public ReflectiveProcessorAotContributionBuilder withClasses(Iterable<Class<?>> classes) {
		this.classes.addAll(StreamSupport.stream(classes.spliterator(), false)
				.filter(registrar::isCandidate).toList());
		return this;
	}

	/**
	 * Process the given classes by checking the ones that use {@link Reflective}.
	 * <p>A class is candidate if it uses {@link Reflective} directly or via a
	 * meta-annotation. Type, fields, constructors, methods and enclosed types
	 * are inspected.
	 * @param classes the classes to inspect
	 */
	public ReflectiveProcessorAotContributionBuilder withClasses(Class<?>[] classes) {
		return withClasses(Arrays.asList(classes));
	}

	/**
	 * Scan the given {@code packageNames} and their sub-packages for classes
	 * that uses {@link Reflective}.
	 * <p>This performs a "deep scan" by loading every class in the specified
	 * packages and search for {@link Reflective} on types, constructors, methods,
	 * and fields. Enclosed classes are candidates as well. Classes that fail to
	 * load are ignored.
	 * @param classLoader the classloader to use
	 * @param packageNames the package names to scan
	 */
	public ReflectiveProcessorAotContributionBuilder scan(@Nullable ClassLoader classLoader, String... packageNames) {
		ReflectiveClassPathScanner scanner = new ReflectiveClassPathScanner(classLoader);
		return withClasses(scanner.scan(packageNames));
	}

	@Nullable
	public BeanFactoryInitializationAotContribution build() {
		return (!this.classes.isEmpty() ? new AotContribution(this.classes) : null);
	}

	private static class AotContribution implements BeanFactoryInitializationAotContribution {

		private final Class<?>[] classes;

		public AotContribution(Set<Class<?>> classes) {
			this.classes = classes.toArray(Class<?>[]::new);
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			RuntimeHints runtimeHints = generationContext.getRuntimeHints();
			registrar.registerRuntimeHints(runtimeHints, this.classes);
		}

	}

	private static class ReflectiveClassPathScanner extends ClassPathScanningCandidateComponentProvider {

		@Nullable
		private final ClassLoader classLoader;

		ReflectiveClassPathScanner(@Nullable ClassLoader classLoader) {
			super(false);
			this.classLoader = classLoader;
			addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
		}

		Class<?>[] scan(String... packageNames) {
			if (logger.isDebugEnabled()) {
				logger.debug("Scanning all types for reflective usage from " + Arrays.toString(packageNames));
			}
			Set<BeanDefinition> candidates = new HashSet<>();
			for (String packageName : packageNames) {
				candidates.addAll(findCandidateComponents(packageName));
			}
			return candidates.stream().map(c -> (Class<?>) c.getAttribute("type")).toArray(Class<?>[]::new);
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			String className = beanDefinition.getBeanClassName();
			if (className != null) {
				try {
					Class<?> type = ClassUtils.forName(className, this.classLoader);
					beanDefinition.setAttribute("type", type);
					return registrar.isCandidate(type);
				}
				catch (Exception ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Ignoring '%s' for reflective usage: %s".formatted(className, ex.getMessage()));
					}
				}
			}
			return false;
		}
	}

}
