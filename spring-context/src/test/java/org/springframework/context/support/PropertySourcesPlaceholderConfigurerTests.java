/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.AbstractPropertyResolver;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.testfixture.env.MockPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.PlaceholderResolutionException;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.core.env.AbstractPropertyResolver.DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME;

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

	/**
	 * Ensure that a {@link Converter} registered in the {@link ConversionService}
	 * used by the {@code Environment} is applied during placeholder resolution
	 * against a {@link PropertySource} registered in the {@code Environment}.
	 */
	@Test  // gh-34936
	void replacementFromEnvironmentPropertiesWithConversion() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		record Point(int x, int y) {
		}

		Converter<Point, String> pointToStringConverter =
				point -> "(%d,%d)".formatted(point.x, point.y);

		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(Point.class, String.class, pointToStringConverter);

		MockEnvironment env = new MockEnvironment();
		env.setConversionService(conversionService);
		env.setProperty("my.name", new Point(4,5));

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setEnvironment(env);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("(4,5)");
	}

	/**
	 * Ensure that a {@link PropertySource} added to the {@code Environment} after context
	 * refresh (i.e., after {@link PropertySourcesPlaceholderConfigurer#postProcessBeanFactory()}
	 * has been invoked) can still contribute properties in late-binding scenarios.
	 */
	@Test  // gh-34861
	void replacementFromEnvironmentPropertiesWithLateBinding() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
		propertySources.addFirst(new MockPropertySource("early properties").withProperty("foo", "bar"));

		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.register(PrototypeBean.class);
		context.refresh();

		// Verify that placeholder resolution works for early binding.
		PrototypeBean prototypeBean = context.getBean(PrototypeBean.class);
		assertThat(prototypeBean.getName()).isEqualTo("bar");
		assertThat(prototypeBean.isJedi()).isFalse();

		// Add new PropertySource after context refresh.
		propertySources.addFirst(new MockPropertySource("late properties").withProperty("jedi", "true"));

		// Verify that placeholder resolution works for late binding: isJedi() switches to true.
		prototypeBean = context.getBean(PrototypeBean.class);
		assertThat(prototypeBean.getName()).isEqualTo("bar");
		assertThat(prototypeBean.isJedi()).isTrue();

		// Add yet another PropertySource after context refresh.
		propertySources.addFirst(new MockPropertySource("even later properties").withProperty("foo", "enigma"));

		// Verify that placeholder resolution works for even later binding: getName() switches to enigma.
		prototypeBean = context.getBean(PrototypeBean.class);
		assertThat(prototypeBean.getName()).isEqualTo("enigma");
		assertThat(prototypeBean.isJedi()).isTrue();
	}

	@Test
	void localPropertiesViaResource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		Resource resource = new ClassPathResource("PropertySourcesPlaceholderConfigurerTests.properties", getClass());
		ppc.setLocation(resource);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("foo");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void localPropertiesOverride(boolean override) {
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

	@Test  // gh-34861
	void withEnumerableAndNonEnumerablePropertySourcesInTheEnvironmentAndLocalProperties() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${foo:bogus}")
					.addPropertyValue("jedi", "${local:false}")
					.getBeanDefinition());

		// 1) MockPropertySource is an EnumerablePropertySource.
		MockPropertySource mockPropertySource = new MockPropertySource("mockPropertySource")
				.withProperty("foo", "${bar}");

		// 2) PropertySource is not an EnumerablePropertySource.
		PropertySource<?> rawPropertySource = new PropertySource<>("rawPropertySource", new Object()) {
			@Override
			public Object getProperty(String key) {
				return ("bar".equals(key) ? "quux" : null);
			}
		};

		MockEnvironment env = new MockEnvironment();
		env.getPropertySources().addFirst(mockPropertySource);
		env.getPropertySources().addLast(rawPropertySource);

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setEnvironment(env);
		// 3) Local properties are stored in a PropertiesPropertySource which is an EnumerablePropertySource.
		ppc.setProperties(new Properties() {{
			setProperty("local", "true");
		}});
		ppc.postProcessBeanFactory(bf);

		// Verify all properties can be resolved via the Environment.
		assertThat(env.getProperty("foo")).isEqualTo("quux");
		assertThat(env.getProperty("bar")).isEqualTo("quux");

		// Verify that placeholder resolution works.
		TestBean testBean = bf.getBean(TestBean.class);
		assertThat(testBean.getName()).isEqualTo("quux");
		assertThat(testBean.isJedi()).isTrue();

		// Verify that the presence of a non-EnumerablePropertySource does not prevent
		// accessing EnumerablePropertySources via getAppliedPropertySources().
		List<String> propertyNames = new ArrayList<>();
		for (PropertySource<?> propertySource : ppc.getAppliedPropertySources()) {
			if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
				Collections.addAll(propertyNames, enumerablePropertySource.getPropertyNames());
			}
		}
		// Should not contain "foo" or "bar" from the Environment.
		assertThat(propertyNames).containsOnly("local");
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


	/**
	 * Tests that use the escape character (or disable it) with nested placeholder
	 * resolution.
	 */
	@Nested
	class EscapedNestedPlaceholdersTests {

		@Test  // gh-34861
		void singleEscapeWithDefaultEscapeCharacter() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin")
					.withProperty("my.property", "\\DOMAIN\\${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.postProcessBeanFactory(bf);

			// \DOMAIN\${user.home} resolves to \DOMAIN${user.home} instead of \DOMAIN\admin
			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN${user.home}");
		}

		@Test  // gh-34861
		void singleEscapeWithCustomEscapeCharacter() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin\\~${nested}")
					.withProperty("my.property", "DOMAIN\\${user.home}\\~${enigma}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			// Set custom escape character.
			ppc.setEscapeCharacter('~');
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN\\admin\\${nested}\\${enigma}");
		}

		@Test  // gh-34861
		void singleEscapeWithEscapeCharacterDisabled() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin\\")
					.withProperty("my.property", "\\DOMAIN\\${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			// Disable escape character.
			ppc.setEscapeCharacter(null);
			ppc.postProcessBeanFactory(bf);

			// \DOMAIN\${user.home} resolves to \DOMAIN\admin
			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN\\admin\\");
		}

		@Test  // gh-34861
		void tripleEscapeWithDefaultEscapeCharacter() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin\\\\\\")
					.withProperty("my.property", "DOMAIN\\\\\\${user.home}#${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN\\\\${user.home}#admin\\\\\\");
		}

		@Test  // gh-34861
		void tripleEscapeWithCustomEscapeCharacter() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin\\~${enigma}")
					.withProperty("my.property", "DOMAIN~~~${user.home}#${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			// Set custom escape character.
			ppc.setEscapeCharacter('~');
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN~~${user.home}#admin\\${enigma}");
		}

		@Test  // gh-34861
		void singleEscapeWithDefaultEscapeCharacterAndIgnoreUnresolvablePlaceholders() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "${enigma}")
					.withProperty("my.property", "\\${DOMAIN}${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.setIgnoreUnresolvablePlaceholders(true);
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${DOMAIN}${enigma}");
		}

		@Test  // gh-34861
		void singleEscapeWithCustomEscapeCharacterAndIgnoreUnresolvablePlaceholders() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "${enigma}")
					.withProperty("my.property", "~${DOMAIN}\\${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			// Set custom escape character.
			ppc.setEscapeCharacter('~');
			ppc.setIgnoreUnresolvablePlaceholders(true);
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("${DOMAIN}\\${enigma}");
		}

		@Test  // gh-34861
		void tripleEscapeWithDefaultEscapeCharacterAndIgnoreUnresolvablePlaceholders() {
			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "${enigma}")
					.withProperty("my.property", "X:\\\\\\${DOMAIN}${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.setIgnoreUnresolvablePlaceholders(true);
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("X:\\\\${DOMAIN}${enigma}");
		}

		private static DefaultListableBeanFactory createBeanFactory() {
			BeanDefinition beanDefinition = genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.property}")
					.getBeanDefinition();
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			bf.registerBeanDefinition("testBean",beanDefinition);
			return bf;
		}

	}


	/**
	 * Tests that globally set the default escape character (or disable it) and
	 * rely on nested placeholder resolution.
	 */
	@Nested
	class GlobalDefaultEscapeCharacterTests {

		private static final Field defaultEscapeCharacterField =
				ReflectionUtils.findField(AbstractPropertyResolver.class, "defaultEscapeCharacter");

		static {
			ReflectionUtils.makeAccessible(defaultEscapeCharacterField);
		}


		@BeforeEach
		void resetStateBeforeEachTest() {
			resetState();
		}

		@AfterAll
		static void resetState() {
			ReflectionUtils.setField(defaultEscapeCharacterField, null, Character.MIN_VALUE);
			setSpringProperty(null);
		}


		@Test  // gh-34865
		void defaultEscapeCharacterSetToXyz() {
			setSpringProperty("XYZ");

			assertThatIllegalArgumentException()
					.isThrownBy(PropertySourcesPlaceholderConfigurer::new)
					.withMessage("Value [XYZ] for property [%s] must be a single character or an empty string",
							DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME);
		}

		@Test  // gh-34865
		void defaultEscapeCharacterDisabled() {
			setSpringProperty("");

			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin")
					.withProperty("my.property", "\\DOMAIN\\${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN\\admin");
		}

		@Test  // gh-34865
		void defaultEscapeCharacterSetToBackslash() {
			setSpringProperty("\\");

			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin")
					.withProperty("my.property", "\\DOMAIN\\${user.home}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.postProcessBeanFactory(bf);

			// \DOMAIN\${user.home} resolves to \DOMAIN${user.home} instead of \DOMAIN\admin
			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("\\DOMAIN${user.home}");
		}

		@Test  // gh-34865
		void defaultEscapeCharacterSetToTilde() {
			setSpringProperty("~");

			MockEnvironment env = new MockEnvironment()
					.withProperty("user.home", "admin\\~${nested}")
					.withProperty("my.property", "DOMAIN\\${user.home}\\~${enigma}");

			DefaultListableBeanFactory bf = createBeanFactory();
			PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
			ppc.setEnvironment(env);
			ppc.postProcessBeanFactory(bf);

			assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("DOMAIN\\admin\\${nested}\\${enigma}");
		}

		private static void setSpringProperty(String value) {
			SpringProperties.setProperty(DEFAULT_PLACEHOLDER_ESCAPE_CHARACTER_PROPERTY_NAME, value);
		}

		private static DefaultListableBeanFactory createBeanFactory() {
			BeanDefinition beanDefinition = genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.property}")
					.getBeanDefinition();
			DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
			bf.registerBeanDefinition("testBean",beanDefinition);
			return bf;
		}

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

	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	static class PrototypeBean {

		@Value("${foo:bogus}")
		private String name;

		@Value("${jedi:false}")
		private boolean jedi;


		public String getName() {
			return this.name;
		}

		public boolean isJedi() {
			return this.jedi;
		}
	}

}
