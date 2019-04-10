/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.tests.sample.beans.TestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class PropertySourcesPlaceholderConfigurerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void replacementFromEnvironmentProperties() {
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
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("myValue"));
		assertThat(ppc.getAppliedPropertySources(), not(nullValue()));
	}

	@Test
	public void localPropertiesViaResource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		Resource resource = new ClassPathResource("PropertySourcesPlaceholderConfigurerTests.properties", this.getClass());
		ppc.setLocation(resource);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("foo"));
	}

	@Test
	public void localPropertiesOverrideFalse() {
		localPropertiesOverride(false);
	}

	@Test
	public void localPropertiesOverrideTrue() {
		localPropertiesOverride(true);
	}

	@Test
	public void explicitPropertySources() {
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
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("foo"));
		assertEquals(ppc.getAppliedPropertySources().iterator().next(), propertySources.iterator().next());
	}

	@Test
	public void explicitPropertySourcesExcludesEnvironment() {
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
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("${my.name}"));
		assertEquals(ppc.getAppliedPropertySources().iterator().next(), propertySources.iterator().next());
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
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("${my.name}"));
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void ignoreUnresolvablePlaceholders_falseIsDefault() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		//pc.setIgnoreUnresolvablePlaceholders(false); // the default
		ppc.postProcessBeanFactory(bf); // should throw
	}

	@Test
	public void ignoreUnresolvablePlaceholders_true() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("${my.name}"));
	}

	@Test(expected = BeanDefinitionStoreException.class)
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
		ppc.postProcessBeanFactory(bf); // should throw
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
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("${bogus}"));
	}

	@Test
	public void withNonEnumerablePropertySource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${foo}")
					.getBeanDefinition());

		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

		PropertySource<?> ps = new PropertySource<Object>("simplePropertySource", new Object()) {
			@Override
			public Object getProperty(String key) {
				return "bar";
			}
		};

		MockEnvironment env = new MockEnvironment();
		env.getPropertySources().addFirst(ps);
		ppc.setEnvironment(env);

		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("bar"));
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
		ppc.setProperties(new Properties() {{ setProperty("foo", "local"); }});
		ppc.setEnvironment(new MockEnvironment().withProperty("foo", "enclosing"));
		ppc.postProcessBeanFactory(bf);
		if (override) {
			assertThat(bf.getBean(TestBean.class).getName(), equalTo("local"));
		}
		else {
			assertThat(bf.getBean(TestBean.class).getName(), equalTo("enclosing"));
		}
	}

	@Test
	public void customPlaceholderPrefixAndSuffix() {
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

		assertThat(bf.getBean(TestBean.class).getName(), is("systemKey1Value"));
		assertThat(bf.getBean(TestBean.class).getSex(), is("${key2}"));
	}

	@Test
	public void nullValueIsPreserved() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setNullValue("customNull");
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", "customNull"));
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), nullValue());
	}

	@Test
	public void trimValuesIsOffByDefault() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo(" myValue  "));
	}

	@Test
	public void trimValuesIsApplied() {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		ppc.setTrimValues(true);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.setEnvironment(new MockEnvironment().withProperty("my.name", " myValue  "));
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("myValue"));
	}

	@Test
	public void getAppliedPropertySourcesTooEarly() throws Exception {
		PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();
		thrown.expect(IllegalStateException.class);
		ppc.getAppliedPropertySources();
	}

	@Test
	public void multipleLocationsWithDefaultResolvedValue() throws Exception {
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
		assertThat(bf.getBean(TestBean.class).isJedi(), equalTo(true));
	}

	@Test
	public void optionalPropertyWithValue() {
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
		assertThat(bf.getBean(OptionalTestBean.class).getName(), equalTo(Optional.of("myValue")));
	}

	@Test
	public void optionalPropertyWithoutValue() {
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
		assertThat(bf.getBean(OptionalTestBean.class).getName(), equalTo(Optional.empty()));
	}


	private static class OptionalTestBean {

		private Optional<String> name;

		public Optional<String> getName() {
			return name;
		}

		public void setName(Optional<String> name) {
			this.name = name;
		}
	}

}
