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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the
 * {@link org.springframework.beans.factory.ListableBeanFactory} and
 * {@link BeanDefinitionRegistry} interfaces: a full-fledged bean factory
 * based on bean definition objects.
 *
 * <p>Typical usage is registering all bean definitions first (possibly read
 * from a bean definition file), before accessing beans. Bean definition lookup
 * is therefore an inexpensive operation in a local bean definition table,
 * operating on pre-built bean definition metadata objects.
 *
 * <p>Can be used as a standalone bean factory, or as a superclass for custom
 * bean factories. Note that readers for specific bean definition formats are
 * typically implemented separately rather than as bean factory subclasses:
 * see for example {@link PropertiesBeanDefinitionReader} and
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * <p>For an alternative implementation of the
 * {@link org.springframework.beans.factory.ListableBeanFactory} interface,
 * have a look at {@link StaticListableBeanFactory}, which manages existing
 * bean instances rather than creating new ones based on bean definitions.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @since 16 April 2001
 * @see StaticListableBeanFactory
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

	private static Class<?> javaxInjectProviderClass = null;

	static {
		ClassLoader cl = DefaultListableBeanFactory.class.getClassLoader();
		try {
			javaxInjectProviderClass = cl.loadClass("javax.inject.Provider");
		}
		catch (ClassNotFoundException ex) {
			// JSR-330 API not available - Provider interface simply not supported then.
		}
	}


	/** Map from serialized id to factory instance */
	private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories =
			new ConcurrentHashMap<String, Reference<DefaultListableBeanFactory>>();

	/** Optional id for this factory, for serialization purposes */
	private String serializationId;

	/** Whether to allow re-registration of a different definition with the same name */
	private boolean allowBeanDefinitionOverriding = true;

	/** Whether to allow eager class loading even for lazy-init beans */
	private boolean allowEagerClassLoading = true;

	/** Resolver to use for checking if a bean definition is an autowire candidate */
	private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

	/** Map from dependency type to corresponding autowired value */
	private final Map<Class<?>, Object> resolvableDependencies = new HashMap<Class<?>, Object>();

	/** Map of bean definition objects, keyed by bean name */
	private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

	/** Map of singleton bean names keyed by bean class */
	private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<Class<?>, String[]>();

	/** Map of non-singleton bean names keyed by bean class */
	private final Map<Class<?>, String[]> nonSingletonBeanNamesByType = new ConcurrentHashMap<Class<?>, String[]>();

	/** List of bean definition names, in registration order */
	private final List<String> beanDefinitionNames = new ArrayList<String>();

	/** Whether bean definition metadata may be cached for all beans */
	private boolean configurationFrozen = false;

	/** Cached array of bean definition names in case of frozen configuration */
	private String[] frozenBeanDefinitionNames;


	/**
	 * Create a new DefaultListableBeanFactory.
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * Create a new DefaultListableBeanFactory with the given parent.
	 * @param parentBeanFactory the parent BeanFactory
	 */
	public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}


	/**
	 * Specify an id for serialization purposes, allowing this BeanFactory to be
	 * deserialized from this id back into the BeanFactory object, if needed.
	 */
	public void setSerializationId(String serializationId) {
		if (serializationId != null) {
			serializableFactories.put(serializationId, new WeakReference<DefaultListableBeanFactory>(this));
		}
		else if (this.serializationId != null) {
			serializableFactories.remove(this.serializationId);
		}
		this.serializationId = serializationId;
	}

	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. This also applies to overriding aliases.
	 * <p>Default is "true".
	 * @see #registerBeanDefinition
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * <p>Default is "true". Turn this flag off to suppress class loading
	 * for lazy-init beans unless such a bean is explicitly requested.
	 * In particular, by-type lookups will then simply ignore bean definitions
	 * without resolved class name, instead of loading the bean classes on
	 * demand just to perform a type check.
	 * @see AbstractBeanDefinition#setLazyInit
	 */
	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
		this.allowEagerClassLoading = allowEagerClassLoading;
	}

	/**
	 * Set a custom autowire candidate resolver for this BeanFactory to use
	 * when deciding whether a bean definition should be considered as a
	 * candidate for autowiring.
	 */
	public void setAutowireCandidateResolver(final AutowireCandidateResolver autowireCandidateResolver) {
		Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
		if (autowireCandidateResolver instanceof BeanFactoryAware) {
			if (System.getSecurityManager() != null) {
				final BeanFactory target = this;
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(target);
						return null;
					}
				}, getAccessControlContext());
			}
			else {
				((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
			}
		}
		this.autowireCandidateResolver = autowireCandidateResolver;
	}

	/**
	 * Return the autowire candidate resolver for this BeanFactory (never <code>null</code>).
	 */
	public AutowireCandidateResolver getAutowireCandidateResolver() {
		return this.autowireCandidateResolver;
	}


	@Override
	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory otherListableFactory = (DefaultListableBeanFactory) otherFactory;
			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
			this.autowireCandidateResolver = otherListableFactory.autowireCandidateResolver;
			this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		Assert.notNull(requiredType, "Required type must not be null");
		String[] beanNames = getBeanNamesForType(requiredType);
		if (beanNames.length > 1) {
			ArrayList<String> autowireCandidates = new ArrayList<String>();
			for (String beanName : beanNames) {
				if (getBeanDefinition(beanName).isAutowireCandidate()) {
					autowireCandidates.add(beanName);
				}
			}
			if (autowireCandidates.size() > 0) {
				beanNames = autowireCandidates.toArray(new String[autowireCandidates.size()]);
			}
		}
		if (beanNames.length == 1) {
			return getBean(beanNames[0], requiredType);
		}
		else if (beanNames.length == 0 && getParentBeanFactory() != null) {
			return getParentBeanFactory().getBean(requiredType);
		}
		else {
			throw new NoSuchBeanDefinitionException(requiredType, "expected single bean but found " +
					beanNames.length + ": " + StringUtils.arrayToCommaDelimitedString(beanNames));
		}
	}

	@Override
	public boolean containsBeanDefinition(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return this.beanDefinitionMap.containsKey(beanName);
	}

	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	public String[] getBeanDefinitionNames() {
		synchronized (this.beanDefinitionMap) {
			if (this.frozenBeanDefinitionNames != null) {
				return this.frozenBeanDefinitionNames;
			}
			else {
				return StringUtils.toStringArray(this.beanDefinitionNames);
			}
		}
	}

	public String[] getBeanNamesForType(Class<?> type) {
		return getBeanNamesForType(type, true, true);
	}

	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if (!isConfigurationFrozen()  || type == null || !allowEagerInit) {
			return doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		}
		Map<Class<?>, String[]> cache = includeNonSingletons ?
				this.nonSingletonBeanNamesByType : this.singletonBeanNamesByType;
		String[] resolvedBeanNames = cache.get(type);
		if (resolvedBeanNames != null) {
			return resolvedBeanNames;
		}
		resolvedBeanNames = doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		cache.put(type, resolvedBeanNames);
		return resolvedBeanNames;
	}

	private String[] doGetBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		List<String> result = new ArrayList<String>();

		// Check all bean definitions.
		String[] beanDefinitionNames = getBeanDefinitionNames();
		for (String beanName : beanDefinitionNames) {
			// Only consider bean as eligible if the bean name
			// is not defined as alias for some other bean.
			if (!isAlias(beanName)) {
				try {
					RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
					// Only check bean definition if it is complete.
					if (!mbd.isAbstract() && (allowEagerInit ||
							((mbd.hasBeanClass() || !mbd.isLazyInit() || this.allowEagerClassLoading)) &&
									!requiresEagerInitForType(mbd.getFactoryBeanName()))) {
						// In case of FactoryBean, match object created by FactoryBean.
						boolean isFactoryBean = isFactoryBean(beanName, mbd);
						boolean matchFound = (allowEagerInit || !isFactoryBean || containsSingleton(beanName)) &&
								(includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type);
						if (!matchFound && isFactoryBean) {
							// In case of FactoryBean, try to match FactoryBean instance itself next.
							beanName = FACTORY_BEAN_PREFIX + beanName;
							matchFound = (includeNonSingletons || mbd.isSingleton()) && isTypeMatch(beanName, type);
						}
						if (matchFound) {
							result.add(beanName);
						}
					}
				}
				catch (CannotLoadBeanClassException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// Probably contains a placeholder: let's ignore it for type matching purposes.
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Ignoring bean class loading failure for bean '" + beanName + "'", ex);
					}
					onSuppressedException(ex);
				}
				catch (BeanDefinitionStoreException ex) {
					if (allowEagerInit) {
						throw ex;
					}
					// Probably contains a placeholder: let's ignore it for type matching purposes.
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Ignoring unresolvable metadata in bean definition '" + beanName + "'", ex);
					}
					onSuppressedException(ex);
				}
			}
		}

		// Check singletons too, to catch manually registered singletons.
		String[] singletonNames = getSingletonNames();
		for (String beanName : singletonNames) {
			// Only check if manually registered.
			if (!containsBeanDefinition(beanName)) {
				// In case of FactoryBean, match object created by FactoryBean.
				if (isFactoryBean(beanName)) {
					if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
						result.add(beanName);
						// Match found for this bean: do not match FactoryBean itself anymore.
						continue;
					}
					// In case of FactoryBean, try to match FactoryBean itself next.
					beanName = FACTORY_BEAN_PREFIX + beanName;
				}
				// Match raw bean instance (might be raw FactoryBean).
				if (isTypeMatch(beanName, type)) {
					result.add(beanName);
				}
			}
		}

		return StringUtils.toStringArray(result);
	}

	/**
	 * Check whether the specified bean would need to be eagerly initialized
	 * in order to determine its type.
	 * @param factoryBeanName a factory-bean reference that the bean definition
	 * defines a factory method for
	 * @return whether eager initialization is necessary
	 */
	private boolean requiresEagerInitForType(String factoryBeanName) {
		return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
		Map<String, T> result = new LinkedHashMap<String, T>(beanNames.length);
		for (String beanName : beanNames) {
			try {
				result.put(beanName, getBean(beanName, type));
			}
			catch (BeanCreationException ex) {
				Throwable rootCause = ex.getMostSpecificCause();
				if (rootCause instanceof BeanCurrentlyInCreationException) {
					BeanCreationException bce = (BeanCreationException) rootCause;
					if (isCurrentlyInCreation(bce.getBeanName())) {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug("Ignoring match to currently created bean '" + beanName + "': " +
									ex.getMessage());
						}
						onSuppressedException(ex);
						// Ignore: indicates a circular reference when autowiring constructors.
						// We want to find matches other than the currently created bean itself.
						continue;
					}
				}
				throw ex;
			}
		}
		return result;
	}

	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
		Set<String> beanNames = new LinkedHashSet<String>(getBeanDefinitionCount());
		beanNames.addAll(Arrays.asList(getBeanDefinitionNames()));
		beanNames.addAll(Arrays.asList(getSingletonNames()));
		Map<String, Object> results = new LinkedHashMap<String, Object>();
		for (String beanName : beanNames) {
			if (findAnnotationOnBean(beanName, annotationType) != null) {
				results.put(beanName, getBean(beanName));
			}
		}
		return results;
	}

	/**
	 * Find a {@link Annotation} of <code>annotationType</code> on the specified
	 * bean, traversing its interfaces and super classes if no annotation can be
	 * found on the given class itself, as well as checking its raw bean class
	 * if not found on the exposed bean reference (e.g. in case of a proxy).
	 */
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
		A ann = null;
		Class<?> beanType = getType(beanName);
		if (beanType != null) {
			ann = AnnotationUtils.findAnnotation(beanType, annotationType);
		}
		if (ann == null && containsBeanDefinition(beanName)) {
			BeanDefinition bd = getMergedBeanDefinition(beanName);
			if (bd instanceof AbstractBeanDefinition) {
				AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
				if (abd.hasBeanClass()) {
					ann = AnnotationUtils.findAnnotation(abd.getBeanClass(), annotationType);
				}
			}
		}
		return ann;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory interface
	//---------------------------------------------------------------------

	public void registerResolvableDependency(Class<?> dependencyType, Object autowiredValue) {
		Assert.notNull(dependencyType, "Type must not be null");
		if (autowiredValue != null) {
			Assert.isTrue((autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue)),
					"Value [" + autowiredValue + "] does not implement specified type [" + dependencyType.getName() + "]");
			this.resolvableDependencies.put(dependencyType, autowiredValue);
		}
	}

	public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException {

		// Consider FactoryBeans as autowiring candidates.
		boolean isFactoryBean = (descriptor != null && descriptor.getDependencyType() != null &&
				FactoryBean.class.isAssignableFrom(descriptor.getDependencyType()));
		if (isFactoryBean) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
		}

		if (containsBeanDefinition(beanName)) {
			return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(beanName), descriptor);
		}
		else if (containsSingleton(beanName)) {
			return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor);
		}
		else if (getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
			// No bean definition found in this factory -> delegate to parent.
			return ((ConfigurableListableBeanFactory) getParentBeanFactory()).isAutowireCandidate(beanName, descriptor);
		}
		else {
			return true;
		}
	}

	/**
	 * Determine whether the specified bean definition qualifies as an autowire candidate,
	 * to be injected into other beans which declare a dependency of matching type.
	 * @param beanName the name of the bean definition to check
	 * @param mbd the merged bean definition to check
	 * @param descriptor the descriptor of the dependency to resolve
	 * @return whether the bean should be considered as autowire candidate
	 */
	protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd, DependencyDescriptor descriptor) {
		resolveBeanClass(mbd, beanName);
		if (mbd.isFactoryMethodUnique) {
			boolean resolve;
			synchronized (mbd.constructorArgumentLock) {
				resolve = (mbd.resolvedConstructorOrFactoryMethod == null);
			}
			if (resolve) {
				new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
			}
		}
		return getAutowireCandidateResolver().isAutowireCandidate(
				new BeanDefinitionHolder(mbd, beanName, getAliases(beanName)), descriptor);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition bd = this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("No bean named '" + beanName + "' found in " + this);
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		return bd;
	}

	public void freezeConfiguration() {
		this.configurationFrozen = true;
		synchronized (this.beanDefinitionMap) {
			this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
		}
	}

	public boolean isConfigurationFrozen() {
		return this.configurationFrozen;
	}

	/**
	 * Considers all beans as eligible for metadata caching
	 * if the factory's configuration has been marked as frozen.
	 * @see #freezeConfiguration()
	 */
	@Override
	protected boolean isBeanEligibleForMetadataCaching(String beanName) {
		return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
	}

	public void preInstantiateSingletons() throws BeansException {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Pre-instantiating singletons in " + this);
		}
		synchronized (this.beanDefinitionMap) {
			// Iterate over a copy to allow for init methods which in turn register new bean definitions.
			// While this may not be part of the regular factory bootstrap, it does otherwise work fine.
			List<String> beanNames = new ArrayList<String>(this.beanDefinitionNames);
			for (String beanName : beanNames) {
				RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
				if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
					if (isFactoryBean(beanName)) {
						final FactoryBean<?> factory = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
						boolean isEagerInit;
						if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
							isEagerInit = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
								public Boolean run() {
									return ((SmartFactoryBean<?>) factory).isEagerInit();
								}
							}, getAccessControlContext());
						}
						else {
							isEagerInit = (factory instanceof SmartFactoryBean &&
									((SmartFactoryBean<?>) factory).isEagerInit());
						}
						if (isEagerInit) {
							getBean(beanName);
						}
					}
					else {
						getBean(beanName);
					}
				}
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry interface
	//---------------------------------------------------------------------

	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		synchronized (this.beanDefinitionMap) {
			Object oldBeanDefinition = this.beanDefinitionMap.get(beanName);
			if (oldBeanDefinition != null) {
				if (!this.allowBeanDefinitionOverriding) {
					throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
							"Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
							"': There is already [" + oldBeanDefinition + "] bound.");
				}
				else {
					if (this.logger.isInfoEnabled()) {
						this.logger.info("Overriding bean definition for bean '" + beanName +
								"': replacing [" + oldBeanDefinition + "] with [" + beanDefinition + "]");
					}
				}
			}
			else {
				this.beanDefinitionNames.add(beanName);
				this.frozenBeanDefinitionNames = null;
			}
			this.beanDefinitionMap.put(beanName, beanDefinition);

			resetBeanDefinition(beanName);
		}
	}

	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		Assert.hasText(beanName, "'beanName' must not be empty");

		synchronized (this.beanDefinitionMap) {
			BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
			if (bd == null) {
				if (this.logger.isTraceEnabled()) {
					this.logger.trace("No bean named '" + beanName + "' found in " + this);
				}
				throw new NoSuchBeanDefinitionException(beanName);
			}
			this.beanDefinitionNames.remove(beanName);
			this.frozenBeanDefinitionNames = null;

			resetBeanDefinition(beanName);
		}
	}

	/**
	 * Reset all bean definition caches for the given bean,
	 * including the caches of beans that are derived from it.
	 * @param beanName the name of the bean to reset
	 */
	protected void resetBeanDefinition(String beanName) {
		// Remove the merged bean definition for the given bean, if already created.
		clearMergedBeanDefinition(beanName);

		// Remove corresponding bean from singleton cache, if any. Shouldn't usually
		// be necessary, rather just meant for overriding a context's default beans
		// (e.g. the default StaticMessageSource in a StaticApplicationContext).
		synchronized (getSingletonMutex()) {
			destroySingleton(beanName);
		}

		// Remove any assumptions about by-type mappings
		this.singletonBeanNamesByType.clear();
		this.nonSingletonBeanNamesByType.clear();

		// Reset all bean definitions that have the given bean as parent (recursively).
		for (String bdName : this.beanDefinitionNames) {
			if (!beanName.equals(bdName)) {
				BeanDefinition bd = this.beanDefinitionMap.get(bdName);
				if (beanName.equals(bd.getParentName())) {
					resetBeanDefinition(bdName);
				}
			}
		}
	}

	/**
	 * Only allows alias overriding if bean definition overriding is allowed.
	 */
	@Override
	protected boolean allowAliasOverriding() {
		return this.allowBeanDefinitionOverriding;
	}


	//---------------------------------------------------------------------
	// Dependency resolution functionality
	//---------------------------------------------------------------------

	public Object resolveDependency(DependencyDescriptor descriptor, String beanName,
			Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException  {

		descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
		if (descriptor.getDependencyType().equals(ObjectFactory.class)) {
			return new DependencyObjectFactory(descriptor, beanName);
		}
		else if (descriptor.getDependencyType().equals(javaxInjectProviderClass)) {
			return new DependencyProviderFactory().createDependencyProvider(descriptor, beanName);
		}
		else {
			return doResolveDependency(descriptor, descriptor.getDependencyType(), beanName, autowiredBeanNames, typeConverter);
		}
	}

	protected Object doResolveDependency(DependencyDescriptor descriptor, Class<?> type, String beanName,
			Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException  {

		Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
		if (value != null) {
			if (value instanceof String) {
				String strVal = resolveEmbeddedValue((String) value);
				BeanDefinition bd = (beanName != null && containsBean(beanName) ? getMergedBeanDefinition(beanName) : null);
				value = evaluateBeanDefinitionString(strVal, bd);
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			return converter.convertIfNecessary(value, type);
		}

		if (type.isArray()) {
			Class<?> componentType = type.getComponentType();
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType, descriptor);
			if (matchingBeans.isEmpty()) {
				if (descriptor.isRequired()) {
					raiseNoSuchBeanDefinitionException(componentType, "array of " + componentType.getName(), descriptor);
				}
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			return converter.convertIfNecessary(matchingBeans.values(), type);
		}
		else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
			Class<?> elementType = descriptor.getCollectionType();
			if (elementType == null) {
				if (descriptor.isRequired()) {
					throw new FatalBeanException("No element type declared for collection [" + type.getName() + "]");
				}
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType, descriptor);
			if (matchingBeans.isEmpty()) {
				if (descriptor.isRequired()) {
					raiseNoSuchBeanDefinitionException(elementType, "collection of " + elementType.getName(), descriptor);
				}
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
			return converter.convertIfNecessary(matchingBeans.values(), type);
		}
		else if (Map.class.isAssignableFrom(type) && type.isInterface()) {
			Class<?> keyType = descriptor.getMapKeyType();
			if (keyType == null || !String.class.isAssignableFrom(keyType)) {
				if (descriptor.isRequired()) {
					throw new FatalBeanException("Key type [" + keyType + "] of map [" + type.getName() +
							"] must be assignable to [java.lang.String]");
				}
				return null;
			}
			Class<?> valueType = descriptor.getMapValueType();
			if (valueType == null) {
				if (descriptor.isRequired()) {
					throw new FatalBeanException("No value type declared for map [" + type.getName() + "]");
				}
				return null;
			}
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType, descriptor);
			if (matchingBeans.isEmpty()) {
				if (descriptor.isRequired()) {
					raiseNoSuchBeanDefinitionException(valueType, "map with value type " + valueType.getName(), descriptor);
				}
				return null;
			}
			if (autowiredBeanNames != null) {
				autowiredBeanNames.addAll(matchingBeans.keySet());
			}
			return matchingBeans;
		}
		else {
			Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
			if (matchingBeans.isEmpty()) {
				if (descriptor.isRequired()) {
					raiseNoSuchBeanDefinitionException(type, "", descriptor);
				}
				return null;
			}
			if (matchingBeans.size() > 1) {
				String primaryBeanName = determinePrimaryCandidate(matchingBeans, descriptor);
				if (primaryBeanName == null) {
					throw new NoSuchBeanDefinitionException(type, "expected single matching bean but found " +
							matchingBeans.size() + ": " + matchingBeans.keySet());
				}
				if (autowiredBeanNames != null) {
					autowiredBeanNames.add(primaryBeanName);
				}
				return matchingBeans.get(primaryBeanName);
			}
			// We have exactly one match.
			Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
			if (autowiredBeanNames != null) {
				autowiredBeanNames.add(entry.getKey());
			}
			return entry.getValue();
		}
	}

	/**
	 * Find bean instances that match the required type.
	 * Called during autowiring for the specified bean.
	 * @param beanName the name of the bean that is about to be wired
	 * @param requiredType the actual type of bean to look for
	 * (may be an array component type or collection element type)
	 * @param descriptor the descriptor of the dependency to resolve
	 * @return a Map of candidate names and candidate instances that match
	 * the required type (never <code>null</code>)
	 * @throws BeansException in case of errors
	 * @see #autowireByType
	 * @see #autowireConstructor
	 */
	protected Map<String, Object> findAutowireCandidates(
			String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

		String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				this, requiredType, true, descriptor.isEager());
		Map<String, Object> result = new LinkedHashMap<String, Object>(candidateNames.length);
		for (Class<?> autowiringType : this.resolvableDependencies.keySet()) {
			if (autowiringType.isAssignableFrom(requiredType)) {
				Object autowiringValue = this.resolvableDependencies.get(autowiringType);
				autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
				if (requiredType.isInstance(autowiringValue)) {
					result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
					break;
				}
			}
		}
		for (String candidateName : candidateNames) {
			if (!candidateName.equals(beanName) && isAutowireCandidate(candidateName, descriptor)) {
				result.put(candidateName, getBean(candidateName));
			}
		}
		return result;
	}

	/**
	 * Determine the primary autowire candidate in the given set of beans.
	 * @param candidateBeans a Map of candidate names and candidate instances
	 * that match the required type, as returned by {@link #findAutowireCandidates}
	 * @param descriptor the target dependency to match against
	 * @return the name of the primary candidate, or <code>null</code> if none found
	 */
	protected String determinePrimaryCandidate(Map<String, Object> candidateBeans, DependencyDescriptor descriptor) {
		String primaryBeanName = null;
		String fallbackBeanName = null;
		for (Map.Entry<String, Object> entry : candidateBeans.entrySet()) {
			String candidateBeanName = entry.getKey();
			Object beanInstance = entry.getValue();
			if (isPrimary(candidateBeanName, beanInstance)) {
				if (primaryBeanName != null) {
					boolean candidateLocal = containsBeanDefinition(candidateBeanName);
					boolean primaryLocal = containsBeanDefinition(primaryBeanName);
					if (candidateLocal == primaryLocal) {
						throw new NoSuchBeanDefinitionException(descriptor.getDependencyType(),
								"more than one 'primary' bean found among candidates: " + candidateBeans.keySet());
					}
					else if (candidateLocal && !primaryLocal) {
						primaryBeanName = candidateBeanName;
					}
				}
				else {
					primaryBeanName = candidateBeanName;
				}
			}
			if (primaryBeanName == null &&
					(this.resolvableDependencies.values().contains(beanInstance) ||
							matchesBeanName(candidateBeanName, descriptor.getDependencyName()))) {
				fallbackBeanName = candidateBeanName;
			}
		}
		return (primaryBeanName != null ? primaryBeanName : fallbackBeanName);
	}

	/**
	 * Return whether the bean definition for the given bean name has been
	 * marked as a primary bean.
	 * @param beanName the name of the bean
	 * @param beanInstance the corresponding bean instance
	 * @return whether the given bean qualifies as primary
	 */
	protected boolean isPrimary(String beanName, Object beanInstance) {
		if (containsBeanDefinition(beanName)) {
			return getMergedLocalBeanDefinition(beanName).isPrimary();
		}
		BeanFactory parentFactory = getParentBeanFactory();
		return (parentFactory instanceof DefaultListableBeanFactory &&
				((DefaultListableBeanFactory) parentFactory).isPrimary(beanName, beanInstance));
	}

	/**
	 * Determine whether the given candidate name matches the bean name or the aliases
	 * stored in this bean definition.
	 */
	protected boolean matchesBeanName(String beanName, String candidateName) {
		return (candidateName != null &&
				(candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
	}

	/**
	 * Raise a NoSuchBeanDefinitionException for an unresolvable dependency.
	 */
	private void raiseNoSuchBeanDefinitionException(
			Class<?> type, String dependencyDescription, DependencyDescriptor descriptor)
			throws NoSuchBeanDefinitionException {

		throw new NoSuchBeanDefinitionException(type, dependencyDescription,
				"expected at least 1 bean which qualifies as autowire candidate for this dependency. " +
				"Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
		sb.append(": defining beans [");
		sb.append(StringUtils.arrayToCommaDelimitedString(getBeanDefinitionNames()));
		sb.append("]; ");
		BeanFactory parent = getParentBeanFactory();
		if (parent == null) {
			sb.append("root of factory hierarchy");
		}
		else {
			sb.append("parent: ").append(ObjectUtils.identityToString(parent));
		}
		return sb.toString();
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " +
				"just a SerializedBeanFactoryReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.serializationId != null) {
			return new SerializedBeanFactoryReference(this.serializationId);
		}
		else {
			throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
		}
	}


	/**
	 * Minimal id reference to the factory.
	 * Resolved to the actual factory instance on deserialization.
	 */
	@SuppressWarnings("serial")
	private static class SerializedBeanFactoryReference implements Serializable {

		private final String id;

		public SerializedBeanFactoryReference(String id) {
			this.id = id;
		}

		private Object readResolve() {
			Reference<?> ref = serializableFactories.get(this.id);
			if (ref == null) {
				throw new IllegalStateException(
						"Cannot deserialize BeanFactory with id " + this.id + ": no factory registered for this id");
			}
			Object result = ref.get();
			if (result == null) {
				throw new IllegalStateException(
						"Cannot deserialize BeanFactory with id " + this.id + ": factory has been garbage-collected");
			}
			return result;
		}
	}


	/**
	 * Serializable ObjectFactory for lazy resolution of a dependency.
	 */
	@SuppressWarnings("serial")
	private class DependencyObjectFactory implements ObjectFactory<Object>, Serializable {

		private final DependencyDescriptor descriptor;

		private final String beanName;

		public DependencyObjectFactory(DependencyDescriptor descriptor, String beanName) {
			this.descriptor = new DependencyDescriptor(descriptor);
			this.descriptor.increaseNestingLevel();
			this.beanName = beanName;
		}

		public Object getObject() throws BeansException {
			return doResolveDependency(this.descriptor, this.descriptor.getDependencyType(), this.beanName, null, null);
		}
	}


	/**
	 * Serializable ObjectFactory for lazy resolution of a dependency.
	 */
	@SuppressWarnings("serial")
	private class DependencyProvider extends DependencyObjectFactory implements Provider<Object> {

		public DependencyProvider(DependencyDescriptor descriptor, String beanName) {
			super(descriptor, beanName);
		}

		public Object get() throws BeansException {
			return getObject();
		}
	}


	/**
	 * Separate inner class for avoiding a hard dependency on the <code>javax.inject</code> API.
	 */
	private class DependencyProviderFactory {

		public Object createDependencyProvider(DependencyDescriptor descriptor, String beanName) {
			return new DependencyProvider(descriptor, beanName);
		}
	}

}
