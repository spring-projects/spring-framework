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

import java.lang.reflect.Proxy;
import java.nio.file.InvalidPathException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.mockito.ArgumentCaptor;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link GenericApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
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

	@Test
	void refreshForAotSetsContextActive() {
		GenericApplicationContext context = new GenericApplicationContext();
		assertThat(context.isActive()).isFalse();
		context.refreshForAotProcessing(new RuntimeHints());
		assertThat(context.isActive()).isTrue();
		context.close();
	}

	@Test
	void refreshForAotRegistersEnvironment() {
		ConfigurableEnvironment environment = mock();
		GenericApplicationContext context = new GenericApplicationContext();
		context.setEnvironment(environment);
		context.refreshForAotProcessing(new RuntimeHints());
		assertThat(context.getBean(Environment.class)).isEqualTo(environment);
		context.close();
	}

	@Test
	void refreshForAotLoadsBeanClassName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("number", new RootBeanDefinition("java.lang.Integer"));
		context.refreshForAotProcessing(new RuntimeHints());
		assertThat(getBeanDefinition(context, "number").getBeanClass()).isEqualTo(Integer.class);
		context.close();
	}

	@Test
	void refreshForAotLoadsBeanClassNameOfConstructorArgumentInnerBeanDefinition() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
		innerBeanDefinition.setBeanClassName("java.lang.Integer");
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, innerBeanDefinition);
		context.registerBeanDefinition("test",beanDefinition);
		context.refreshForAotProcessing(new RuntimeHints());
		RootBeanDefinition bd = getBeanDefinition(context, "test");
		GenericBeanDefinition value = (GenericBeanDefinition) bd.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, GenericBeanDefinition.class).getValue();
		assertThat(value.hasBeanClass()).isTrue();
		assertThat(value.getBeanClass()).isEqualTo(Integer.class);
		context.close();
	}

	@Test
	void refreshForAotLoadsBeanClassNameOfPropertyValueInnerBeanDefinition() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
		innerBeanDefinition.setBeanClassName("java.lang.Integer");
		beanDefinition.getPropertyValues().add("inner", innerBeanDefinition);
		context.registerBeanDefinition("test",beanDefinition);
		context.refreshForAotProcessing(new RuntimeHints());
		RootBeanDefinition bd = getBeanDefinition(context, "test");
		GenericBeanDefinition value = (GenericBeanDefinition) bd.getPropertyValues().get("inner");
		assertThat(value.hasBeanClass()).isTrue();
		assertThat(value.getBeanClass()).isEqualTo(Integer.class);
		context.close();
	}

	@Test
	void refreshForAotInvokesBeanFactoryPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanFactoryPostProcessor bfpp = mock();
		context.addBeanFactoryPostProcessor(bfpp);
		context.refreshForAotProcessing(new RuntimeHints());
		verify(bfpp).postProcessBeanFactory(context.getBeanFactory());
		context.close();
	}

	@Test
	void refreshForAotInvokesMergedBeanDefinitionPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(String.class));
		context.registerBeanDefinition("number", new RootBeanDefinition("java.lang.Integer"));
		MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
		context.refreshForAotProcessing(new RuntimeHints());
		verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "test"), String.class, "test");
		verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "number"), Integer.class, "number");
		context.close();
	}

	@Test
	void refreshForAotInvokesMergedBeanDefinitionPostProcessorsOnConstructorArgument() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanD.class);
		GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
		innerBeanDefinition.setBeanClassName("java.lang.Integer");
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, innerBeanDefinition);
		context.registerBeanDefinition("test", beanDefinition);
		MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
		context.refreshForAotProcessing(new RuntimeHints());
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "test"), BeanD.class, "test");
		verify(bpp).postProcessMergedBeanDefinition(any(RootBeanDefinition.class), eq(Integer.class), captor.capture());
		assertThat(captor.getValue()).startsWith("(inner bean)");
		context.close();
	}

	@Test
	void refreshForAotInvokesMergedBeanDefinitionPostProcessorsOnPropertyValue() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanD.class);
		GenericBeanDefinition innerBeanDefinition = new GenericBeanDefinition();
		innerBeanDefinition.setBeanClassName("java.lang.Integer");
		beanDefinition.getPropertyValues().add("counter", innerBeanDefinition);
		context.registerBeanDefinition("test", beanDefinition);
		MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
		context.refreshForAotProcessing(new RuntimeHints());
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(bpp).postProcessMergedBeanDefinition(getBeanDefinition(context, "test"), BeanD.class, "test");
		verify(bpp).postProcessMergedBeanDefinition(any(RootBeanDefinition.class), eq(Integer.class), captor.capture());
		assertThat(captor.getValue()).startsWith("(inner bean)");
		context.close();
	}

	@Test
	void refreshForAotFreezeConfiguration() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", new RootBeanDefinition(String.class));
		MergedBeanDefinitionPostProcessor bpp = registerMockMergedBeanDefinitionPostProcessor(context);
		context.refreshForAotProcessing(new RuntimeHints());
		RootBeanDefinition mergedBeanDefinition = getBeanDefinition(context, "test");
		verify(bpp).postProcessMergedBeanDefinition(mergedBeanDefinition, String.class, "test");
		context.getBeanFactory().clearMetadataCache();
		assertThat(context.getBeanFactory().getMergedBeanDefinition("test")).isSameAs(mergedBeanDefinition);
		context.close();
	}

	@Test
	void refreshForAotInvokesBeanPostProcessorContractOnMergedBeanDefinitionPostProcessors() {
		MergedBeanDefinitionPostProcessor bpp = new MergedBeanDefinitionPostProcessor() {
			@Override
			public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
				beanDefinition.setAttribute("mbdppCalled", true);
			}

			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
				return (beanName.equals("test") ? "42" : bean);
			}
		};
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bpp", BeanDefinitionBuilder.rootBeanDefinition(
						MergedBeanDefinitionPostProcessor.class, () -> bpp)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		AbstractBeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(String.class)
				.addConstructorArgValue("value").getBeanDefinition();
		context.registerBeanDefinition("test", bd);
		context.refreshForAotProcessing(new RuntimeHints());
		assertThat(context.getBeanFactory().getMergedBeanDefinition("test")
				.hasAttribute("mbdppCalled")).isTrue();
		assertThat(context.getBean("test")).isEqualTo("42");
		context.close();
	}

	@Test
	void refreshForAotFailsOnAnActiveContext() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.refresh();
		assertThatIllegalStateException().isThrownBy(() -> context.refreshForAotProcessing(new RuntimeHints()))
				.withMessageContaining("does not support multiple refresh attempts");
		context.close();
	}

	@Test
	void refreshForAotDoesNotInitializeFactoryBeansEarly() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("genericFactoryBean",
				new RootBeanDefinition(TestAotFactoryBean.class));
		context.refreshForAotProcessing(new RuntimeHints());
		context.close();
	}

	@Test
	void refreshForAotDoesNotInstantiateBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("test", BeanDefinitionBuilder.rootBeanDefinition(String.class, () -> {
			throw new IllegalStateException("Should not be invoked");
		}).getBeanDefinition());
		context.refreshForAotProcessing(new RuntimeHints());
		context.close();
	}

	@Test
	void refreshForAotRegisterProxyHint() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("bpp", BeanDefinitionBuilder.rootBeanDefinition(
				SmartInstantiationAwareBeanPostProcessor.class, () -> new SmartInstantiationAwareBeanPostProcessor() {
					@Override
					public Class<?> determineBeanType(Class<?> beanClass, String beanName) throws BeansException {
						if (beanClass.isInterface()) {
							return Proxy.newProxyInstance(GenericApplicationContextTests.class.getClassLoader(),
									new Class[] { Map.class, DecoratingProxy.class }, (proxy, method, args) -> null).getClass();
						}
						else {
							return beanClass;
						}
					}
				})
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		context.registerBeanDefinition("map", BeanDefinitionBuilder.rootBeanDefinition(Map.class,
				HashMap::new).getBeanDefinition());
		RuntimeHints runtimeHints = new RuntimeHints();
		context.refreshForAotProcessing(runtimeHints);
		assertThat(RuntimeHintsPredicates.proxies().forInterfaces(Map.class, DecoratingProxy.class)).accepts(runtimeHints);
		context.close();
	}


	private MergedBeanDefinitionPostProcessor registerMockMergedBeanDefinitionPostProcessor(GenericApplicationContext context) {
		MergedBeanDefinitionPostProcessor bpp = mock();
		context.registerBeanDefinition("bpp", BeanDefinitionBuilder.rootBeanDefinition(
						MergedBeanDefinitionPostProcessor.class, () -> bpp)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE).getBeanDefinition());
		return bpp;
	}

	private RootBeanDefinition getBeanDefinition(GenericApplicationContext context, String name) {
		return (RootBeanDefinition) context.getBeanFactory().getMergedBeanDefinition(name);
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

	static class BeanD {

		@SuppressWarnings("unused")
		private Integer counter;

		BeanD(Integer counter) {
			this.counter = counter;
		}

		public BeanD() {
		}

		public void setCounter(Integer counter) {
			this.counter = counter;
		}
	}


	static class TestAotFactoryBean<T> extends AbstractFactoryBean<T> {

		TestAotFactoryBean() {
			throw new IllegalStateException("FactoryBean should not be instantied early");
		}

		@Override
		public Class<?> getObjectType() {
			return Object.class;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected T createInstance() {
			return (T) new Object();
		}
	}


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
