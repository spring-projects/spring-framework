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

package org.springframework.orm.jpa.persistenceunit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.instrument.classloading.SimpleThrowawayClassLoader;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Subclass of {@link MutablePersistenceUnitInfo} that adds instrumentation hooks based on
 * Spring's {@link org.springframework.instrument.classloading.LoadTimeWeaver} abstraction.
 *
 * <p>As of 7.0, this class is public for custom bootstrapping purposes. A fully configured
 * {@code SpringPersistenceUnitInfo} instance can be turned into a standard JPA descriptor
 * through {@link #asStandardPersistenceUnitInfo} (returning a JPA 3.2/4.0 adapted proxy).
 *
 * <p>Note: For post-processing within a {@code LocalContainerEntityManagerFactoryBean}
 * bootstrap, the base type {@code MutablePersistenceUnitInfo} is entirely sufficient.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 7.0
 * @see DefaultPersistenceUnitManager
 */
public class SpringPersistenceUnitInfo extends MutablePersistenceUnitInfo {

	private @Nullable LoadTimeWeaver loadTimeWeaver;

	private @Nullable ClassLoader classLoader;

	private @Nullable String scopeAnnotationName;

	private final List<String> qualifierAnnotationNames = new ArrayList<>();


	/**
	 * Construct a new SpringPersistenceUnitInfo for custom purposes.
	 * @param loadTimeWeaver the LoadTimeWeaver to use
	 */
	public SpringPersistenceUnitInfo(LoadTimeWeaver loadTimeWeaver) {
		init(loadTimeWeaver);
	}

	/**
	 * Construct a new SpringPersistenceUnitInfo for custom purposes.
	 * @param classLoader the ClassLoader to use
	 */
	public SpringPersistenceUnitInfo(ClassLoader classLoader) {
		init(classLoader);
	}

	/**
	 * Construct a SpringPersistenceUnitInfo for internal purposes.
	 * @see #init(LoadTimeWeaver)
	 * @see #init(ClassLoader)
	 */
	SpringPersistenceUnitInfo() {
	}


	/**
	 * Initialize this PersistenceUnitInfo with the LoadTimeWeaver SPI interface
	 * used by Spring to add instrumentation to the current class loader.
	 */
	void init(LoadTimeWeaver loadTimeWeaver) {
		Assert.notNull(loadTimeWeaver, "LoadTimeWeaver must not be null");
		this.loadTimeWeaver = loadTimeWeaver;
		this.classLoader = loadTimeWeaver.getInstrumentableClassLoader();
	}

	/**
	 * Initialize this PersistenceUnitInfo with the current class loader
	 * (instead of with a LoadTimeWeaver).
	 */
	void init(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * This implementation returns the LoadTimeWeaver's instrumentable ClassLoader,
	 * if specified.
	 */
	public @Nullable ClassLoader getClassLoader() {
		return this.classLoader;
	}

	/**
	 * This implementation delegates to the LoadTimeWeaver, if specified.
	 */
	public void addTransformer(ClassTransformer classTransformer) {
		if (this.loadTimeWeaver != null) {
			this.loadTimeWeaver.addTransformer(new ClassFileTransformerAdapter(classTransformer));
		}
		else {
			LogFactory.getLog(getClass()).info("No LoadTimeWeaver setup: ignoring JPA class transformer");
		}
	}

	/**
	 * This implementation delegates to the LoadTimeWeaver, if specified.
	 */
	public ClassLoader getNewTempClassLoader() {
		ClassLoader tcl = (this.loadTimeWeaver != null ? this.loadTimeWeaver.getThrowawayClassLoader() :
				new SimpleThrowawayClassLoader(this.classLoader));
		String packageToExclude = getPersistenceProviderPackageName();
		if (packageToExclude != null && tcl instanceof DecoratingClassLoader dcl) {
			dcl.excludePackage(packageToExclude);
		}
		return tcl;
	}

	public void setScopeAnnotationName(@Nullable String scopeAnnotationName) {
		this.scopeAnnotationName = scopeAnnotationName;
	}

	public @Nullable String getScopeAnnotationName() {
		return this.scopeAnnotationName;
	}

	public void addQualifierAnnotationName(String qualifierAnnotationName) {
		this.qualifierAnnotationNames.add(qualifierAnnotationName);
	}

	public List<String> getQualifierAnnotationNames() {
		return this.qualifierAnnotationNames;
	}


	/**
	 * Apply the given {@link PersistenceManagedTypes} to this persistence unit,
	 * typically coming from Spring AOT.
	 * @param managedTypes the managed persistent types to register
	 * @since 7.0
	 */
	public void apply(PersistenceManagedTypes managedTypes) {
		Assert.notNull(managedTypes, "PersistenceManagedTypes must not be null");
		managedTypes.getManagedClassNames().forEach(this::addManagedClassName);
		managedTypes.getManagedPackages().forEach(this::addManagedPackage);
		URL persistenceUnitRootUrl = managedTypes.getPersistenceUnitRootUrl();
		if (getPersistenceUnitRootUrl() == null && persistenceUnitRootUrl != null) {
			setPersistenceUnitRootUrl(persistenceUnitRootUrl);
		}
	}

	/**
	 * Apply the given JPA 3.2 {@link PersistenceConfiguration} to this persistence unit,
	 * copying all applicable settings.
	 * <p>Beyond the standard {@code PersistenceConfiguration} settings, "rootUrl" and
	 * "jarFileUrls" from {@link org.hibernate.jpa.HibernatePersistenceConfiguration}
	 * are also detected and applied.
	 * @param config the JPA persistence configuration to apply
	 * @param dataSourceLookup the JDBC DataSourceLookup that provides DataSources for the
	 * persistence provider, resolving data source names in {@code PersistenceConfiguration}
	 * against Spring-managed DataSource instances
	 * @since 7.0
	 */
	@SuppressWarnings("unchecked")
	public void apply(PersistenceConfiguration config, DataSourceLookup dataSourceLookup) {
		Assert.notNull(config, "PersistenceConfiguration must not be null");

		setPersistenceUnitName(config.name());
		setPersistenceProviderClassName(config.provider());
		setTransactionType(config.transactionType());

		if (config.nonJtaDataSource() != null) {
			setNonJtaDataSource(dataSourceLookup.getDataSource(config.nonJtaDataSource()));
		}
		if (config.jtaDataSource() != null) {
			setJtaDataSource(dataSourceLookup.getDataSource(config.jtaDataSource()));
		}

		config.mappingFiles().forEach(this::addMappingFileName);
		config.managedClasses().forEach(clazz -> addManagedClassName(clazz.getName()));

		setSharedCacheMode(config.sharedCacheMode());
		setValidationMode(config.validationMode());
		getProperties().putAll(config.properties());

		// Further relevant settings from HibernatePersistenceConfiguration on Hibernate 7.1+
		Method rootUrl = ClassUtils.getMethodIfAvailable(config.getClass(), "rootUrl");
		if (rootUrl != null) {
			setPersistenceUnitRootUrl((URL) ReflectionUtils.invokeMethod(rootUrl, config));
		}
		Method jarFileUrls = ClassUtils.getMethodIfAvailable(config.getClass(), "jarFileUrls");
		if (jarFileUrls != null) {
			List<URL> urlList = ((List<URL>) ReflectionUtils.invokeMethod(jarFileUrls, config));
			if (urlList != null) {
				urlList.forEach(this::addJarFileUrl);
			}
		}
	}

	/**
	 * Expose a standard {@code jakarta.persistence.spi.PersistenceUnitInfo} proxy for the
	 * persistence unit configuration in this {@code SpringPersistenceUnitInfo} instance.
	 * <p>The returned proxy implements {@code jakarta.persistence.spi.PersistenceUnitInfo}
	 * (and its extended variant {@link SmartPersistenceUnitInfo}) for use with persistence
	 * provider bootstrapping. Note that the returned proxy is effectively unmodifiable and
	 * cannot be downcast to {@code Mutable/SpringPersistenceUnitInfo}.
	 */
	public PersistenceUnitInfo asStandardPersistenceUnitInfo() {
		return (PersistenceUnitInfo) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] {SmartPersistenceUnitInfo.class},
				new SmartPersistenceUnitInfoInvocationHandler());
	}


	/**
	 * Invocation handler for a JPA-compliant {@link SmartPersistenceUnitInfo} proxy,
	 * delegating to the corresponding methods on {@link SpringPersistenceUnitInfo}.
	 */
	private class SmartPersistenceUnitInfoInvocationHandler implements InvocationHandler {

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Fast path for SmartPersistenceUnitInfo JTA check
			if (method.getName().equals("isConfiguredForJta")) {
				return (getTransactionType() == PersistenceUnitTransactionType.JTA);
			}

			// Regular methods to be delegated to SpringPersistenceUnitInfo
			Method targetMethod = SpringPersistenceUnitInfo.class.getMethod(method.getName(), method.getParameterTypes());
			Object returnValue = ReflectionUtils.invokeMethod(targetMethod, SpringPersistenceUnitInfo.this, args);

			// Special handling for JPA 3.2 vs 4.0 getTransactionType() return type
			Class<?> returnType = method.getReturnType();
			if (returnType.isEnum() && returnValue != null && !returnType.isInstance(returnValue)) {
				return Enum.valueOf((Class<Enum>) returnType, returnValue.toString());
			}

			return returnValue;
		}
	}

}
