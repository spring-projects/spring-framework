/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.env;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

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
import org.springframework.jca.context.ResourceAdapterApplicationContext;
import org.springframework.jca.support.SimpleBootstrapContext;
import org.springframework.jca.work.SimpleTaskWorkManager;
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
import org.springframework.web.portlet.context.AbstractRefreshablePortletApplicationContext;
import org.springframework.web.portlet.context.StandardPortletEnvironment;
import org.springframework.web.portlet.context.StaticPortletApplicationContext;
import org.springframework.web.portlet.context.XmlPortletApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;
import static org.springframework.context.ConfigurableApplicationContext.*;
import static org.springframework.core.env.EnvironmentIntegrationTests.Constants.*;


/**
 * Integration tests for container support of {@link Environment}
 * interface.
 *
 * Tests all existing BeanFactory and ApplicationContext implementations to
 * ensure that:
 * - a standard environment object is always present
 * - a custom environment object can be set and retrieved against the factory/context
 * - the {@link EnvironmentAware} interface is respected
 * - the environment object is registered with the container as a singleton
 *   bean (if an ApplicationContext)
 * - bean definition files (if any, and whether XML or @Configuration) are
 *   registered conditionally based on environment metadata
 *
 * @author Chris Beams
 */
public class EnvironmentIntegrationTests {

	private ConfigurableEnvironment prodEnv;
	private ConfigurableEnvironment devEnv;
	private ConfigurableEnvironment prodWebEnv;

	/**
	 * Constants used both locally and in scan* sub-packages
	 */
	public static class Constants {
		public static final String XML_PATH = "org/springframework/core/env/EnvironmentIntegrationTests-context.xml";

		public static final String ENVIRONMENT_AWARE_BEAN_NAME = "envAwareBean";

		public static final String PROD_BEAN_NAME = "prodBean";
		public static final String DEV_BEAN_NAME = "devBean";
		public static final String DERIVED_DEV_BEAN_NAME = "derivedDevBean";
		public static final String TRANSITIVE_BEAN_NAME = "transitiveBean";

		public static final String PROD_ENV_NAME = "prod";
		public static final String DEV_ENV_NAME = "dev";
		public static final String DERIVED_DEV_ENV_NAME = "derivedDev";
	}

	@Before
	public void setUp() {
		prodEnv = new StandardEnvironment();
		prodEnv.setActiveProfiles(PROD_ENV_NAME);

		devEnv = new StandardEnvironment();
		devEnv.setActiveProfiles(DEV_ENV_NAME);

		prodWebEnv = new StandardServletEnvironment();
		prodWebEnv.setActiveProfiles(PROD_ENV_NAME);
	}

