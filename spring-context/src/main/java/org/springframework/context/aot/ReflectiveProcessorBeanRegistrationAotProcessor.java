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

package org.springframework.context.aot;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.aot.hint.support.RuntimeHintsUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * AOT {@code BeanRegistrationAotProcessor} that detects the presence of
 * {@link Reflective @Reflective} on annotated elements and invoke the
 * underlying {@link ReflectiveProcessor} implementations.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private final Map<Class<? extends ReflectiveProcessor>, ReflectiveProcessor> processors = new HashMap<>();

	@Nullable
	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		Set<Entry> entries = new LinkedHashSet<>();
		processType(entries, beanClass);
		for (Class<?> implementedInterface : ClassUtils.getAllInterfacesForClass(beanClass)) {
			processType(entries, implementedInterface);
		}
		if (!entries.isEmpty()) {
			return new ReflectiveProcessorBeanRegistrationAotContribution(entries);
		}
		return null;
	}

	private void processType(Set<Entry> entries, Class<?> typeToProcess) {
		if (isReflective(typeToProcess)) {
			entries.add(createEntry(typeToProcess));
		}
		doWithReflectiveConstructors(typeToProcess, constructor ->
				entries.add(createEntry(constructor)));
		ReflectionUtils.doWithFields(typeToProcess, field ->
				entries.add(createEntry(field)), this::isReflective);
		ReflectionUtils.doWithMethods(typeToProcess, method ->
				entries.add(createEntry(method)), this::isReflective);
	}

	private void doWithReflectiveConstructors(Class<?> typeToProcess, Consumer<Constructor<?>> consumer) {
		for (Constructor<?> constructor : typeToProcess.getDeclaredConstructors()) {
			if (isReflective(constructor)) {
				consumer.accept(constructor);
			}
		}
	}

	private boolean isReflective(AnnotatedElement element) {
		return MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).isPresent(Reflective.class);
	}

	@SuppressWarnings("unchecked")
	private Entry createEntry(AnnotatedElement element) {
		Class<? extends ReflectiveProcessor>[] processorClasses = (Class<? extends ReflectiveProcessor>[])
				MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Reflective.class).getClassArray("value");
		List<ReflectiveProcessor> processors = Arrays.stream(processorClasses).distinct()
				.map(processorClass -> this.processors.computeIfAbsent(processorClass, BeanUtils::instantiateClass))
				.toList();
		ReflectiveProcessor processorToUse = (processors.size() == 1 ? processors.get(0)
				: new DelegateReflectiveProcessor(processors));
		return new Entry(element, processorToUse);
	}

	static class DelegateReflectiveProcessor implements ReflectiveProcessor {

		private final Iterable<ReflectiveProcessor> processors;

		public DelegateReflectiveProcessor(Iterable<ReflectiveProcessor> processors) {
			this.processors = processors;
		}

		@Override
		public void registerReflectionHints(ReflectionHints hints, AnnotatedElement element) {
			this.processors.forEach(processor -> processor.registerReflectionHints(hints, element));
		}

	}

	private record Entry(AnnotatedElement element, ReflectiveProcessor processor) {}

	private static class ReflectiveProcessorBeanRegistrationAotContribution implements BeanRegistrationAotContribution {

		private final Iterable<Entry> entries;

		public ReflectiveProcessorBeanRegistrationAotContribution(Iterable<Entry> entries) {
			this.entries = entries;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
			RuntimeHints runtimeHints = generationContext.getRuntimeHints();
			this.entries.forEach(entry -> {
				AnnotatedElement element = entry.element();
				entry.processor().registerReflectionHints(runtimeHints.reflection(), element);
				registerAnnotationIfNecessary(runtimeHints, element);
			});
		}

		private void registerAnnotationIfNecessary(RuntimeHints hints, AnnotatedElement element) {
			MergedAnnotation<Reflective> reflectiveAnnotation = MergedAnnotations.from(element).get(Reflective.class);
			if (reflectiveAnnotation.getDistance() > 0) {
				RuntimeHintsUtils.registerAnnotation(hints, reflectiveAnnotation.getRoot().getType());
			}
		}

	}

}
