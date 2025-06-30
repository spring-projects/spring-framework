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

package org.springframework.core.env;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.context.ConfigurableApplicationContext.ENVIRONMENT_BEAN_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.DERIVED_DEV_BEAN_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.DERIVED_DEV_ENV_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.DEV_BEAN_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.DEV_ENV_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.ENVIRONMENT_AWARE_BEAN_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.PROD_BEAN_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.PROD_ENV_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.TRANSITIVE_BEAN_NAME;
import static org.springframework.core.env.EnvironmentSystemIntegrationTests.Constants.XML_PATH;

/**
 * System integration tests for container support of the {@link Environment} API.
 *
 * <p>
 * Tests all existing BeanFactory and ApplicationContext implementations to ensure that:
 * <ul>
 * <li>a standard environment object is always present
 * <li>a custom environment object can be set and retrieved against the factory/context
 * <li>the {@link EnvironmentAware} interface is respected
 * <li>the environment object is registered with the container as a singleton bean (if an
 * ApplicationContext)
 * <li>bean definition files (if any, and whether XML or @Configuration) are registered
 * conditionally based on environment metadata
 * </ul>
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @see org.springframework.context.support.EnvironmentIntegrationTests
 */
public class EnvironmentSystemIntegrationTests {

	private final ConfigurableEnvironment prodEnv = new StandardEnvironment();

	private final ConfigurableEnvironment devEnv = new StandardEnvironment();

	private final ConfigurableEnvironment prodWebEnv = new StandardServletEnvironment();

	@BeforeEach
	void setUp() {
		prodEnv.setActiveProfiles(PROD_ENV_NAME);
		devEnv.setActiveProfiles(DEV_ENV_NAME);
		prodWebEnv.setActiveProfiles(PROD_ENV_NAME);
	}