	@Test
	public void genericApplicationContext_standardEnv() {
		ConfigurableApplicationContext ctx =
			new GenericApplicationContext(newBeanFactoryWithEnvironmentAwareBean());

		ctx.refresh();

		assertHasStandardEnvironment(ctx);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, ctx.getEnvironment());
	}

	@Test
	public void genericApplicationContext_customEnv() {
		GenericApplicationContext ctx =
			new GenericApplicationContext(newBeanFactoryWithEnvironmentAwareBean());
		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	public void xmlBeanDefinitionReader_inheritsEnvironmentFromEnvironmentCapableBDR() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		new XmlBeanDefinitionReader(ctx).loadBeanDefinitions(XML_PATH);
		ctx.refresh();
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void annotatedBeanDefinitionReader_inheritsEnvironmentFromEnvironmentCapableBDR() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		new AnnotatedBeanDefinitionReader(ctx).register(Config.class);
		ctx.refresh();
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void classPathBeanDefinitionScanner_inheritsEnvironmentFromEnvironmentCapableBDR_scanProfileAnnotatedConfigClasses() {
		// it's actually ConfigurationClassPostProcessor's Environment that gets the job done here.
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
		scanner.scan("org.springframework.core.env.scan1");
		ctx.refresh();
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void classPathBeanDefinitionScanner_inheritsEnvironmentFromEnvironmentCapableBDR_scanProfileAnnotatedComponents() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.setEnvironment(prodEnv);
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(ctx);
		scanner.scan("org.springframework.core.env.scan2");
		ctx.refresh();
		assertThat(scanner.getEnvironment(), is((Environment)ctx.getEnvironment()));
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void genericXmlApplicationContext() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();

		assertHasStandardEnvironment(ctx);

		ctx.setEnvironment(prodEnv);

		ctx.load(XML_PATH);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void classPathXmlApplicationContext() {
		ConfigurableApplicationContext ctx =
			new ClassPathXmlApplicationContext(new String[] { XML_PATH });
		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertEnvironmentBeanRegistered(ctx);
		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentAwareInvoked(ctx, ctx.getEnvironment());
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void fileSystemXmlApplicationContext() throws IOException {
		ClassPathResource xml = new ClassPathResource(XML_PATH);
		File tmpFile = File.createTempFile("test", "xml");
		FileCopyUtils.copy(xml.getFile(), tmpFile);

		// strange - FSXAC strips leading '/' unless prefixed with 'file:'
		ConfigurableApplicationContext ctx =
			new FileSystemXmlApplicationContext(new String[] { "file:"+tmpFile.getPath() }, false);
		ctx.setEnvironment(prodEnv);
		ctx.refresh();
		assertEnvironmentBeanRegistered(ctx);
		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentAwareInvoked(ctx, ctx.getEnvironment());
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void annotationConfigApplicationContext_withPojos() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(EnvironmentAwareBean.class);
		ctx.refresh();

		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	public void annotationConfigApplicationContext_withProdEnvAndProdConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(ProdConfig.class);
		ctx.refresh();

		assertThat("should have prod bean", ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void annotationConfigApplicationContext_withProdEnvAndDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(DevConfig.class);
		ctx.refresh();

		assertThat("should not have dev bean", ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat("should not have transitive bean", ctx.containsBean(TRANSITIVE_BEAN_NAME), is(false));
	}

	@Test
	public void annotationConfigApplicationContext_withDevEnvAndDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(devEnv);

		ctx.register(DevConfig.class);
		ctx.refresh();

		assertThat("should have dev bean", ctx.containsBean(DEV_BEAN_NAME), is(true));
		assertThat("should have transitive bean", ctx.containsBean(TRANSITIVE_BEAN_NAME), is(true));
	}

	@Test
	public void annotationConfigApplicationContext_withImportedConfigClasses() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

		assertHasStandardEnvironment(ctx);
		ctx.setEnvironment(prodEnv);

		ctx.register(Config.class);
		ctx.refresh();

		assertEnvironmentAwareInvoked(ctx, prodEnv);
		assertThat("should have prod bean", ctx.containsBean(PROD_BEAN_NAME), is(true));
		assertThat("should not have dev bean", ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat("should not have transitive bean", ctx.containsBean(TRANSITIVE_BEAN_NAME), is(false));
	}

	@Test
	public void mostSpecificDerivedClassDrivesEnvironment_withDerivedDevEnvAndDerivedDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		StandardEnvironment derivedDevEnv = new StandardEnvironment();
		derivedDevEnv.setActiveProfiles(DERIVED_DEV_ENV_NAME);
		ctx.setEnvironment(derivedDevEnv);
		ctx.register(DerivedDevConfig.class);
		ctx.refresh();

		assertThat("should have dev bean", ctx.containsBean(DEV_BEAN_NAME), is(true));
		assertThat("should have derived dev bean", ctx.containsBean(DERIVED_DEV_BEAN_NAME), is(true));
		assertThat("should have transitive bean", ctx.containsBean(TRANSITIVE_BEAN_NAME), is(true));
	}

	@Test
	public void mostSpecificDerivedClassDrivesEnvironment_withDevEnvAndDerivedDevConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setEnvironment(devEnv);
		ctx.register(DerivedDevConfig.class);
		ctx.refresh();

		assertThat("should not have dev bean", ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat("should not have derived dev bean", ctx.containsBean(DERIVED_DEV_BEAN_NAME), is(false));
		assertThat("should not have transitive bean", ctx.containsBean(TRANSITIVE_BEAN_NAME), is(false));
	}

	@Test
	public void webApplicationContext() {
		GenericWebApplicationContext ctx =
			new GenericWebApplicationContext(newBeanFactoryWithEnvironmentAwareBean());

		assertHasStandardServletEnvironment(ctx);

		ctx.setEnvironment(prodWebEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
	}

	@Test
	public void xmlWebApplicationContext() {
		AbstractRefreshableWebApplicationContext ctx = new XmlWebApplicationContext();
		ctx.setConfigLocation("classpath:" + XML_PATH);
		ctx.setEnvironment(prodWebEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void staticApplicationContext() {
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
	public void staticWebApplicationContext() {
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
	public void annotationConfigWebApplicationContext() {
		AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
		ctx.setEnvironment(prodWebEnv);
		ctx.setConfigLocation(EnvironmentAwareBean.class.getName());
		ctx.refresh();

		assertHasEnvironment(ctx, prodWebEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodWebEnv);
	}

	@Test
	public void registerServletParamPropertySources_AbstractRefreshableWebApplicationContext() {
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
		assertThat(environment, instanceOf(StandardServletEnvironment.class));
		MutablePropertySources propertySources = environment.getPropertySources();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME), is(true));
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME), is(true));

		// ServletConfig gets precedence
		assertThat(environment.getProperty("pCommon"), is("pCommonConfigValue"));
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)),
				lessThan(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME))));

		// but all params are available
		assertThat(environment.getProperty("pContext1"), is("pContext1Value"));
		assertThat(environment.getProperty("pConfig1"), is("pConfig1Value"));

		// Servlet* PropertySources have precedence over System* PropertySources
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)),
				lessThan(propertySources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME))));

		// Replace system properties with a mock property source for convenience
		MockPropertySource mockSystemProperties = new MockPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		mockSystemProperties.setProperty("pCommon", "pCommonSysPropsValue");
		mockSystemProperties.setProperty("pSysProps1", "pSysProps1Value");
		propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockSystemProperties);

		// assert that servletconfig params resolve with higher precedence than sysprops
		assertThat(environment.getProperty("pCommon"), is("pCommonConfigValue"));
		assertThat(environment.getProperty("pSysProps1"), is("pSysProps1Value"));
	}

	@Test
	public void registerServletParamPropertySources_GenericWebApplicationContext() {
		MockServletContext servletContext = new MockServletContext();
		servletContext.addInitParameter("pCommon", "pCommonContextValue");
		servletContext.addInitParameter("pContext1", "pContext1Value");

		GenericWebApplicationContext ctx = new GenericWebApplicationContext();
		ctx.setServletContext(servletContext);
		ctx.refresh();

		ConfigurableEnvironment environment = ctx.getEnvironment();
		assertThat(environment, instanceOf(StandardServletEnvironment.class));
		MutablePropertySources propertySources = environment.getPropertySources();
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME), is(true));

		// ServletContext params are available
		assertThat(environment.getProperty("pCommon"), is("pCommonContextValue"));
		assertThat(environment.getProperty("pContext1"), is("pContext1Value"));

		// Servlet* PropertySources have precedence over System* PropertySources
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)),
				lessThan(propertySources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME))));

		// Replace system properties with a mock property source for convenience
		MockPropertySource mockSystemProperties = new MockPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		mockSystemProperties.setProperty("pCommon", "pCommonSysPropsValue");
		mockSystemProperties.setProperty("pSysProps1", "pSysProps1Value");
		propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockSystemProperties);

		// assert that servletcontext init params resolve with higher precedence than sysprops
		assertThat(environment.getProperty("pCommon"), is("pCommonContextValue"));
		assertThat(environment.getProperty("pSysProps1"), is("pSysProps1Value"));
	}

	@Test
	public void registerServletParamPropertySources_StaticWebApplicationContext() {
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
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME), is(true));
		assertThat(propertySources.contains(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME), is(true));

		// ServletConfig gets precedence
		assertThat(environment.getProperty("pCommon"), is("pCommonConfigValue"));
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)),
				lessThan(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME))));

		// but all params are available
		assertThat(environment.getProperty("pContext1"), is("pContext1Value"));
		assertThat(environment.getProperty("pConfig1"), is("pConfig1Value"));

		// Servlet* PropertySources have precedence over System* PropertySources
		assertThat(propertySources.precedenceOf(PropertySource.named(StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)),
				lessThan(propertySources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME))));

		// Replace system properties with a mock property source for convenience
		MockPropertySource mockSystemProperties = new MockPropertySource(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		mockSystemProperties.setProperty("pCommon", "pCommonSysPropsValue");
		mockSystemProperties.setProperty("pSysProps1", "pSysProps1Value");
		propertySources.replace(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, mockSystemProperties);

		// assert that servletconfig params resolve with higher precedence than sysprops
		assertThat(environment.getProperty("pCommon"), is("pCommonConfigValue"));
		assertThat(environment.getProperty("pSysProps1"), is("pSysProps1Value"));
	}

	@Test
	public void resourceAdapterApplicationContext() {
		ResourceAdapterApplicationContext ctx = new ResourceAdapterApplicationContext(new SimpleBootstrapContext(new SimpleTaskWorkManager()));

		assertHasStandardEnvironment(ctx);

		registerEnvironmentBeanDefinition(ctx);

		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	public void staticPortletApplicationContext() {
		StaticPortletApplicationContext ctx = new StaticPortletApplicationContext();

		assertHasStandardPortletEnvironment(ctx);

		registerEnvironmentBeanDefinition(ctx);

		ctx.setEnvironment(prodEnv);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
	}

	@Test
	public void xmlPortletApplicationContext() {
		AbstractRefreshablePortletApplicationContext ctx = new XmlPortletApplicationContext();
		ctx.setEnvironment(prodEnv);
		ctx.setConfigLocation("classpath:" + XML_PATH);
		ctx.refresh();

		assertHasEnvironment(ctx, prodEnv);
		assertEnvironmentBeanRegistered(ctx);
		assertEnvironmentAwareInvoked(ctx, prodEnv);
		assertThat(ctx.containsBean(DEV_BEAN_NAME), is(false));
		assertThat(ctx.containsBean(PROD_BEAN_NAME), is(true));
	}

	@Test
	public void abstractApplicationContextValidatesRequiredPropertiesOnRefresh() {
		{
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.refresh();
		}

		{
			ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setRequiredProperties("foo", "bar");
			try {
				ctx.refresh();
				fail("expected missing property exception");
			} catch (MissingRequiredPropertiesException ex) {
			}
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
		assertThat(ctx.containsBean(ENVIRONMENT_BEAN_NAME), is(true));
	}

	private void assertHasStandardEnvironment(ApplicationContext ctx) {
		Environment defaultEnv = ctx.getEnvironment();
		assertThat(defaultEnv, notNullValue());
		assertThat(defaultEnv, instanceOf(StandardEnvironment.class));
	}

	private void assertHasStandardServletEnvironment(WebApplicationContext ctx) {
		// ensure a default servlet environment exists
		Environment defaultEnv = ctx.getEnvironment();
		assertThat(defaultEnv, notNullValue());
		assertThat(defaultEnv, instanceOf(StandardServletEnvironment.class));
	}

	private void assertHasStandardPortletEnvironment(WebApplicationContext ctx) {
		// ensure a default portlet environment exists
		Environment defaultEnv = ctx.getEnvironment();
		assertThat(defaultEnv, notNullValue());
		assertThat(defaultEnv, instanceOf(StandardPortletEnvironment.class));
	}

	private void assertHasEnvironment(ApplicationContext ctx, Environment expectedEnv) {
		// ensure the custom environment took
		Environment actualEnv = ctx.getEnvironment();
		assertThat(actualEnv, notNullValue());
		assertThat(actualEnv, is(expectedEnv));
		// ensure environment is registered as a bean
		assertThat(ctx.containsBean(ENVIRONMENT_BEAN_NAME), is(true));
	}

	private void assertEnvironmentAwareInvoked(ConfigurableApplicationContext ctx, Environment expectedEnv) {
		assertThat(ctx.getBean(EnvironmentAwareBean.class).environment, is(expectedEnv));
	}

	private static class EnvironmentAwareBean implements EnvironmentAware {

		public Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

	/**
	 * Mirrors the structure of beans and environment-specific config files
	 * in EnvironmentIntegrationTests-context.xml
	 */
	@Configuration
	@Import({DevConfig.class, ProdConfig.class})
	static class Config {
		@Bean
		public EnvironmentAwareBean envAwareBean() {
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
}
