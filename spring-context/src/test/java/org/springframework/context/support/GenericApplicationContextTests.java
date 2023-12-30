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

package org.springframework.context.support;

import java.nio.file.InvalidPathException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.metrics.jfr.FlightRecorderApplicationStartup;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * Tests for {@link GenericApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
class GenericApplicationContextTests {

	private final GenericApplicationContext context = new GenericApplicationContext();

	@AfterEach
	void closeContext() {
		context.close();
	}

	@Test
	void getBeanForClass() {
		context.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		context.refresh();

		assertThat(context.getBean("testBean")).isEqualTo("");
		assertThat(context.getBean(String.class)).isSameAs(context.getBean("testBean"));
		assertThat(context.getBean(CharSequence.class)).isSameAs(context.getBean("testBean"));

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
			.isThrownBy(() -> context.getBean(Object.class));
	}

	@Test
	void withSingletonSupplier() {
		context.registerBeanDefinition("testBean", new RootBeanDefinition(String.class, context::toString));
		context.refresh();

		assertThat(context.getBean("testBean")).isSameAs(context.getBean("testBean"));
		assertThat(context.getBean(String.class)).isSameAs(context.getBean("testBean"));
		assertThat(context.getBean(CharSequence.class)).isSameAs(context.getBean("testBean"));
		assertThat(context.getBean("testBean")).isEqualTo(context.toString());
	}

	@Test
	void withScopedSupplier() {
		context.registerBeanDefinition("testBean",
				new RootBeanDefinition(String.class, BeanDefinition.SCOPE_PROTOTYPE, context::toString));
		context.refresh();

		assertThat(context.getBean("testBean")).isNotSameAs(context.getBean("testBean"));
		assertThat(context.getBean(String.class)).isEqualTo(context.getBean("testBean"));
		assertThat(context.getBean(CharSequence.class)).isEqualTo(context.getBean("testBean"));
		assertThat(context.getBean("testBean")).isEqualTo(context.toString());
	}

	@Test
	void accessAfterClosing() {
		context.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		context.refresh();

		assertThat(context.getBean(String.class)).isSameAs(context.getBean("testBean"));
		assertThat(context.getAutowireCapableBeanFactory().getBean(String.class))
				.isSameAs(context.getAutowireCapableBeanFactory().getBean("testBean"));

		context.close();

		assertThatIllegalStateException()
				.isThrownBy(() -> context.getBean(String.class));
		assertThatIllegalStateException()
				.isThrownBy(() -> context.getAutowireCapableBeanFactory().getBean(String.class));
		assertThatIllegalStateException()
				.isThrownBy(() -> context.getAutowireCapableBeanFactory().getBean("testBean"));
	}

	@Test
	void individualBeans() {
		context.registerBean(BeanA.class);
		context.registerBean(BeanB.class);
		context.registerBean(BeanC.class);
		context.refresh();

		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualNamedBeans() {
		context.registerBean("a", BeanA.class);
		context.registerBean("b", BeanB.class);
		context.registerBean("c", BeanC.class);
		context.refresh();

		assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b"));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualBeanWithSupplier() {
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isTrue();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

		assertThat(context.getDefaultListableBeanFactory().getDependentBeans(BeanB.class.getName()))
			.containsExactly(BeanA.class.getName());
		assertThat(context.getDefaultListableBeanFactory().getDependentBeans(BeanC.class.getName()))
			.containsExactly(BeanA.class.getName());
	}

	@Test
	void individualBeanWithSupplierAndCustomizer() {
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isFalse();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualNamedBeanWithSupplier() {
		context.registerBean("a", BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("a")).isTrue();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualNamedBeanWithSupplierAndCustomizer() {
		context.registerBean("a", BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("a")).isFalse();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualBeanWithNullReturningSupplier() {
		context.registerBean("a", BeanA.class, () -> null);
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanNamesForType(BeanA.class)).containsExactly("a");
		assertThat(context.getBeanNamesForType(BeanB.class)).containsExactly("b");
		assertThat(context.getBeanNamesForType(BeanC.class)).containsExactly("c");
		assertThat(context.getBeansOfType(BeanA.class)).isEmpty();
		assertThat(context.getBeansOfType(BeanB.class).values().iterator().next())
			.isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBeansOfType(BeanC.class).values().iterator().next())
			.isSameAs(context.getBean(BeanC.class));
	}

	@Test
	void configureApplicationStartupOnBeanFactory() {
		FlightRecorderApplicationStartup applicationStartup = new FlightRecorderApplicationStartup();
		context.setApplicationStartup(applicationStartup);
		assertThat(context.getBeanFactory().getApplicationStartup()).isEqualTo(applicationStartup);
	}

	@Test
	void getResourceWithDefaultResourceLoader() {
		assertGetResourceSemantics(null, ClassPathResource.class);
	}

	@Test
	void getResourceWithCustomResourceLoader() {
		assertGetResourceSemantics(new FileSystemResourceLoader(), FileSystemResource.class);
	}

	private void assertGetResourceSemantics(ResourceLoader resourceLoader, Class<? extends Resource> defaultResourceType) {
		if (resourceLoader != null) {
			context.setResourceLoader(resourceLoader);
		}

		String relativePathLocation = "foo";
		String fileLocation = "file:foo";
		String pingLocation = "ping:foo";

		Resource resource = context.getResource(relativePathLocation);
		assertThat(resource).isInstanceOf(defaultResourceType);
		resource = context.getResource(fileLocation);
		assertThat(resource).isInstanceOf(FileUrlResource.class);

		// If we are using a FileSystemResourceLoader on Windows, we expect an error
		// similar to the following since "ping:foo" is not a valid file name in the
		// Windows file system and since the PingPongProtocolResolver has not yet been
		// registered.
		//
		// java.nio.file.InvalidPathException: Illegal char <:> at index 4: ping:foo
		if (resourceLoader instanceof FileSystemResourceLoader && OS.WINDOWS.isCurrentOs()) {
			assertThatExceptionOfType(InvalidPathException.class)
				.isThrownBy(() -> context.getResource(pingLocation))
				.withMessageContaining(pingLocation);
		}
		else {
			resource = context.getResource(pingLocation);
			assertThat(resource).isInstanceOf(defaultResourceType);
		}

		context.addProtocolResolver(new PingPongProtocolResolver());

		resource = context.getResource(relativePathLocation);
		assertThat(resource).isInstanceOf(defaultResourceType);
		resource = context.getResource(fileLocation);
		assertThat(resource).isInstanceOf(FileUrlResource.class);
		resource = context.getResource(pingLocation);
		assertThat(resource).asInstanceOf(type(ByteArrayResource.class))
			.extracting(bar -> new String(bar.getByteArray(), UTF_8))
			.isEqualTo("pong:foo");
	}


	static class BeanA {

		BeanB b;
		BeanC c;

		public BeanA(BeanB b, BeanC c) {
			this.b = b;
			this.c = c;
		}
	}

	static class BeanB implements ApplicationContextAware  {

		ApplicationContext applicationContext;

		public BeanB() {
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}
	}

	static class BeanC {}

	static class PingPongProtocolResolver implements ProtocolResolver {

		@Override
		public Resource resolve(String location, ResourceLoader resourceLoader) {
			if (location.startsWith("ping:")) {
				return new ByteArrayResource(("pong:" + location.substring(5)).getBytes(UTF_8));
			}
			return null;
		}
	}

}
