/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import org.springframework.tests.sample.beans.TestBean;

/**
 * Tests the processing of @PropertySource annotations on @Configuration classes.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class PropertySourceAnnotationTests {

	@Test
	public void withExplicitName() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithExplicitName.class);
		ctx.refresh();
		assertTrue("property source p1 was not added",
				ctx.getEnvironment().getPropertySources().contains("p1"));
		assertThat(ctx.getBean(TestBean.class).getName(), equalTo("p1TestBean"));

		// assert that the property source was added last to the set of sources
		String name;
		MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
		Iterator<org.springframework.core.env.PropertySource<?>> iterator = sources.iterator();
		do {
			name = iterator.next().getName();
		}
		while(iterator.hasNext());

		assertThat(name, is("p1"));
	}

	@Test
	public void withImplicitName() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithImplicitName.class);
		ctx.refresh();
		assertTrue("property source p1 was not added",
				ctx.getEnvironment().getPropertySources().contains("class path resource [org/springframework/context/annotation/p1.properties]"));
		assertThat(ctx.getBean(TestBean.class).getName(), equalTo("p1TestBean"));
	}

	/**
	 * Tests the LIFO behavior of @PropertySource annotaitons.
	 * The last one registered should 'win'.
	 */
	@Test
	public void orderingIsLifo() {
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.register(ConfigWithImplicitName.class, P2Config.class);
			ctx.refresh();
			// p2 should 'win' as it was registered last
			assertThat(ctx.getBean(TestBean.class).getName(), equalTo("p2TestBean"));
		}

		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.register(P2Config.class, ConfigWithImplicitName.class);
			ctx.refresh();
			// p1 should 'win' as it was registered last
			assertThat(ctx.getBean(TestBean.class).getName(), equalTo("p1TestBean"));
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void withUnresolvablePlaceholder() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithUnresolvablePlaceholder.class);
		ctx.refresh();
	}

	@Test
	public void withUnresolvablePlaceholderAndDefault() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithUnresolvablePlaceholderAndDefault.class);
		ctx.refresh();
		assertThat(ctx.getBean(TestBean.class).getName(), equalTo("p1TestBean"));
	}

	@Test
	public void withResolvablePlaceholder() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithResolvablePlaceholder.class);
		System.setProperty("path.to.properties", "org/springframework/context/annotation");
		ctx.refresh();
		assertThat(ctx.getBean(TestBean.class).getName(), equalTo("p1TestBean"));
		System.clearProperty("path.to.properties");
	}

	/**
	 * Corner bug reported in SPR-9127.
	 */
	@Test
	public void withNameAndMultipleResourceLocations() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithNameAndMultipleResourceLocations.class);
		ctx.refresh();
		assertThat(ctx.getEnvironment().containsProperty("from.p1"), is(true));
		assertThat(ctx.getEnvironment().containsProperty("from.p2"), is(true));
	}

	@Test(expected=IllegalArgumentException.class)
	public void withEmptyResourceLocations() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithEmptyResourceLocations.class);
		ctx.refresh();
	}


	@Configuration
	@PropertySource(value="classpath:${unresolvable}/p1.properties")
	static class ConfigWithUnresolvablePlaceholder {
	}


	@Configuration
	@PropertySource(value="classpath:${unresolvable:org/springframework/context/annotation}/p1.properties")
	static class ConfigWithUnresolvablePlaceholderAndDefault {
		@Inject Environment env;

		@Bean
		public TestBean testBean() {
			return new TestBean(env.getProperty("testbean.name"));
		}
	}


	@Configuration
	@PropertySource(value="classpath:${path.to.properties}/p1.properties")
	static class ConfigWithResolvablePlaceholder {
		@Inject Environment env;

		@Bean
		public TestBean testBean() {
			return new TestBean(env.getProperty("testbean.name"));
		}
	}



	@Configuration
	@PropertySource(name="p1", value="classpath:org/springframework/context/annotation/p1.properties")
	static class ConfigWithExplicitName {
		@Inject Environment env;

		@Bean
		public TestBean testBean() {
			return new TestBean(env.getProperty("testbean.name"));
		}
	}


	@Configuration
	@PropertySource("classpath:org/springframework/context/annotation/p1.properties")
	static class ConfigWithImplicitName {
		@Inject Environment env;

		@Bean
		public TestBean testBean() {
			return new TestBean(env.getProperty("testbean.name"));
		}
	}


	@Configuration
	@PropertySource("classpath:org/springframework/context/annotation/p2.properties")
	static class P2Config {
	}


	@Configuration
	@PropertySource(
			name = "psName",
			value = {
					"classpath:org/springframework/context/annotation/p1.properties",
					"classpath:org/springframework/context/annotation/p2.properties"
			})
	static class ConfigWithNameAndMultipleResourceLocations {
	}


	@Configuration
	@PropertySource(value = {})
	static class ConfigWithEmptyResourceLocations {
	}
}