	@Test
	void genericApplicationContext_standardEnv() {
		ConfigurableApplicationContext ctx = new GenericApplicationContext(newBeanFactoryWithEnvironmentAwareBean());
		ctx.refresh();

		assertHasStandardEnvironment(ctx);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, ctx.getEnvironment());
	}

	@Test
	void genericApplicationContext_customEnv() {
		GenericApplicationContext ctx = new GenericApplicationContext(newBeanFactoryWithEnvironmentAwareBean());
		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	void xmlBeanDefinitionReader_inheritsEnvironmentFromEnvironmentCapableBDR() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(XML_PATH);
		ctx.refresh();
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void annotatedBeanDefinitionReader_inheritsEnvironmentFromEnvironmentCapableBDR() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		new AnnotatedBeanDefinitionReader(ctx).register(Config.class);
		ctx.refresh();
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void classPathBeanDefinitionScanner_inheritsEnvironmentFromEnvironmentCapableBDR_scanProfileAnnotatedConfigClasses() {
		// it's actually ConfigurationClassPostProcessor's Environment that gets the job done here.
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
		scanner.scan("org.springframework.core.env.scan1");
		ctx.refresh();
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void classPathBeanDefinitionScanner_inheritsEnvironmentFromEnvironmentCapableBDR_scanProfileAnnotatedComponents() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
		scanner.scan("org.springframework.core.env.scan2");
		ctx.refresh();
		assertThat(scanner.getEnvironment()).isEqualTo(ctx.getEnvironment());
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void genericXmlApplicationContext() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);
		ctx.load(XML_PATH);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void classPathXmlApplicationContext() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(XML_PATH);
		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertEnvironmentBeanRegistered(ctx);
		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentAwareInvoked(ctx, ctx.getEnvironment());
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void fileSystemXmlApplicationContext() throws IOException {
		ClassPathResource xml = new ClassPathResource(XML_PATH);
		File tmpFile = File.createTempFile("test", "xml");
		FileCopyUtils.copy(xml.getFile(), tmpFile);

		// strange - FSXAC strips leading '/' unless prefixed with 'file:'
		ConfigurableApplicationContext ctx =
				new FileSystemXmlApplicationContext(new String[] {"file:" + tmpFile.getPath()}, false);
		ctx.setEnvironment(prodEnv);
		ctx.refresh();
		assertEnvironmentBeanRegistered(ctx);
		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentAwareInvoked(ctx, ctx.getEnvironment());
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void annotationConfigApplicationContext_withPojos() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(EnvironmentAwareBean.class);
		ctx.refresh();

		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	void annotationConfigApplicationContext_withProdEnvAndProdConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(ProdConfig.class);
		ctx.refresh();

		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void annotationConfigApplicationContext_withProdEnvAndDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(DevConfig.class);
		ctx.refresh();

		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(TRANSITIVE_BEAN_NAME)).isFalse();
	}

	@Test
	void annotationConfigApplicationContext_withDevEnvAndDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(devEnv);

		ctx.register(DevConfig.class);
		ctx.refresh();

		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isTrue();
		assertThat(ctx.containsBean(TRANSITIVE_BEAN_NAME)).isTrue();
	}

	@Test
	void annotationConfigApplicationContext_withImportedConfigClasses() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(Config.class);
		ctx.refresh();

		assertEnvironmentAwareInvoked(ctx, prodEnv);
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(TRANSITIVE_BEAN_NAME)).isFalse();
	}

	@Test
	void mostSpecificDerivedClassDrivesEnvironment_withDerivedDevEnvAndDerivedDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		StandardEnvironment derivedDevEnv = new StandardEnvironment();
		derivedDevEnv.setActiveProfiles(DERIVED_DEV_ENV_NAME);
		ctx.setEnvironment(derivedDevEnv);
		ctx.register(DerivedDevConfig.class);
		ctx.refresh();

		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isTrue();
		assertThat(ctx.containsBean(DERIVED_DEV_BEAN_NAME)).isTrue();
		assertThat(ctx.containsBean(TRANSITIVE_BEAN_NAME)).isTrue();
	}

	@Test
	void mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setEnvironment(devEnv);
		ctx.register(DerivedDevConfig.class);
		ctx.refresh();

		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(DERIVED_DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(TRANSITIVE_BEAN_NAME)).isFalse();
	}

	@Test
	void annotationConfigApplicationContext_withProfileExpressionMatchOr() {
		testProfileExpression(true, "p3");
	}

	@Test
	void annotationConfigApplicationContext_withProfileExpressionMatchAnd() {
		testProfileExpression(true, "p1", "p2");
	}

	@Test
	void annotationConfigApplicationContext_withProfileExpressionNoMatchAnd() {
		testProfileExpression(false, "p1");
	}

	@Test
	void annotationConfigApplicationContext_withProfileExpressionNoMatchNone() {
		testProfileExpression(false, "p4");
	}

	private void testProfileExpression(boolean expected, String... activeProfiles) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		StandardEnvironment environment = new StandardEnvironment();
		environment.setActiveProfiles(activeProfiles);
		ctx.setEnvironment(environment);
		ctx.register(ProfileExpressionConfig.class);
		ctx.refresh();
		assertThat(ctx.containsBean("expressionBean")).isEqualTo(expected);
	}

	@Test
	void webApplicationContext() {
		GenericWebApplicationContext ctx = new GenericWebApplicationContext(newBeanFactoryWithEnvironmentAwareBean());
		assertHasStandardServletEnvironment(ctx);
		ctx.setEnvironment(prodWebEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
	}

	@Test
	void xmlWebApplicationContext() {
		AbstractRefreshableWebApplicationContext ctx = new XmlWebApplicationContext();
		ctx.setConfigLocation("classpath:" + XML_PATH);
		ctx.setEnvironment(prodWebEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
		assertThat(ctx.containsBean(DEV_BEAN_NAME)).isFalse();
		assertThat(ctx.containsBean(PROD_BEAN_NAME)).isTrue();
	}

	@Test
	void staticApplicationContext() {
		StaticApplicationContext ctx = new StaticApplicationContext();

		assertHasStandardEnvironment(ctx);

		registerEnvironmentBeanDefinition(ctx);

		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	void staticWebApplicationContext() {
		StaticWebApplicationContext ctx = new StaticWebApplicationContext();

		assertHasStandardServletEnvironment(ctx);

		registerEnvironmentBeanDefinition(ctx);

		ctx.setEnvironment(prodWebEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
	}

	@Test
	void annotationConfigWebApplicationContext() {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setEnvironment(prodWebEnv);
		ctx.setConfigLocation(EnvironmentAwareBean.class.getName());
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
	}

	@Test
	void registerServletParamPropertySources_AbstractRefreshableWebApplicationContext() {
		MockServletContext servletContext = new MockServletContext();
		servletContext.addInitParameter("pCommon", "pCommonContextValue");
		servletContext.addInitParameter("pContext1", "pContext1Value");

		MockServletConfig servletConfig = new MockServletConfig(servletContext);
		servletConfig.addInitParameter("pCommon", "pCommonConfigValue");
		servletConfig.addInitParameter("pConfig1", "pConfig1Value");

		AbstractRefreshableWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setConfigLocation(EnvironmentAwareBean.class.getName());
		ctx.setServletConfig(servletConfig);
		ctx.refresh();

		ConfigurableEnvironment environment = ctx.getEnvironment();
		assertThat(environment).isInstanceOf(StandardServletEnvironment.class);
		MutablePropertySources propertySources = environment.getPropertySources();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)).isTrue();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)).isTrue();

		// ServletConfig gets precedence
		assertThat(environment.getProperty("pCommon")).isEqualTo("pCommonConfigValue");
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)))
				.isLessThan(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)));

		// but all params are available
		assertThat(environment.getProperty("pContext1")).isEqualTo("pContext1Value");
		assertThat(environment.getProperty("pConfig1")).isEqualTo("pConfig1Value");

		// Servlet* PropertySources have precedence over System* PropertySources
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)))
				.isLessThan(propertySources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)));

		// Replace system properties with a mock property source for convenience
		MockPropertySource mockSystemProperties = new MockPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		mockSystemProperties.setProperty("pCommon", "pCommonSysPropsValue");
		mockSystemProperties.setProperty("pSysProps1", "pSysProps1Value");
		propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockSystemProperties);

		// assert that servletconfig params resolve with higher precedence than sysprops
		assertThat(environment.getProperty("pCommon")).isEqualTo("pCommonConfigValue");
		assertThat(environment.getProperty("pSysProps1")).isEqualTo("pSysProps1Value");
	}

	@Test
	void registerServletParamPropertySources_GenericWebApplicationContext() {
		MockServletContext servletContext = new MockServletContext();
		servletContext.addInitParameter("pCommon", "pCommonContextValue");
		servletContext.addInitParameter("pContext1", "pContext1Value");

		GenericWebApplicationContext ctx = new GenericWebApplicationContext();
		ctx.setServletContext(servletContext);
		ctx.refresh();

		ConfigurableEnvironment environment = ctx.getEnvironment();
		assertThat(environment).isInstanceOf(StandardServletEnvironment.class);
		MutablePropertySources propertySources = environment.getPropertySources();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)).isTrue();

		// ServletContext params are available
		assertThat(environment.getProperty("pCommon")).isEqualTo("pCommonContextValue");
		assertThat(environment.getProperty("pContext1")).isEqualTo("pContext1Value");

		// Servlet* PropertySources have precedence over System* PropertySources
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)))
				.isLessThan(propertySources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)));

		// Replace system properties with a mock property source for convenience
		MockPropertySource mockSystemProperties = new MockPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		mockSystemProperties.setProperty("pCommon", "pCommonSysPropsValue");
		mockSystemProperties.setProperty("pSysProps1", "pSysProps1Value");
		propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockSystemProperties);

		// assert that servletcontext init params resolve with higher precedence than sysprops
		assertThat(environment.getProperty("pCommon")).isEqualTo("pCommonContextValue");
		assertThat(environment.getProperty("pSysProps1")).isEqualTo("pSysProps1Value");
	}

	@Test
	void registerServletParamPropertySources_StaticWebApplicationContext() {
		MockServletContext servletContext = new MockServletContext();
		servletContext.addInitParameter("pCommon", "pCommonContextValue");
		servletContext.addInitParameter("pContext1", "pContext1Value");

		MockServletConfig servletConfig = new MockServletConfig(servletContext);
		servletConfig.addInitParameter("pCommon", "pCommonConfigValue");
		servletConfig.addInitParameter("pConfig1", "pConfig1Value");

		StaticWebApplicationContext ctx = new StaticWebApplicationContext();
		ctx.setServletConfig(servletConfig);
		ctx.refresh();

		ConfigurableEnvironment environment = ctx.getEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)).isTrue();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)).isTrue();

		// ServletConfig gets precedence
		assertThat(environment.getProperty("pCommon")).isEqualTo("pCommonConfigValue");
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)))
				.isLessThan(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)));

		// but all params are available
		assertThat(environment.getProperty("pContext1")).isEqualTo("pContext1Value");
		assertThat(environment.getProperty("pConfig1")).isEqualTo("pConfig1Value");

		// Servlet* PropertySources have precedence over System* PropertySources
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)))
				.isLessThan(propertySources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)));

		// Replace system properties with a mock property source for convenience
		MockPropertySource mockSystemProperties = new MockPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		mockSystemProperties.setProperty("pCommon", "pCommonSysPropsValue");
		mockSystemProperties.setProperty("pSysProps1", "pSysProps1Value");
		propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockSystemProperties);

		// assert that servletconfig params resolve with higher precedence than sysprops
		assertThat(environment.getProperty("pCommon")).isEqualTo("pCommonConfigValue");
		assertThat(environment.getProperty("pSysProps1")).isEqualTo("pSysProps1Value");
	}

	@Test
	void abstractApplicationContextValidatesRequiredPropertiesOnRefresh() {
		{
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.refresh();
		}

		{
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setRequiredProperties("foo", "bar");
			assertThatExceptionOfType(MissingRequiredPropertiesException.class).isThrownBy(ctx::refresh);
		}

		{
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setRequiredProperties("foo");
			ctx.setEnvironment(new MockEnvironment().withProperty("foo", "fooValue"));
			ctx.refresh(); // should succeed
		}
	}


	private DefaultListableBeanFactory newBeanFactoryWithEnvironmentAwareBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		registerEnvironmentBeanDefinition(bf);
		return bf;
	}

	private void registerEnvironmentBeanDefinition(BeanDefinitionRegistry registry) {
		registry.registerBeanDefinition(ENVIRONMENT_AWARE_BEAN_NAME,
				rootBeanDefinition(EnvironmentAwareBean.class).getBeanDefinition());
	}

	private void assertEnvironmentBeanRegistered(
			ConfigurableApplicationContext ctx) {
		// ensure environment is registered as a bean
		assertThat(ctx.containsBean(ENVIRONMENT_BEAN_NAME)).isTrue();
	}

	private void assertHasStandardEnvironment(ApplicationContext ctx) {
		Environment defaultEnv = ctx.getEnvironment();
		assertThat(defaultEnv).isNotNull();
		assertThat(defaultEnv).isInstanceOf(StandardEnvironment.class);
	}

	private void assertHasStandardServletEnvironment(WebApplicationContext ctx) {
		// ensure a default servlet environment exists
		Environment defaultEnv = ctx.getEnvironment();
		assertThat(defaultEnv).isNotNull();
		assertThat(defaultEnv).isInstanceOf(StandardServletEnvironment.class);
	}

	private void assertHasEnvironment(ApplicationContext ctx, Environment expectedEnv) {
		// ensure the custom environment took
		Environment actualEnv = ctx.getEnvironment();
		assertThat(actualEnv).isNotNull();
		assertThat(actualEnv).isEqualTo(expectedEnv);
		// ensure environment is registered as a bean
		assertThat(ctx.containsBean(ENVIRONMENT_BEAN_NAME)).isTrue();
	}

	private void assertEnvironmentAwareInvoked(ConfigurableApplicationContext ctx, Environment expectedEnv) {
		assertThat(ctx.getBean(EnvironmentAwareBean.class).environment).isEqualTo(expectedEnv);
	}


	private static class EnvironmentAwareBean implements EnvironmentAware {

		public Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}
	}


	/**
	 * Mirrors the structure of beans and environment-specific config files in
	 * EnvironmentSystemIntegrationTests-context.xml
	 */
	@Configuration
	@Import({DevConfig.class, ProdConfig.class})
	static class Config {
		@Bean
		EnvironmentAwareBean envAwareBean() {
			return new EnvironmentAwareBean();
		}
	}

	@Profile(DEV_ENV_NAME)
	@Configuration
	@Import(TransitiveConfig.class)
	static class DevConfig {
		@Bean
		public Object devBean() {
			return new Object();
		}
	}

	@Profile(PROD_ENV_NAME)
	@Configuration
	static class ProdConfig {
		@Bean
		public Object prodBean() {
			return new Object();
		}
	}

	@Configuration
	static class TransitiveConfig {
		@Bean
		public Object transitiveBean() {
			return new Object();
		}
	}

	@Profile(DERIVED_DEV_ENV_NAME)
	@Configuration
	static class DerivedDevConfig extends DevConfig {
		@Bean
		public Object derivedDevBean() {
			return new Object();
		}
	}

	@Profile("(p1 & p2) | p3")
	@Configuration
	static class ProfileExpressionConfig {
		@Bean
		public Object expressionBean() {
			return new Object();
		}
	}


	/**
	 * Constants used both locally and in scan* sub-packages
	 */
	public static class Constants {

		public static final String XML_PATH = "org/springframework/core/env/EnvironmentSystemIntegrationTests-context.xml";

		public static final String ENVIRONMENT_AWARE_BEAN_NAME = "envAwareBean";

		public static final String PROD_BEAN_NAME = "prodBean";
		public static final String DEV_BEAN_NAME = "devBean";
		public static final String DERIVED_DEV_BEAN_NAME = "derivedDevBean";
		public static final String TRANSITIVE_BEAN_NAME = "transitiveBean";

		public static final String PROD_ENV_NAME = "prod";
		public static final String DEV_ENV_NAME = "dev";
		public static final String DERIVED_DEV_ENV_NAME = "derivedDev";
	}

}
