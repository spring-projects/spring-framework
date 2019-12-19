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

package org.springframework.context.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationClassParser}'s parsing order of {@link DeferredImportSelector.Group}.
 *
 * @author Tadaya Tsuyukubo
 */
class DeferredImportSelectorGroupOrderingTests {

	private ConfigurationClassParser parser;

	@BeforeEach
	void beforeEach() {
		this.parser = new ConfigurationClassParser(
				new CachingMetadataReaderFactory(), new FailFastProblemReporter(), new StandardEnvironment(),
				new DefaultResourceLoader(), new AnnotationBeanNameGenerator(), new DefaultListableBeanFactory());
	}

	@Test
	void sameGroupDifferentOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithSameGroupDifferentOrder.class);
		assertThat(importingClassNames)
				.as("@Order will take precedence")
				.containsExactly(
						ClassUtils.getShortName(ImportConfigWithSameGroupDifferentOrder.class),
						ClassUtils.getShortName(MyConfigA.class),
						ClassUtils.getShortName(MyConfigB.class),
						ClassUtils.getShortName(MyConfigSameGroupDifferentOrder.class)
				);
	}

	@Test
	void sameGroupSameOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithSameGroupSameOrder.class);
		assertThat(importingClassNames).hasSize(4);

		String first = importingClassNames.get(0);
		String second = importingClassNames.get(1);
		String third = importingClassNames.get(2);
		String last = importingClassNames.get(3);

		assertThat(first).isEqualTo(ClassUtils.getShortName(ImportConfigWithSameGroupSameOrder.class));
		assertThat(Arrays.asList(second, third))
				.as("Same-group-with-same-order are processed at same level")
				.containsExactlyInAnyOrder(
						ClassUtils.getShortName(MyConfigSameGroupSameOrder.class),
						ClassUtils.getShortName(MyConfigA.class)
				);
		assertThat(last).isEqualTo(ClassUtils.getShortName(MyConfigB.class));
	}

	@Test
	void sameGroupNoOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithSameGroupNoOrder.class);
		assertThat(importingClassNames)
				.as("No @Order should be always last")
				.containsExactly(
						ClassUtils.getShortName(ImportConfigWithSameGroupNoOrder.class),
						ClassUtils.getShortName(MyConfigA.class),
						ClassUtils.getShortName(MyConfigB.class),
						ClassUtils.getShortName(MyConfigSameGroupNoOrder.class)
				);
	}

	@Test
	void differentGroupDifferentOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithDifferentGroupDifferentOrder.class);
		assertThat(importingClassNames)
				.as("No @Order should be always last")
				.containsExactly(
						ClassUtils.getShortName(ImportConfigWithDifferentGroupDifferentOrder.class),
						ClassUtils.getShortName(MyConfigA.class),
						ClassUtils.getShortName(MyConfigB.class),
						ClassUtils.getShortName(MyConfigDifferentGroupDifferentOrder.class)
				);
	}

	@Test
	void differentGroupSameOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithDifferentGroupSameOrder.class);
		assertThat(importingClassNames).hasSize(4);

		String first = importingClassNames.get(0);
		String second = importingClassNames.get(1);
		String third = importingClassNames.get(2);
		String last = importingClassNames.get(3);

		assertThat(first).isEqualTo(ClassUtils.getShortName(ImportConfigWithDifferentGroupSameOrder.class));
		assertThat(Arrays.asList(second, third))
				.as("Different-group-with-same-order are processed at same level")
				.containsExactlyInAnyOrder(
						ClassUtils.getShortName(MyConfigDifferentGroupSameOrder.class),
						ClassUtils.getShortName(MyConfigA.class)
				);
		assertThat(last).isEqualTo(ClassUtils.getShortName(MyConfigB.class));
	}

	@Test
	void differentGroupNoOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithDifferentGroupNoOrder.class);
		assertThat(importingClassNames)
				.as("No @Order should be always last")
				.containsExactly(
						ClassUtils.getShortName(ImportConfigWithDifferentGroupNoOrder.class),
						ClassUtils.getShortName(MyConfigA.class),
						ClassUtils.getShortName(MyConfigB.class),
						ClassUtils.getShortName(MyConfigDifferentGroupNoOrder.class)
				);
	}

	@Test
	void noGroupDifferentOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithNoGroupDifferentOrder.class);
		assertThat(importingClassNames)
				.as("No @Order should be always last")
				.containsExactly(
						ClassUtils.getShortName(ImportConfigWithNoGroupDifferentOrder.class),
						ClassUtils.getShortName(MyConfigA.class),
						ClassUtils.getShortName(MyConfigB.class),
						ClassUtils.getShortName(MyConfigNoGroupDifferentOrder.class)
				);
	}

	@Test
	void noGroupSameOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithNoGroupSameOrder.class);
		assertThat(importingClassNames).hasSize(4);

		String first = importingClassNames.get(0);
		String second = importingClassNames.get(1);
		String third = importingClassNames.get(2);
		String last = importingClassNames.get(3);

		assertThat(first).isEqualTo(ClassUtils.getShortName(ImportConfigWithNoGroupSameOrder.class));
		assertThat(Arrays.asList(second, third))
				.as("No-group-with-same-order are processed at same level")
				.containsExactlyInAnyOrder(
						ClassUtils.getShortName(MyConfigNoGroupSameOrder.class),
						ClassUtils.getShortName(MyConfigA.class)
				);
		assertThat(last).isEqualTo(ClassUtils.getShortName(MyConfigB.class));
	}

	@Test
	void noGroupNoOrder() {
		List<String> importingClassNames = parseAndGetConfigurationClassNames(ImportConfigWithNoGroupNoOrder.class);
		assertThat(importingClassNames)
				.as("No @Order should be always last")
				.containsExactly(
						ClassUtils.getShortName(ImportConfigWithNoGroupNoOrder.class),
						ClassUtils.getShortName(MyConfigA.class),
						ClassUtils.getShortName(MyConfigB.class),
						ClassUtils.getShortName(MyConfigNoGroupNoOrder.class)
				);
	}

	private List<String> parseAndGetConfigurationClassNames(Class<?> configClass) {
		AnnotatedGenericBeanDefinition beanDef = new AnnotatedGenericBeanDefinition(configClass);
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(beanDef, "importConfig");
		this.parser.parse(Collections.singleton(definitionHolder));

		// this is LinkedKeySet
		Set<ConfigurationClass> configurationClasses = this.parser.getConfigurationClasses();

		return configurationClasses.stream()
				.map(ConfigurationClass::getMetadata)
				.map(ClassMetadata::getClassName)
				.map(ClassUtils::getShortName)
				.collect(Collectors.toList());

	}

	private static abstract class ParentGroup implements DeferredImportSelector.Group {
		private final List<Entry> imports = new ArrayList<>();

		@Override
		public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
			for (String importClassName : selector.selectImports(metadata)) {
				this.imports.add(new Entry(metadata, importClassName));
			}
		}

		@Override
		public Iterable<Entry> selectImports() {
			return this.imports;
		}
	}

	static class GroupA extends ParentGroup {
	}

	static class GroupB extends ParentGroup {
	}

	static class GroupC extends ParentGroup {
	}


	@Order(10)
	static class DeferredImportSelectorA implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigA.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupA.class;
		}
	}

	@Order(20)
	static class DeferredImportSelectorB implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigB.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupB.class;
		}
	}


	@Order(100)  // different order
	static class DeferredImportSelectorSameGroupDifferentOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigSameGroupDifferentOrder.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupA.class;  // same as selector-A
		}
	}

	@Order(10)  // same as selector-A
	static class DeferredImportSelectorSameGroupSameOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigSameGroupSameOrder.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupA.class; // same as selector-A
		}
	}


	static class DeferredImportSelectorSameGroupNoOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigSameGroupNoOrder.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupA.class; // same as selector-A
		}
	}

	@Order(100)  // different order
	static class DeferredImportSelectorDifferentGroupDifferentOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigDifferentGroupDifferentOrder.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupC.class;  // different group
		}
	}

	@Order(10)  // same as selector-A
	static class DeferredImportSelectorDifferentGroupSameOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigDifferentGroupSameOrder.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupC.class;  // different group
		}
	}

	static class DeferredImportSelectorDifferentGroupNoOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigDifferentGroupNoOrder.class.getName()};
		}

		@Override
		public Class<? extends Group> getImportGroup() {
			return GroupC.class;  // different group
		}
	}


	@Order(100)  // different order
	static class DeferredImportSelectorNoGroupDifferentOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigNoGroupDifferentOrder.class.getName()};
		}
	}

	@Order(10)  // same as selector-A
	static class DeferredImportSelectorNoGroupSameOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigNoGroupSameOrder.class.getName()};
		}
	}

	static class DeferredImportSelectorNoGroupNoOrder implements DeferredImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return new String[]{MyConfigNoGroupNoOrder.class.getName()};
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class MyConfigA {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigB {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigSameGroupDifferentOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigSameGroupSameOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigSameGroupNoOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigDifferentGroupDifferentOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigDifferentGroupSameOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigDifferentGroupNoOrder {

	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigNoGroupDifferentOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigNoGroupSameOrder {
	}

	@Configuration(proxyBeanMethods = false)
	static class MyConfigNoGroupNoOrder {
	}


	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorSameGroupDifferentOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithSameGroupDifferentOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorSameGroupSameOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithSameGroupSameOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorSameGroupNoOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithSameGroupNoOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorDifferentGroupDifferentOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithDifferentGroupDifferentOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorDifferentGroupSameOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithDifferentGroupSameOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorDifferentGroupNoOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithDifferentGroupNoOrder {
	}


	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorNoGroupDifferentOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithNoGroupDifferentOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorNoGroupSameOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithNoGroupSameOrder {
	}

	@Configuration(proxyBeanMethods = false)
	@Import({DeferredImportSelectorNoGroupNoOrder.class, DeferredImportSelectorA.class, DeferredImportSelectorB.class})
	static class ImportConfigWithNoGroupNoOrder {
	}

}
