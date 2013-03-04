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

package org.springframework.test.jpa;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import junit.framework.TestCase;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.ResourceOverridingShadowingClassLoader;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.orm.jpa.ExtendedEntityManagerCreator;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.test.annotation.AbstractAnnotationAwareTransactionalTests;
import org.springframework.util.StringUtils;

/**
 * Convenient support class for JPA-related tests. Offers the same contract as
 * AbstractTransactionalDataSourceSpringContextTests and equally good performance,
 * even when performing the instrumentation required by the JPA specification.
 *
 * <p>Exposes an EntityManagerFactory and a shared EntityManager.
 * Requires an EntityManagerFactory to be injected, plus the DataSource and
 * JpaTransactionManager through the superclass.
 *
 * <p>When using Xerces, make sure a post 2.0.2 version is available on the classpath
 * to avoid a critical
 * <a href="http://nagoya.apache.org/bugzilla/show_bug.cgi?id=16014"/>bug</a>
 * that leads to StackOverflow. Maven users are likely to encounter this problem since
 * 2.0.2 is used by default.
 *
 * <p>A workaround is to explicitly specify the Xerces version inside the Maven POM:
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;xerces&lt;/groupId&gt;
 *     &lt;artifactId&gt;xercesImpl&lt;/artifactId&gt;
 *   &lt;version&gt;2.8.1&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
public abstract class AbstractJpaTests extends AbstractAnnotationAwareTransactionalTests {

	private static final String DEFAULT_ORM_XML_LOCATION = "META-INF/orm.xml";

	/**
	 * Map from String defining unique combination of config locations, to ApplicationContext.
	 * Values are intentionally not strongly typed, to avoid potential class cast exceptions
	 * through use between different class loaders.
	 */
	private static Map<String, Object> contextCache = new HashMap<String, Object>();

	private static Map<String, ClassLoader> classLoaderCache = new HashMap<String, ClassLoader>();

	protected EntityManagerFactory entityManagerFactory;

	/**
	 * If this instance is in a shadow loader, this variable
	 * will contain the parent instance of the subclass.
	 * The class will not be the same as the class of the
	 * shadow instance, as it was loaded by a different class loader,
	 * but it can be invoked reflectively. The shadowParent
	 * and the shadow loader can communicate reflectively
	 * but not through direct invocation.
	 */
	private Object shadowParent;

	/**
	 * Subclasses can use this in test cases.
	 * It will participate in any current transaction.
	 */
	protected EntityManager sharedEntityManager;


	public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
		this.sharedEntityManager = SharedEntityManagerCreator.createSharedEntityManager(this.entityManagerFactory);
	}

	/**
	 * Create an EntityManager that will always automatically enlist itself in current
	 * transactions, in contrast to an EntityManager returned by
	 * {@code EntityManagerFactory.createEntityManager()}
	 * (which requires an explicit {@code joinTransaction()} call).
	 */
	protected EntityManager createContainerManagedEntityManager() {
		return ExtendedEntityManagerCreator.createContainerManagedEntityManager(this.entityManagerFactory);
	}

	/**
	 * Subclasses should override this method if they wish to disable shadow class loading.
	 * <p>The default implementation deactivates shadow class loading if Spring's
	 * InstrumentationSavingAgent has been configured on VM startup.
	 */
	protected boolean shouldUseShadowLoader() {
		return !InstrumentationLoadTimeWeaver.isInstrumentationAvailable();
	}

	@Override
	public void setDirty() {
		super.setDirty();
		contextCache.remove(cacheKeys());
		classLoaderCache.remove(cacheKeys());

		// If we are a shadow loader, we need to invoke
		// the shadow parent to set it dirty, as
		// it is the shadow parent that maintains the cache state,
		// not the child
		if (this.shadowParent != null) {
			try {
				Method m = shadowParent.getClass().getMethod("setDirty", (Class[]) null);
				m.invoke(shadowParent, (Object[]) null);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}


	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void runBare() throws Throwable {

		// getName will return the name of the method being run.
		if (isDisabledInThisEnvironment(getName())) {
			// Let superclass log that we didn't run the test.
			super.runBare();
			return;
		}

		final Method testMethod = getTestMethod();

		if (isDisabledInThisEnvironment(testMethod)) {
			recordDisabled();
			this.logger.info("**** " + getClass().getName() + "." + getName() + " is disabled in this environment: "
					+ "Total disabled tests=" + getDisabledTestCount());
			return;
		}

		if (!shouldUseShadowLoader()) {
			super.runBare();
			return;
		}

		String combinationOfContextLocationsForThisTestClass = cacheKeys();
		ClassLoader classLoaderForThisTestClass = getClass().getClassLoader();
		// save the TCCL
		ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();

		if (this.shadowParent != null) {
			Thread.currentThread().setContextClassLoader(classLoaderForThisTestClass);
			super.runBare();
		}

		else {
			ShadowingClassLoader shadowingClassLoader = (ShadowingClassLoader) classLoaderCache.get(combinationOfContextLocationsForThisTestClass);

			if (shadowingClassLoader == null) {
				shadowingClassLoader = (ShadowingClassLoader) createShadowingClassLoader(classLoaderForThisTestClass);
				classLoaderCache.put(combinationOfContextLocationsForThisTestClass, shadowingClassLoader);
			}
			try {
				Thread.currentThread().setContextClassLoader(shadowingClassLoader);
				String[] configLocations = getConfigLocations();

				// Do not strongly type, to avoid ClassCastException.
				Object cachedContext = contextCache.get(combinationOfContextLocationsForThisTestClass);

				if (cachedContext == null) {

					// Create the LoadTimeWeaver.
					Class shadowingLoadTimeWeaverClass = shadowingClassLoader.loadClass(ShadowingLoadTimeWeaver.class.getName());
					Constructor constructor = shadowingLoadTimeWeaverClass.getConstructor(ClassLoader.class);
					constructor.setAccessible(true);
					Object ltw = constructor.newInstance(shadowingClassLoader);

					// Create the BeanFactory.
					Class beanFactoryClass = shadowingClassLoader.loadClass(DefaultListableBeanFactory.class.getName());
					Object beanFactory = BeanUtils.instantiateClass(beanFactoryClass);

					// Create the BeanDefinitionReader.
					Class beanDefinitionReaderClass = shadowingClassLoader.loadClass(XmlBeanDefinitionReader.class.getName());
					Class beanDefinitionRegistryClass = shadowingClassLoader.loadClass(BeanDefinitionRegistry.class.getName());
					Object reader = beanDefinitionReaderClass.getConstructor(beanDefinitionRegistryClass).newInstance(beanFactory);

					// Load the bean definitions into the BeanFactory.
					Method loadBeanDefinitions = beanDefinitionReaderClass.getMethod("loadBeanDefinitions", String[].class);
					loadBeanDefinitions.invoke(reader, new Object[] {configLocations});

					// Create LoadTimeWeaver-injecting BeanPostProcessor.
					Class loadTimeWeaverInjectingBeanPostProcessorClass = shadowingClassLoader.loadClass(LoadTimeWeaverInjectingBeanPostProcessor.class.getName());
					Class loadTimeWeaverClass = shadowingClassLoader.loadClass(LoadTimeWeaver.class.getName());
					Constructor bppConstructor = loadTimeWeaverInjectingBeanPostProcessorClass.getConstructor(loadTimeWeaverClass);
					bppConstructor.setAccessible(true);
					Object beanPostProcessor = bppConstructor.newInstance(ltw);

					// Add LoadTimeWeaver-injecting BeanPostProcessor.
					Class beanPostProcessorClass = shadowingClassLoader.loadClass(BeanPostProcessor.class.getName());
					Method addBeanPostProcessor = beanFactoryClass.getMethod("addBeanPostProcessor", beanPostProcessorClass);
					addBeanPostProcessor.invoke(beanFactory, beanPostProcessor);

					// Create the GenericApplicationContext.
					Class genericApplicationContextClass = shadowingClassLoader.loadClass(GenericApplicationContext.class.getName());
					Class defaultListableBeanFactoryClass = shadowingClassLoader.loadClass(DefaultListableBeanFactory.class.getName());
					cachedContext = genericApplicationContextClass.getConstructor(defaultListableBeanFactoryClass).newInstance(beanFactory);

					// Invoke the context's "refresh" method.
					genericApplicationContextClass.getMethod("refresh").invoke(cachedContext);

					// Store the context reference in the cache.
					contextCache.put(combinationOfContextLocationsForThisTestClass, cachedContext);
				}
				// create the shadowed test
				Class shadowedTestClass = shadowingClassLoader.loadClass(getClass().getName());

				// So long as JUnit is excluded from shadowing we
				// can minimize reflective invocation here
				TestCase shadowedTestCase = (TestCase) BeanUtils.instantiateClass(shadowedTestClass);

				/* shadowParent = this */
				Class thisShadowedClass = shadowingClassLoader.loadClass(AbstractJpaTests.class.getName());
				Field shadowed = thisShadowedClass.getDeclaredField("shadowParent");
				shadowed.setAccessible(true);
				shadowed.set(shadowedTestCase, this);

				/* AbstractSpringContextTests.addContext(Object, ApplicationContext) */
				Class applicationContextClass = shadowingClassLoader.loadClass(ConfigurableApplicationContext.class.getName());
				Method addContextMethod = shadowedTestClass.getMethod("addContext", Object.class, applicationContextClass);
				addContextMethod.invoke(shadowedTestCase, configLocations, cachedContext);

				// Invoke tests on shadowed test case
				shadowedTestCase.setName(getName());
				shadowedTestCase.runBare();
			}
			catch (InvocationTargetException ex) {
				// Unwrap this for better exception reporting
				// when running tests
				throw ex.getTargetException();
			}
			finally {
				Thread.currentThread().setContextClassLoader(initialClassLoader);
			}
		}
	}

	protected String cacheKeys() {
		return StringUtils.arrayToCommaDelimitedString(getConfigLocations());
	}

	/**
	 * NB: This method must <b>not</b> have a return type of ShadowingClassLoader as that would cause that
	 * class to be loaded eagerly when this test case loads, creating verify errors at runtime.
	 */
	protected ClassLoader createShadowingClassLoader(ClassLoader classLoader) {
		OrmXmlOverridingShadowingClassLoader orxl = new OrmXmlOverridingShadowingClassLoader(classLoader,
				getActualOrmXmlLocation());
		customizeResourceOverridingShadowingClassLoader(orxl);
		return orxl;
	}

	/**
	 * Customize the shadowing class loader.
	 * @param shadowingClassLoader this parameter is actually of type
	 * ResourceOverridingShadowingClassLoader, and can safely to be cast to
	 * that type. However, the signature must not be of that type as that
	 * would cause the present class loader to load that type.
	 */
	protected void customizeResourceOverridingShadowingClassLoader(ClassLoader shadowingClassLoader) {
		// empty
	}

	/**
	 * Subclasses can override this to return the real location path for
	 * orm.xml or null if they do not wish to find any orm.xml
	 * @return orm.xml path or null to hide any such file
	 */
	protected String getActualOrmXmlLocation() {
		return DEFAULT_ORM_XML_LOCATION;
	}


	private static class LoadTimeWeaverInjectingBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

		private final LoadTimeWeaver ltw;

		@SuppressWarnings("unused")
		public LoadTimeWeaverInjectingBeanPostProcessor(LoadTimeWeaver ltw) {
			this.ltw = ltw;
		}

		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof LocalContainerEntityManagerFactoryBean) {
				((LocalContainerEntityManagerFactoryBean) bean).setLoadTimeWeaver(this.ltw);
			}
			if (bean instanceof DefaultPersistenceUnitManager) {
				((DefaultPersistenceUnitManager) bean).setLoadTimeWeaver(this.ltw);
			}
			return bean;
		}
	}


	private static class ShadowingLoadTimeWeaver implements LoadTimeWeaver {

		private final ClassLoader shadowingClassLoader;

		@SuppressWarnings("unused")
		public ShadowingLoadTimeWeaver(ClassLoader shadowingClassLoader) {
			this.shadowingClassLoader = shadowingClassLoader;
		}

		public void addTransformer(ClassFileTransformer transformer) {
			try {
				Method addClassFileTransformer =
						this.shadowingClassLoader.getClass().getMethod("addTransformer", ClassFileTransformer.class);
				addClassFileTransformer.setAccessible(true);
				addClassFileTransformer.invoke(this.shadowingClassLoader, transformer);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public ClassLoader getInstrumentableClassLoader() {
			return this.shadowingClassLoader;
		}

		public ClassLoader getThrowawayClassLoader() {
			// Be sure to copy the same resource overrides and same class file transformers:
			// We want the throwaway class loader to behave like the instrumentable class loader.
			ResourceOverridingShadowingClassLoader roscl =
					new ResourceOverridingShadowingClassLoader(getClass().getClassLoader());
			if (this.shadowingClassLoader instanceof ShadowingClassLoader) {
				roscl.copyTransformers((ShadowingClassLoader) this.shadowingClassLoader);
			}
			if (this.shadowingClassLoader instanceof ResourceOverridingShadowingClassLoader) {
				roscl.copyOverrides((ResourceOverridingShadowingClassLoader) this.shadowingClassLoader);
			}
			return roscl;
		}
	}

}
