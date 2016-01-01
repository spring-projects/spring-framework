/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.orm.jpa;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Abstract {@link org.springframework.beans.factory.FactoryBean} that creates
 * a local JPA {@link javax.persistence.EntityManagerFactory} instance within
 * a Spring application context.
 *
 * <p>Encapsulates the common functionality between the different JPA bootstrap
 * contracts (standalone as well as container).
 *
 * <p>Implements support for standard JPA configuration conventions as well as
 * Spring's customizable {@link JpaVendorAdapter} mechanism, and controls the
 * EntityManagerFactory's lifecycle.
 *
 * <p>This class also implements the
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator}
 * interface, as autodetected by Spring's
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor},
 * for AOP-based translation of native exceptions to Spring DataAccessExceptions.
 * Hence, the presence of e.g. LocalEntityManagerFactoryBean automatically enables
 * a PersistenceExceptionTranslationPostProcessor to translate JPA exceptions.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see LocalEntityManagerFactoryBean
 * @see LocalContainerEntityManagerFactoryBean
 */
@SuppressWarnings("serial")
public abstract class AbstractEntityManagerFactoryBean implements
		FactoryBean<EntityManagerFactory>, BeanClassLoaderAware, BeanFactoryAware, BeanNameAware,
		InitializingBean, DisposableBean, EntityManagerFactoryInfo, PersistenceExceptionTranslator, Serializable {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private PersistenceProvider persistenceProvider;

	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<String, Object>();

	private Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	private Class<? extends EntityManager> entityManagerInterface;

	private JpaDialect jpaDialect;

	private JpaVendorAdapter jpaVendorAdapter;

	private AsyncTaskExecutor bootstrapExecutor;

	private ClassLoader beanClassLoader = getClass().getClassLoader();

	private BeanFactory beanFactory;

	private String beanName;

	/** Raw EntityManagerFactory as returned by the PersistenceProvider */
	private EntityManagerFactory nativeEntityManagerFactory;

	/** Future for lazily initializing raw target EntityManagerFactory */
	private Future<EntityManagerFactory> nativeEntityManagerFactoryFuture;

	/** Exposed client-level EntityManagerFactory proxy */
	private EntityManagerFactory entityManagerFactory;


	/**
	 * Set the PersistenceProvider implementation class to use for creating the
	 * EntityManagerFactory. If not specified, the persistence provider will be
	 * taken from the JpaVendorAdapter (if any) or retrieved through scanning
	 * (as far as possible).
	 * @see JpaVendorAdapter#getPersistenceProvider()
	 * @see javax.persistence.spi.PersistenceProvider
	 * @see javax.persistence.Persistence
	 */
	public void setPersistenceProviderClass(Class<? extends PersistenceProvider> persistenceProviderClass) {
		Assert.isAssignable(PersistenceProvider.class, persistenceProviderClass);
		this.persistenceProvider = BeanUtils.instantiateClass(persistenceProviderClass);
	}

	/**
	 * Set the PersistenceProvider instance to use for creating the
	 * EntityManagerFactory. If not specified, the persistence provider
	 * will be taken from the JpaVendorAdapter (if any) or determined
	 * by the persistence unit deployment descriptor (as far as possible).
	 * @see JpaVendorAdapter#getPersistenceProvider()
	 * @see javax.persistence.spi.PersistenceProvider
	 * @see javax.persistence.Persistence
	 */
	public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	/**
	 * Specify the name of the EntityManagerFactory configuration.
	 * <p>Default is none, indicating the default EntityManagerFactory
	 * configuration. The persistence provider will throw an exception if
	 * ambiguous EntityManager configurations are found.
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String)
	 */
	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * Specify JPA properties, to be passed into
	 * {@code Persistence.createEntityManagerFactory} (if any).
	 * <p>Can be populated with a String "value" (parsed via PropertiesEditor) or a
	 * "props" element in XML bean definitions.
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String, java.util.Map)
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory(javax.persistence.spi.PersistenceUnitInfo, java.util.Map)
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * Specify JPA properties as a Map, to be passed into
	 * {@code Persistence.createEntityManagerFactory} (if any).
	 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String, java.util.Map)
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory(javax.persistence.spi.PersistenceUnitInfo, java.util.Map)
	 */
	public void setJpaPropertyMap(Map<String, ?> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * Allow Map access to the JPA properties to be passed to the persistence
	 * provider, with the option to add or override specific entries.
	 * <p>Useful for specifying entries directly, for example via
	 * "jpaPropertyMap[myKey]".
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * Specify the (potentially vendor-specific) EntityManagerFactory interface
	 * that this EntityManagerFactory proxy is supposed to implement.
	 * <p>The default will be taken from the specific JpaVendorAdapter, if any,
	 * or set to the standard {@code javax.persistence.EntityManagerFactory}
	 * interface else.
	 * @see JpaVendorAdapter#getEntityManagerFactoryInterface()
	 */
	public void setEntityManagerFactoryInterface(Class<? extends EntityManagerFactory> emfInterface) {
		Assert.isAssignable(EntityManagerFactory.class, emfInterface);
		this.entityManagerFactoryInterface = emfInterface;
	}

	/**
	 * Specify the (potentially vendor-specific) EntityManager interface
	 * that this factory's EntityManagers are supposed to implement.
	 * <p>The default will be taken from the specific JpaVendorAdapter, if any,
	 * or set to the standard {@code javax.persistence.EntityManager}
	 * interface else.
	 * @see JpaVendorAdapter#getEntityManagerInterface()
	 * @see EntityManagerFactoryInfo#getEntityManagerInterface()
	 */
	public void setEntityManagerInterface(Class<? extends EntityManager> emInterface) {
		Assert.isAssignable(EntityManager.class, emInterface);
		this.entityManagerInterface = emInterface;
	}

	@Override
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

	/**
	 * Specify the vendor-specific JpaDialect implementation to associate with
	 * this EntityManagerFactory. This will be exposed through the
	 * EntityManagerFactoryInfo interface, to be picked up as default dialect by
	 * accessors that intend to use JpaDialect functionality.
	 * @see EntityManagerFactoryInfo#getJpaDialect()
	 */
	public void setJpaDialect(JpaDialect jpaDialect) {
		this.jpaDialect = jpaDialect;
	}

	@Override
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * Specify the JpaVendorAdapter implementation for the desired JPA provider,
	 * if any. This will initialize appropriate defaults for the given provider,
	 * such as persistence provider class and JpaDialect, unless locally
	 * overridden in this FactoryBean.
	 */
	public void setJpaVendorAdapter(JpaVendorAdapter jpaVendorAdapter) {
		this.jpaVendorAdapter = jpaVendorAdapter;
	}

	/**
	 * Return the JpaVendorAdapter implementation for this
	 * EntityManagerFactory, or {@code null} if not known.
	 */
	public JpaVendorAdapter getJpaVendorAdapter() {
		return this.jpaVendorAdapter;
	}

	/**
	 * Specify an asynchronous executor for background bootstrapping,
	 * e.g. a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 * <p>{@code EntityManagerFactory} initialization will then switch into background
	 * bootstrap mode, with a {@code EntityManagerFactory} proxy immediately returned for
	 * injection purposes instead of waiting for the JPA provider's bootstrapping to complete.
	 * However, note that the first actual call to a {@code EntityManagerFactory} method will
	 * then block until the JPA provider's bootstrapping completed, if not ready by then.
	 * For maximum benefit, make sure to avoid early {@code EntityManagerFactory} calls
	 * in init methods of related beans, even for metadata introspection purposes.
	 * @since 4.3
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * Return the asynchronous executor for background bootstrapping, if any.
	 * @since 4.3
	 */
	public AsyncTaskExecutor getBootstrapExecutor() {
		return this.bootstrapExecutor;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public final void afterPropertiesSet() throws PersistenceException {
		if (this.jpaVendorAdapter != null) {
			if (this.persistenceProvider == null) {
				this.persistenceProvider = this.jpaVendorAdapter.getPersistenceProvider();
			}
			Map<String, ?> vendorPropertyMap = this.jpaVendorAdapter.getJpaPropertyMap();
			if (vendorPropertyMap != null) {
				for (Map.Entry<String, ?> entry : vendorPropertyMap.entrySet()) {
					if (!this.jpaPropertyMap.containsKey(entry.getKey())) {
						this.jpaPropertyMap.put(entry.getKey(), entry.getValue());
					}
				}
			}
			if (this.entityManagerFactoryInterface == null) {
				this.entityManagerFactoryInterface = this.jpaVendorAdapter.getEntityManagerFactoryInterface();
				if (!ClassUtils.isVisible(this.entityManagerFactoryInterface, this.beanClassLoader)) {
					this.entityManagerFactoryInterface = EntityManagerFactory.class;
				}
			}
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = this.jpaVendorAdapter.getEntityManagerInterface();
				if (!ClassUtils.isVisible(this.entityManagerInterface, this.beanClassLoader)) {
					this.entityManagerInterface = EntityManager.class;
				}
			}
			if (this.jpaDialect == null) {
				this.jpaDialect = this.jpaVendorAdapter.getJpaDialect();
			}
		}

		if (this.bootstrapExecutor != null) {
			this.nativeEntityManagerFactoryFuture = this.bootstrapExecutor.submit(new Callable<EntityManagerFactory>() {
				@Override
				public EntityManagerFactory call() {
					return buildNativeEntityManagerFactory();
				}
			});
		}
		else {
			this.nativeEntityManagerFactory = buildNativeEntityManagerFactory();
		}

		// Wrap the EntityManagerFactory in a factory implementing all its interfaces.
		// This allows interception of createEntityManager methods to return an
		// application-managed EntityManager proxy that automatically joins
		// existing transactions.
		this.entityManagerFactory = createEntityManagerFactoryProxy(this.nativeEntityManagerFactory);
	}

	private EntityManagerFactory buildNativeEntityManagerFactory() {
		EntityManagerFactory emf = createNativeEntityManagerFactory();
		if (emf == null) {
			throw new IllegalStateException(
					"JPA PersistenceProvider returned null EntityManagerFactory - check your JPA provider setup!");
		}
		if (this.jpaVendorAdapter != null) {
			this.jpaVendorAdapter.postProcessEntityManagerFactory(emf);
		}
		if (logger.isInfoEnabled()) {
			logger.info("Initialized JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		return emf;
	}

	/**
	 * Create a proxy of the given EntityManagerFactory. We do this to be able
	 * to return transaction-aware proxies for application-managed
	 * EntityManagers, and to introduce the NamedEntityManagerFactory interface
	 * @param emf EntityManagerFactory as returned by the persistence provider
	 * @return proxy entity manager
	 */
	protected EntityManagerFactory createEntityManagerFactoryProxy(EntityManagerFactory emf) {
		Set<Class<?>> ifcs = new LinkedHashSet<Class<?>>();
		if (this.entityManagerFactoryInterface != null) {
			ifcs.add(this.entityManagerFactoryInterface);
		}
		else if (emf != null) {
			ifcs.addAll(ClassUtils.getAllInterfacesForClassAsSet(emf.getClass(), this.beanClassLoader));
		}
		else {
			ifcs.add(EntityManagerFactory.class);
		}
		ifcs.add(EntityManagerFactoryInfo.class);
		try {
			return (EntityManagerFactory) Proxy.newProxyInstance(
					this.beanClassLoader, ifcs.toArray(new Class<?>[ifcs.size()]),
					new ManagedEntityManagerFactoryInvocationHandler(this));
		}
		catch (IllegalArgumentException ex) {
			if (this.entityManagerFactoryInterface != null) {
				throw new IllegalStateException("EntityManagerFactory interface [" + this.entityManagerFactoryInterface +
						"] seems to conflict with Spring's EntityManagerFactoryInfo mixin - consider resetting the "+
						"'entityManagerFactoryInterface' property to plain [javax.persistence.EntityManagerFactory]", ex);
			}
			else {
				throw new IllegalStateException("Conflicting EntityManagerFactory interfaces - " +
						"consider specifying the 'jpaVendorAdapter' or 'entityManagerFactoryInterface' property " +
						"to select a specific EntityManagerFactory interface to proceed with", ex);
			}
		}
	}

	/**
	 * Delegate an incoming invocation from the proxy, dispatching to EntityManagerFactoryInfo
	 * or the native EntityManagerFactory accordingly.
	 */
	Object invokeProxyMethod(Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(EntityManagerFactoryInfo.class)) {
			return method.invoke(this, args);
		}
		else if (method.getName().equals("createEntityManager") && args != null && args.length > 0 &&
				args[0] != null && args[0].getClass().isEnum() && "SYNCHRONIZED".equals(args[0].toString())) {
			// JPA 2.1's createEntityManager(SynchronizationType, Map)
			// Redirect to plain createEntityManager and add synchronization semantics through Spring proxy
			EntityManager rawEntityManager = (args.length > 1 ?
					getNativeEntityManagerFactory().createEntityManager((Map<?, ?>) args[1]) :
					getNativeEntityManagerFactory().createEntityManager());
			return ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, true);
		}

		// Standard delegation to the native factory, just post-processing EntityManager return values
		Object retVal = method.invoke(getNativeEntityManagerFactory(), args);
		if (retVal instanceof EntityManager) {
			// Any other createEntityManager variant - expecting non-synchronized semantics
			EntityManager rawEntityManager = (EntityManager) retVal;
			retVal = ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, false);
		}
		return retVal;
	}

	/**
	 * Subclasses must implement this method to create the EntityManagerFactory
	 * that will be returned by the {@code getObject()} method.
	 * @return EntityManagerFactory instance returned by this FactoryBean
	 * @throws PersistenceException if the EntityManager cannot be created
	 */
	protected abstract EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException;


	/**
	 * Implementation of the PersistenceExceptionTranslator interface, as
	 * autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
	 * <p>Uses the dialect's conversion if possible; otherwise falls back to
	 * standard JPA exception conversion.
	 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
	 * @see JpaDialect#translateExceptionIfPossible
	 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
	 */
	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return (this.jpaDialect != null ? this.jpaDialect.translateExceptionIfPossible(ex) :
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex));
	}

	@Override
	public EntityManagerFactory getNativeEntityManagerFactory() {
		if (this.nativeEntityManagerFactory != null) {
			return this.nativeEntityManagerFactory;
		}
		else {
			try {
				return this.nativeEntityManagerFactoryFuture.get();
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException("Interrupted during initialization of native EntityManagerFactory: " +
						ex.getMessage());
			}
			catch (ExecutionException ex) {
				throw new IllegalStateException("Failed to asynchronously initialize native EntityManagerFactory: " +
						ex.getMessage(), ex.getCause());
			}
		}
	}

	@Override
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return null;
	}

	@Override
	public DataSource getDataSource() {
		return null;
	}


	/**
	 * Return the singleton EntityManagerFactory.
	 */
	@Override
	public EntityManagerFactory getObject() {
		return this.entityManagerFactory;
	}

	@Override
	public Class<? extends EntityManagerFactory> getObjectType() {
		return (this.entityManagerFactory != null ? this.entityManagerFactory.getClass() : EntityManagerFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * Close the EntityManagerFactory on bean factory shutdown.
	 */
	@Override
	public void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Closing JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		this.entityManagerFactory.close();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("An EntityManagerFactoryBean itself is not deserializable - " +
				"just a SerializedEntityManagerFactoryBeanReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.beanFactory != null && this.beanName != null) {
			return new SerializedEntityManagerFactoryBeanReference(this.beanFactory, this.beanName);
		}
		else {
			throw new NotSerializableException("EntityManagerFactoryBean does not run within a BeanFactory");
		}
	}


	/**
	 * Minimal bean reference to the surrounding AbstractEntityManagerFactoryBean.
	 * Resolved to the actual AbstractEntityManagerFactoryBean instance on deserialization.
	 */
	@SuppressWarnings("serial")
	private static class SerializedEntityManagerFactoryBeanReference implements Serializable {

		private final BeanFactory beanFactory;

		private final String lookupName;

		public SerializedEntityManagerFactoryBeanReference(BeanFactory beanFactory, String beanName) {
			this.beanFactory = beanFactory;
			this.lookupName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
		}

		private Object readResolve() {
			return this.beanFactory.getBean(this.lookupName, AbstractEntityManagerFactoryBean.class);
		}
	}


	/**
	 * Dynamic proxy invocation handler proxying an EntityManagerFactory to
	 * return a proxy EntityManager if necessary from createEntityManager()
	 * methods.
	 */
	@SuppressWarnings("serial")
	private static class ManagedEntityManagerFactoryInvocationHandler implements InvocationHandler, Serializable {

		private final AbstractEntityManagerFactoryBean entityManagerFactoryBean;

		public ManagedEntityManagerFactoryInvocationHandler(AbstractEntityManagerFactoryBean emfb) {
			this.entityManagerFactoryBean = emfb;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				if (method.getName().equals("equals")) {
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				}
				else if (method.getName().equals("hashCode")) {
					// Use hashCode of EntityManagerFactory proxy.
					return System.identityHashCode(proxy);
				}
				else if (method.getName().equals("unwrap")) {
					// Handle JPA 2.1 unwrap method - could be a proxy match.
					Class<?> targetClass = (Class<?>) args[0];
					if (targetClass == null || targetClass.isInstance(proxy)) {
						return proxy;
					}
				}
				return this.entityManagerFactoryBean.invokeProxyMethod(method, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
