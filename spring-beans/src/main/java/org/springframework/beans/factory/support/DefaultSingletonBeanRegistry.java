/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/** Maximum number of suppressed exceptions to preserve. */
	private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;


	/** Common lock for singleton creation. */
	final Lock singletonLock = new ReentrantLock();

	/** Cache of singleton objects: bean name to bean instance. */
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

	/** Creation-time registry of singleton factories: bean name to ObjectFactory. */
	private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

	/** Custom callbacks for singleton creation/registration. */
	private final Map<String, Consumer<Object>> singletonCallbacks = new ConcurrentHashMap<>(16);

	/** Cache of early singleton objects: bean name to bean instance. */
	private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

	/** Set of registered singletons, containing the bean names in registration order. */
	private final Set<String> registeredSingletons = Collections.synchronizedSet(new LinkedHashSet<>(256));

	/** Names of beans that are currently in creation. */
	private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet(16);

	/** Names of beans currently excluded from in creation checks. */
	private final Set<String> inCreationCheckExclusions = ConcurrentHashMap.newKeySet(16);

	/** Specific lock for lenient creation tracking. */
	private final Lock lenientCreationLock = new ReentrantLock();

	/** Specific lock condition for lenient creation tracking. */
	private final Condition lenientCreationFinished = this.lenientCreationLock.newCondition();

	/** Names of beans that are currently in lenient creation. */
	private final Set<String> singletonsInLenientCreation = new HashSet<>();

	/** Map from one creation thread waiting on a lenient creation thread. */
	private final Map<Thread, Thread> lenientWaitingThreads = new HashMap<>();

	/** Map from bean name to actual creation thread for currently created beans. */
	private final Map<String, Thread> currentCreationThreads = new ConcurrentHashMap<>();

	/** Flag that indicates whether we're currently within destroySingletons. */
	private volatile boolean singletonsCurrentlyInDestruction = false;

	/** Collection of suppressed Exceptions, available for associating related causes. */
	private @Nullable Set<Exception> suppressedExceptions;

	/** Disposable bean instances: bean name to disposable instance. */
	private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();

	/** Map between containing bean names: bean name to Set of bean names that the bean contains. */
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

	/** Map between dependent bean names: bean name to Set of dependent bean names. */
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

	/** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "Bean name must not be null");
		Assert.notNull(singletonObject, "Singleton object must not be null");
		this.singletonLock.lock();
		try {
			addSingleton(beanName, singletonObject);
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * Add the given singleton object to the singleton registry.
	 * <p>To be called for exposure of freshly registered/created singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		Object oldObject = this.singletonObjects.putIfAbsent(beanName, singletonObject);
		if (oldObject != null) {
			throw new IllegalStateException("Could not register object [" + singletonObject +
					"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
		}
		this.singletonFactories.remove(beanName);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.add(beanName);

		Consumer<Object> callback = this.singletonCallbacks.get(beanName);
		if (callback != null) {
			callback.accept(singletonObject);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for early exposure purposes, for example, to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		this.singletonFactories.put(beanName, singletonFactory);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.add(beanName);
	}

	@Override
	public void addSingletonCallback(String beanName, Consumer<Object> singletonConsumer) {
		this.singletonCallbacks.put(beanName, singletonConsumer);
	}

	@Override
	public @Nullable Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	protected @Nullable Object getSingleton(String beanName, boolean allowEarlyReference) {
		// Quick check for existing instance without full singleton lock.
		Object singletonObject = this.singletonObjects.get(beanName);
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			singletonObject = this.earlySingletonObjects.get(beanName);
			if (singletonObject == null && allowEarlyReference) {
				if (!this.singletonLock.tryLock()) {
					// Avoid early singleton inference outside of original creation thread.
					return null;
				}
				try {
					// Consistent creation of early reference within full singleton lock.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						singletonObject = this.earlySingletonObjects.get(beanName);
						if (singletonObject == null) {
							ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
							if (singletonFactory != null) {
								singletonObject = singletonFactory.getObject();
								// Singleton could have been added or removed in the meantime.
								if (this.singletonFactories.remove(beanName) != null) {
									this.earlySingletonObjects.put(beanName, singletonObject);
								}
								else {
									singletonObject = this.singletonObjects.get(beanName);
								}
							}
						}
					}
				}
				finally {
					this.singletonLock.unlock();
				}
			}
		}
		return singletonObject;
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "Bean name must not be null");

		Thread currentThread = Thread.currentThread();
		Boolean lockFlag = isCurrentThreadAllowedToHoldSingletonLock();
		boolean acquireLock = !Boolean.FALSE.equals(lockFlag);
		boolean locked = (acquireLock && this.singletonLock.tryLock());

		try {
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				if (acquireLock && !locked) {
					if (Boolean.TRUE.equals(lockFlag)) {
						// Another thread is busy in a singleton factory callback, potentially blocked.
						// Fallback as of 6.2: process given singleton bean outside of singleton lock.
						// Thread-safe exposure is still guaranteed, there is just a risk of collisions
						// when triggering creation of other beans as dependencies of the current bean.
						this.lenientCreationLock.lock();
						try {
							if (logger.isInfoEnabled()) {
								Set<String> lockedBeans = new HashSet<>(this.singletonsCurrentlyInCreation);
								lockedBeans.removeAll(this.singletonsInLenientCreation);
								logger.info("Obtaining singleton bean '" + beanName + "' in thread \"" +
										currentThread.getName() + "\" while other thread holds singleton " +
										"lock for other beans " + lockedBeans);
							}
							this.singletonsInLenientCreation.add(beanName);
						}
						finally {
							this.lenientCreationLock.unlock();
						}
					}
					else {
						// No specific locking indication (outside a coordinated bootstrap) and
						// singleton lock currently held by some other creation method -> wait.
						this.singletonLock.lock();
						locked = true;
						// Singleton object might have possibly appeared in the meantime.
						singletonObject = this.singletonObjects.get(beanName);
						if (singletonObject != null) {
							return singletonObject;
						}
					}
				}

				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}

				try {
					beforeSingletonCreation(beanName);
				}
				catch (BeanCurrentlyInCreationException ex) {
					this.lenientCreationLock.lock();
					try {
						while ((singletonObject = this.singletonObjects.get(beanName)) == null) {
							Thread otherThread = this.currentCreationThreads.get(beanName);
							if (otherThread != null && (otherThread == currentThread ||
									checkDependentWaitingThreads(otherThread, currentThread))) {
								throw ex;
							}
							if (!this.singletonsInLenientCreation.contains(beanName)) {
								break;
							}
							if (otherThread != null) {
								this.lenientWaitingThreads.put(currentThread, otherThread);
							}
							try {
								this.lenientCreationFinished.await();
							}
							catch (InterruptedException ie) {
								currentThread.interrupt();
							}
							finally {
								if (otherThread != null) {
									this.lenientWaitingThreads.remove(currentThread);
								}
							}
						}
					}
					finally {
						this.lenientCreationLock.unlock();
					}
					if (singletonObject != null) {
						return singletonObject;
					}
					if (locked) {
						throw ex;
					}
					// Try late locking for waiting on specific bean to be finished.
					this.singletonLock.lock();
					locked = true;
					// Lock-created singleton object should have appeared in the meantime.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject != null) {
						return singletonObject;
					}
					beforeSingletonCreation(beanName);
				}

				boolean newSingleton = false;
				boolean recordSuppressedExceptions = (locked && this.suppressedExceptions == null);
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<>();
				}
				try {
					// Leniently created singleton object could have appeared in the meantime.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						this.currentCreationThreads.put(beanName, currentThread);
						try {
							singletonObject = singletonFactory.getObject();
						}
						finally {
							this.currentCreationThreads.remove(beanName);
						}
						newSingleton = true;
					}
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					afterSingletonCreation(beanName);
				}

				if (newSingleton) {
					try {
						addSingleton(beanName, singletonObject);
					}
					catch (IllegalStateException ex) {
						// Leniently accept same instance if implicitly appeared.
						Object object = this.singletonObjects.get(beanName);
						if (singletonObject != object) {
							throw ex;
						}
					}
				}
			}
			return singletonObject;
		}
		finally {
			if (locked) {
				this.singletonLock.unlock();
			}
			this.lenientCreationLock.lock();
			try {
				this.singletonsInLenientCreation.remove(beanName);
				this.lenientWaitingThreads.entrySet().removeIf(
						entry -> entry.getValue() == currentThread);
				this.lenientCreationFinished.signalAll();
			}
			finally {
				this.lenientCreationLock.unlock();
			}
		}
	}

	private boolean checkDependentWaitingThreads(Thread waitingThread, Thread candidateThread) {
		Thread threadToCheck = waitingThread;
		while ((threadToCheck = this.lenientWaitingThreads.get(threadToCheck)) != null) {
			if (threadToCheck == candidateThread) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether the current thread is allowed to hold the singleton lock.
	 * <p>By default, all threads are forced to hold a full lock through {@code null}.
	 * {@link DefaultListableBeanFactory} overrides this to specifically handle its
	 * threads during the pre-instantiation phase: {@code true} for the main thread,
	 * {@code false} for managed background threads, and configuration-dependent
	 * behavior for unmanaged threads.
	 * @return {@code true} if the current thread is explicitly allowed to hold the
	 * lock but also accepts lenient fallback behavior, {@code false} if it is
	 * explicitly not allowed to hold the lock and therefore forced to use lenient
	 * fallback behavior, or {@code null} if there is no specific indication
	 * (traditional behavior: forced to always hold a full lock)
	 * @since 6.2
	 */
	protected @Nullable Boolean isCurrentThreadAllowedToHoldSingletonLock() {
		return null;
	}

	/**
	 * Register an exception that happened to get suppressed during the creation of a
	 * singleton bean instance, for example, a temporary circular reference resolution problem.
	 * <p>The default implementation preserves any given exception in this registry's
	 * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
	 * them as related causes to an eventual top-level {@link BeanCreationException}.
	 * @param ex the Exception to register
	 * @see BeanCreationException#getRelatedCauses()
	 */
	protected void onSuppressedException(Exception ex) {
		if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
			this.suppressedExceptions.add(ex);
		}
	}

	/**
	 * Remove the bean with the given name from the singleton registry, either on
	 * regular destruction or on cleanup after early exposure when creation failed.
	 * @param beanName the name of the bean
	 */
	protected void removeSingleton(String beanName) {
		this.singletonObjects.remove(beanName);
		this.singletonFactories.remove(beanName);
		this.earlySingletonObjects.remove(beanName);
		this.registeredSingletons.remove(beanName);
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		return StringUtils.toStringArray(this.registeredSingletons);
	}

	@Override
	public int getSingletonCount() {
		return this.registeredSingletons.size();
	}


	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(@Nullable String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 */
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * for example, between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		synchronized (this.containedBeanMap) {
			Set<String> containedBeans =
					this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
			if (!containedBeans.add(containedBeanName)) {
				return;
			}
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		String canonicalName = canonicalName(beanName);

		synchronized (this.dependentBeanMap) {
			Set<String> dependentBeans =
					this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
			if (!dependentBeans.add(dependentBeanName)) {
				return;
			}
		}

		synchronized (this.dependenciesForBeanMap) {
			Set<String> dependenciesForBean =
					this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		synchronized (this.dependentBeanMap) {
			return isDependent(beanName, dependentBeanName, null);
		}
	}

	private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		String canonicalName = canonicalName(beanName);
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null || dependentBeans.isEmpty()) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		if (alreadySeen == null) {
			alreadySeen = new HashSet<>();
		}
		alreadySeen.add(beanName);
		for (String transitiveDependency : dependentBeans) {
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		synchronized (this.dependentBeanMap) {
			return StringUtils.toStringArray(dependentBeans);
		}
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		synchronized (this.dependenciesForBeanMap) {
			return StringUtils.toStringArray(dependenciesForBean);
		}
	}

	public void destroySingletons() {
		if (logger.isTraceEnabled()) {
			logger.trace("Destroying singletons in " + this);
		}
		this.singletonsCurrentlyInDestruction = true;

		String[] disposableBeanNames;
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		this.singletonLock.lock();
		try {
			clearSingletonCache();
		}
		finally {
			this.singletonLock.unlock();
		}
	}

	/**
	 * Clear all cached singleton instances in this registry.
	 * @since 4.3.15
	 */
	protected void clearSingletonCache() {
		this.singletonObjects.clear();
		this.singletonFactories.clear();
		this.earlySingletonObjects.clear();
		this.registeredSingletons.clear();
		this.singletonsCurrentlyInDestruction = false;
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	public void destroySingleton(String beanName) {
		// Destroy the corresponding DisposableBean instance.
		// This also triggers the destruction of dependent beans.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			disposableBean = this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);

		// destroySingletons() removes all singleton instances at the end,
		// leniently tolerating late retrieval during the shutdown phase.
		if (!this.singletonsCurrentlyInDestruction) {
			// For an individual destruction, remove the registered instance now.
			// As of 6.2, this happens after the current bean's destruction step,
			// allowing for late bean retrieval by on-demand suppliers etc.
			if (this.currentCreationThreads.get(beanName) == Thread.currentThread()) {
				// Local remove after failed creation step -> without singleton lock
				// since bean creation may have happened leniently without any lock.
				removeSingleton(beanName);
			}
			else {
				this.singletonLock.lock();
				try {
					removeSingleton(beanName);
				}
				finally {
					this.singletonLock.unlock();
				}
			}
		}
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		Set<String> dependentBeanNames;
		synchronized (this.dependentBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			dependentBeanNames = this.dependentBeanMap.remove(beanName);
		}
		if (dependentBeanNames != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependentBeanNames);
			}
			for (String dependentBeanName : dependentBeanNames) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
				}
			}
		}

		// Trigger destruction of contained beans...
		Set<String> containedBeans;
		synchronized (this.containedBeanMap) {
			// Within full synchronization in order to guarantee a disconnected Set
			containedBeans = this.containedBeanMap.remove(beanName);
		}
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	@Deprecated(since = "6.2")
	@Override
	public final Object getSingletonMutex() {
		return new Object();
	}

}
