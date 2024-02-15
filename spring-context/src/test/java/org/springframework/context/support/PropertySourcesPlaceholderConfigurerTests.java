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

package org.springframework.context.support;

import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.testfixture.env.MockPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Tests for {@link PropertySourcesPlaceholderConfigurer}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
class PropertySourcesPlaceholderConfigurerTests {

	@Test
	void replacementFromEnvironmentProperties() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MockEnvironment env = new MockEnvironment();
		env.setProperty("my.name", "myValue");

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setEnvironment(env);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
		assertThat(ppc.getAppliedPropertySources()).isNotNull();
	}

	@Test
	void localPropertiesViaResource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		Resource resource = new ClassPathResource("PropertySourcesPlaceholderConfigurerTests.properties", this.getClass());
		ppc.setLocation(resource);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
	}

	@Test
	void localPropertiesOverrideFalse() {
		localPropertiesOverride(false);
	}

	@Test
	void localPropertiesOverrideTrue() {
		localPropertiesOverride(true);
	}

	@Test
	void explicitPropertySources() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MockPropertySource().withProperty("my.name", "foo"));

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setPropertySources(propertySources);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
		assertThat(propertySources).containsExactlyElementsOf(ppc.getAppliedPropertySources());
	}

	@Test
	void explicitPropertySourcesExcludesEnvironment() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MockPropertySource());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setPropertySources(propertySources);
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "env"));
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
		assertThat(propertySources).containsExactlyElementsOf(ppc.getAppliedPropertySources());
	}

	@Test
	@SuppressWarnings("serial")
	public void explicitPropertySourcesExcludesLocalProperties() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addLast(new MockPropertySource());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setPropertySources(propertySources);
		ppc.setProperties(new Properties() {{
			put("my.name", "local");
		}});
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
	}

	@Test
	void ignoreUnresolvablePlaceholders_falseIsDefault() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		//pc.setIgnoreUnresolvablePlaceholders(false); // the default
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
			.isThrownBy(() -> ppc.postProcessBeanFactory(bf))
			.havingCause()
				.isExactlyInstanceOf(PlaceholderResolutionException.class)
				.withMessage("Could not resolve placeholder 'my.name' in value \"${my.name}\"");
	}

	@Test
	void ignoreUnresolvablePlaceholders_true() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${my.name}");
	}

	@Test
	// https://github.com/spring-projects/spring-framework/issues/27947
	public void ignoreUnresolvablePlaceholdersInAtValueAnnotation__falseIsDefault() {
		MockPropertySource mockPropertySource = new MockPropertySource("test");
		mockPropertySource.setProperty("my.key", "${enigma}");
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addLast(mockPropertySource);
		context.register(IgnoreUnresolvablePlaceholdersFalseConfig.class);

		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(context::refresh)
			.havingCause()
				.isExactlyInstanceOf(PlaceholderResolutionException.class)
				.withMessage("Could not resolve placeholder 'enigma' in value \"${enigma}\" <-- \"${my.key}\"");
	}

	@Test
	// https://github.com/spring-projects/spring-framework/issues/27947
	public void ignoreUnresolvablePlaceholdersInAtValueAnnotation_true() {
		MockPropertySource mockPropertySource = new MockPropertySource("test");
		mockPropertySource.setProperty("my.key", "${enigma}");
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addLast(mockPropertySource);
		context.register(IgnoreUnresolvablePlaceholdersTrueConfig.class);
		context.refresh();

		IgnoreUnresolvablePlaceholdersTrueConfig config = context.getBean(IgnoreUnresolvablePlaceholdersTrueConfig.class);
		assertThat(config.value).isEqualTo("${enigma}");
	}

	@Test
	@SuppressWarnings("serial")
	public void nestedUnresolvablePlaceholder() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
						.addPropertyValue("name", "${my.name}")
						.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setProperties(new Properties() {{
			put("my.name", "${bogus}");
		}});
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				ppc.postProcessBeanFactory(bf));
	}

	@Test
	@SuppressWarnings("serial")
	public void ignoredNestedUnresolvablePlaceholder() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
						.addPropertyValue("name", "${my.name}")
						.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setProperties(new Properties() {{
			put("my.name", "${bogus}");
		}});
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${bogus}");
	}

	@Test
	void withNonEnumerablePropertySource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${foo}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

		PropertySource<?> ps = new PropertySource<>("simplePropertySource", new Object()) {
			@Override
			public Object getProperty(String key) {
				return "bar";
			}
		};

		MockEnvironment env = new MockEnvironment();
		env.getPropertySources().addFirst(ps);
		ppc.setEnvironment(env);

		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("bar");
	}

	@SuppressWarnings("serial")
	private void localPropertiesOverride(boolean override) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${foo}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

		ppc.setLocalOverride(override);
		ppc.setProperties(new Properties() {{
				setProperty("foo", "local");
		}});
		ppc.setEnvironment(new MockEnvironment().withProperty("foo", "enclosing"));
		ppc.postProcessBeanFactory(bf);
		if (override) {
			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("local");
		}
		else {
			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("enclosing");
		}
	}

	@Test
	void customPlaceholderPrefixAndSuffix() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setPlaceholderPrefix("@<");
		ppc.setPlaceholderSuffix(">");

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "@<key1>")
				.addPropertyValue("sex", "${key2}")
				.getBeanDefinition());

		System.setProperty("key1", "systemKey1Value");
		System.setProperty("key2", "systemKey2Value");
		ppc.setEnvironment(new StandardEnvironment());
		ppc.postProcessBeanFactory(bf);
		System.clearProperty("key1");
		System.clearProperty("key2");

		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("systemKey1Value");
		assertThat(bf.getBean(TestBean.class).getSex()).isEqualTo("${key2}");
	}

	@Test
	void nullValueIsPreserved() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setNullValue("customNull");
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "customNull"));
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isNull();
	}

	@Test
	void trimValuesIsOffByDefault() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo(" myValue  ");
	}

	@Test
	void trimValuesIsApplied() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setTrimValues(true);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
	}

	@Test
	void getAppliedPropertySourcesTooEarly() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		assertThatIllegalStateException().isThrownBy(
				ppc::getAppliedPropertySources);
	}

	@Test
	void multipleLocationsWithDefaultResolvedValue() {
		// SPR-10619
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ClassPathResource doesNotHave = new ClassPathResource("test.properties", getClass());
		ClassPathResource setToTrue = new ClassPathResource("placeholder.properties", getClass());
		ppc.setLocations(doesNotHave, setToTrue);
		ppc.setIgnoreResourceNotFound(true);
		ppc.setIgnoreUnresolvablePlaceholders(true);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("jedi", "${jedi:false}")
					.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).isJedi()).isTrue();
	}

	@Test
	void optionalPropertyWithValue() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setConversionService(new DefaultConversionService());
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(OptionalTestBean.class)
						.addPropertyValue("name", "${my.name}")
						.getBeanDefinition());

		MockEnvironment env = new MockEnvironment();
		env.setProperty("my.name", "myValue");

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setEnvironment(env);
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(OptionalTestBean.class).getName()).contains("myValue");
	}

	@Test
	void optionalPropertyWithoutValue() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setConversionService(new DefaultConversionService());
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(OptionalTestBean.class)
						.addPropertyValue("name", "${my.name}")
						.getBeanDefinition());

		MockEnvironment env = new MockEnvironment();
		env.setProperty("my.name", "");

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setEnvironment(env);
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.setNullValue("");
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(OptionalTestBean.class).getName()).isNotPresent();
	}


	private static class OptionalTestBean {

		private Optional<String> name;

		public Optional<String> getName() {
			return name;
		}

		@SuppressWarnings("unused")
		public void setName(Optional<String> name) {
			this.name = name;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class IgnoreUnresolvablePlaceholdersFalseConfig {

		@Value("${my.key}")
		String value;

		@Bean
		static PropertySourcesPlaceholderConfigurer pspc() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class IgnoreUnresolvablePlaceholdersTrueConfig {

		@Value("${my.key}")
		String value;

		@Bean
		static PropertySourcesPlaceholderConfigurer pspc() {
			PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
			pspc.setIgnoreUnresolvablePlaceholders(true);
			return pspc;
		}
	}

}
