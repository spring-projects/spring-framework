/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.registry;

import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.echo.EchoA;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceProxyBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
class HttpServiceProxyRegistrationAotProcessorTests {

	@Test
	void httpServiceProxyBeanRegistrationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanRegistrationAotProcessor.class))
				.anyMatch(HttpServiceProxyBeanRegistrationAotProcessor.class::isInstance);
	}

	@Test
	void getAotContributionWhenBeanHasNoGroup() {
		assertThat(hasContribution(new RootBeanDefinition(EchoA.class))).isFalse();
	}

	@Test
	void getAotContributionWhenBeanHasGroup() {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(EchoA.class);
		beanDefinition.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo");
		assertThat(hasContribution(beanDefinition)).isTrue();
	}

	private boolean hasContribution(RootBeanDefinition beanDefinition) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", beanDefinition);
		RegisteredBean registeredBean = RegisteredBean.of(beanFactory, "test");
		return new HttpServiceProxyBeanRegistrationAotProcessor().processAheadOfTime(registeredBean) != null;
	}

	@Test
	@CompileWithForkedClassLoader
	void processHttpServiceProxyWhenSingleClientType() {
		GroupsMetadata groupsMetadata = new GroupsMetadata();
		groupsMetadata.getOrCreateGroup("echo", ClientType.UNSPECIFIED)
				.httpServiceTypeNames().add(EchoA.class.getName());
		DefaultListableBeanFactory beanFactory = prepareBeanFactory(groupsMetadata);
		RootBeanDefinition beanDefinition = new RootBeanDefinition(EchoA.class);
		beanDefinition.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo");
		beanFactory.registerBeanDefinition("echoA", beanDefinition);
		compile(beanFactory, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
			HttpServiceProxyRegistry registry = freshApplicationContext.getBean(HttpServiceProxyRegistry.class);
			assertThat(registry.getClient("echo", EchoA.class)).isSameAs(freshApplicationContext.getBean(EchoA.class));
		});
	}

	@Test
	@CompileWithForkedClassLoader
	void processHttpServiceProxyWhenSameClientTypeInDifferentGroups() {
		GroupsMetadata groupsMetadata = new GroupsMetadata();
		groupsMetadata.getOrCreateGroup("echo", ClientType.UNSPECIFIED)
				.httpServiceTypeNames().add(EchoA.class.getName());
		groupsMetadata.getOrCreateGroup("echo2", ClientType.UNSPECIFIED)
				.httpServiceTypeNames().add(EchoA.class.getName());
		DefaultListableBeanFactory beanFactory = prepareBeanFactory(groupsMetadata);
		RootBeanDefinition beanDefinition = new RootBeanDefinition(EchoA.class);
		beanDefinition.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo");
		beanFactory.registerBeanDefinition("echoA", beanDefinition);
		RootBeanDefinition beanDefinition2 = new RootBeanDefinition(EchoA.class);
		beanDefinition2.setAttribute(AbstractHttpServiceRegistrar.HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, "echo2");
		beanFactory.registerBeanDefinition("echoA2", beanDefinition2);
		compile(beanFactory, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
			HttpServiceProxyRegistry registry = freshApplicationContext.getBean(HttpServiceProxyRegistry.class);
			assertThat(registry.getClient("echo", EchoA.class)).isSameAs(freshApplicationContext.getBean("echoA", EchoA.class));
			assertThat(registry.getClient("echo2", EchoA.class)).isSameAs(freshApplicationContext.getBean("echoA2", EchoA.class));
		});
	}

	private DefaultListableBeanFactory prepareBeanFactory(GroupsMetadata metadata) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDefinition = new RootBeanDefinition(HttpServiceProxyRegistryFactoryBean.class);
		beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, metadata);
		beanFactory.registerBeanDefinition(AbstractHttpServiceRegistrar.HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME, beanDefinition);
		return beanFactory;
	}


	@SuppressWarnings("unchecked")
	private void compile(DefaultListableBeanFactory beanFactory,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(new GenericApplicationContext(beanFactory), generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile(compiled ->
				result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
	}

	private GenericApplicationContext toFreshApplicationContext(
			ApplicationContextInitializer<GenericApplicationContext> initializer) {
		GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
		initializer.initialize(freshApplicationContext);
		freshApplicationContext.refresh();
		return freshApplicationContext;
	}

}
