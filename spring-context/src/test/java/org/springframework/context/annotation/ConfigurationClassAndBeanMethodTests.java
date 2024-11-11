/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConfigurationClassParser}, {@link ConfigurationClass},
 * and {@link BeanMethod}.
 *
 * @author Sam Brannen
 * @since 5.3.9
 */
class ConfigurationClassAndBeanMethodTests {

	@Test
	void verifyEquals() throws Exception {
		ConfigurationClass configurationClass1 = newConfigurationClass(Config1.class);
		ConfigurationClass configurationClass2 = newConfigurationClass(Config1.class);
		ConfigurationClass configurationClass3 = newConfigurationClass(Config2.class);

		assertThat(configurationClass1).isNotEqualTo(null);
		assertThat(configurationClass1).isNotSameAs(configurationClass2);

		assertThat(configurationClass1).isEqualTo(configurationClass1);
		assertThat(configurationClass2).isEqualTo(configurationClass2);
		assertThat(configurationClass1).isEqualTo(configurationClass2);
		assertThat(configurationClass2).isEqualTo(configurationClass1);

		assertThat(configurationClass1).isNotEqualTo(configurationClass3);
		assertThat(configurationClass3).isNotEqualTo(configurationClass2);

		// ---------------------------------------------------------------------

		List<BeanMethod> beanMethods1 = getBeanMethods(configurationClass1);
		BeanMethod beanMethod_1_0 = beanMethods1.get(0);
		BeanMethod beanMethod_1_1 = beanMethods1.get(1);
		BeanMethod beanMethod_1_2 = beanMethods1.get(2);

		List<BeanMethod> beanMethods2 = getBeanMethods(configurationClass2);
		BeanMethod beanMethod_2_0 = beanMethods2.get(0);
		BeanMethod beanMethod_2_1 = beanMethods2.get(1);
		BeanMethod beanMethod_2_2 = beanMethods2.get(2);

		List<BeanMethod> beanMethods3 = getBeanMethods(configurationClass3);
		BeanMethod beanMethod_3_0 = beanMethods3.get(0);
		BeanMethod beanMethod_3_1 = beanMethods3.get(1);
		BeanMethod beanMethod_3_2 = beanMethods3.get(2);

		assertThat(beanMethod_1_0).isNotEqualTo(null);
		assertThat(beanMethod_1_0).isNotSameAs(beanMethod_2_0);

		assertThat(beanMethod_1_0).isEqualTo(beanMethod_1_0);
		assertThat(beanMethod_1_0).isEqualTo(beanMethod_2_0);
		assertThat(beanMethod_1_1).isEqualTo(beanMethod_2_1);
		assertThat(beanMethod_1_2).isEqualTo(beanMethod_2_2);

		assertThat(beanMethod_1_0.getMetadata().getMethodName()).isEqualTo(beanMethod_3_0.getMetadata().getMethodName());
		assertThat(beanMethod_1_0).isNotEqualTo(beanMethod_3_0);
		assertThat(beanMethod_1_1).isNotEqualTo(beanMethod_3_1);
		assertThat(beanMethod_1_2).isNotEqualTo(beanMethod_3_2);
	}

	@Test
	void verifyHashCode() throws Exception {
		ConfigurationClass configurationClass1 = newConfigurationClass(Config1.class);
		ConfigurationClass configurationClass2 = newConfigurationClass(Config1.class);
		ConfigurationClass configurationClass3 = newConfigurationClass(Config2.class);

		assertThat(configurationClass1).hasSameHashCodeAs(configurationClass2);
		assertThat(configurationClass1).doesNotHaveSameHashCodeAs(configurationClass3);

		// ---------------------------------------------------------------------

		List<BeanMethod> beanMethods1 = getBeanMethods(configurationClass1);
		BeanMethod beanMethod_1_0 = beanMethods1.get(0);
		BeanMethod beanMethod_1_1 = beanMethods1.get(1);
		BeanMethod beanMethod_1_2 = beanMethods1.get(2);

		List<BeanMethod> beanMethods2 = getBeanMethods(configurationClass2);
		BeanMethod beanMethod_2_0 = beanMethods2.get(0);
		BeanMethod beanMethod_2_1 = beanMethods2.get(1);
		BeanMethod beanMethod_2_2 = beanMethods2.get(2);

		List<BeanMethod> beanMethods3 = getBeanMethods(configurationClass3);
		BeanMethod beanMethod_3_0 = beanMethods3.get(0);
		BeanMethod beanMethod_3_1 = beanMethods3.get(1);
		BeanMethod beanMethod_3_2 = beanMethods3.get(2);

		assertThat(beanMethod_1_0).hasSameHashCodeAs(beanMethod_2_0);
		assertThat(beanMethod_1_1).hasSameHashCodeAs(beanMethod_2_1);
		assertThat(beanMethod_1_2).hasSameHashCodeAs(beanMethod_2_2);

		assertThat(beanMethod_1_0).doesNotHaveSameHashCodeAs(beanMethod_3_0);
		assertThat(beanMethod_1_1).doesNotHaveSameHashCodeAs(beanMethod_3_1);
		assertThat(beanMethod_1_2).doesNotHaveSameHashCodeAs(beanMethod_3_2);
	}

	@Test
	void verifyToString() throws Exception {
		ConfigurationClass configurationClass = newConfigurationClass(Config1.class);
		assertThat(configurationClass.toString())
			.startsWith("ConfigurationClass: beanName 'Config1', class path resource");

		List<BeanMethod> beanMethods = getBeanMethods(configurationClass);
		String prefix = "BeanMethod: " + Config1.class.getName();
		assertThat(beanMethods.get(0).toString()).isEqualTo(prefix + ".bean0()");
		assertThat(beanMethods.get(1).toString()).isEqualTo(prefix + ".bean1(java.lang.String)");
		assertThat(beanMethods.get(2).toString()).isEqualTo(prefix + ".bean2(java.lang.String,java.lang.Integer)");
	}


	private static ConfigurationClass newConfigurationClass(Class<?> clazz) throws Exception {
		ConfigurationClassParser parser = newParser();
		parser.parse(clazz.getName(), clazz.getSimpleName());
		assertThat(parser.getConfigurationClasses()).hasSize(1);
		return parser.getConfigurationClasses().iterator().next();
	}

	private static ConfigurationClassParser newParser() {
		return new ConfigurationClassParser(
				new CachingMetadataReaderFactory(),
				new FailFastProblemReporter(),
				new StandardEnvironment(),
				new DefaultResourceLoader(),
				new AnnotationBeanNameGenerator(),
				new DefaultListableBeanFactory());
	}

	private static List<BeanMethod> getBeanMethods(ConfigurationClass configurationClass) {
		List<BeanMethod> beanMethods = configurationClass.getBeanMethods().stream()
				.sorted(Comparator.comparing(beanMethod -> beanMethod.getMetadata().getMethodName()))
				.toList();
		assertThat(beanMethods).hasSize(3);
		return beanMethods;
	}

	static class Config1 {

		@Bean
		String bean0() {
			return "";
		}

		@Bean
		String bean1(String text) {
			return "";
		}

		@Bean
		String bean2(String text, Integer num) {
			return "";
		}

	}

	static class Config2 {

		@Bean
		String bean0() {
			return "";
		}

		@Bean
		String bean1(String text) {
			return "";
		}

		@Bean
		String bean2(String text, Integer num) {
			return "";
		}

	}

}
