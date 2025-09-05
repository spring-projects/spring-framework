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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;
import org.crac.management.CRaCMXBean;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.Phased;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.NativeDetector;
import org.springframework.core.SpringProperties;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Spring's default implementation of the {@link LifecycleProcessor} strategy.
 *
 * <p>Provides interaction with {@link Lifecycle} and {@link SmartLifecycle} beans in
 * groups for specific phases, on startup/shutdown as well as for explicit start/stop
 * interactions on a {@link org.springframework.context.ConfigurableApplicationContext}.
 *
 * <p>As of 6.1, this also includes support for JVM checkpoint/restore (Project CRaC)
 * when the {@code org.crac:crac} dependency is on the classpath. All running beans
 * will get stopped and restarted according to the CRaC checkpoint/restore callbacks.
 *
 * <p>As of 6.2, this processor can be configured with custom timeouts for specific
 * shutdown phases, applied to {@link SmartLifecycle#stop(Runnable)} implementations.
 * As of 6.2.6, there is also support for the concurrent startup of specific phases
 * with individual timeouts, triggering the {@link SmartLifecycle#start()} callbacks
 * of all associated beans asynchronously and then waiting for all of them to return,
 * as an alternative to the default sequential startup of beans without a timeout.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.0
 * @see SmartLifecycle#getPhase()
 * @see #setConcurrentStartupForPhase
 * @see #setTimeoutForShutdownPhase
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	/**
	 * Property name for a common context checkpoint: {@value}.
	 * @since 6.1
	 * @see #ON_REFRESH_VALUE
	 * @see org.crac.Core#checkpointRestore()
	 */
	public static final String CHECKPOINT_PROPERTY_NAME = "spring.context.checkpoint";

	/**
	 * Property name for terminating the JVM when the context reaches a specific phase: {@value}.
	 * @since 6.1
	 * @see #ON_REFRESH_VALUE
	 */
	public static final String EXIT_PROPERTY_NAME = "spring.context.exit";

	/**
	 * Recognized value for the context checkpoint and exit properties: {@value}.
	 * @since 6.1
	 * @see #CHECKPOINT_PROPERTY_NAME
	 * @see #EXIT_PROPERTY_NAME
	 */
	public static final String ON_REFRESH_VALUE = "onRefresh";


	private static boolean checkpointOnRefresh =
			ON_REFRESH_VALUE.equalsIgnoreCase(SpringProperties.getProperty(CHECKPOINT_PROPERTY_NAME));

	private static final boolean exitOnRefresh =
			ON_REFRESH_VALUE.equalsIgnoreCase(SpringProperties.getProperty(EXIT_PROPERTY_NAME));

	private final Log logger = LogFactory.getLog(getClass());

	private final Map<Integer, Long> concurrentStartupForPhases = new ConcurrentHashMap<>();

	private final Map<Integer, Long> timeoutsForShutdownPhases = new ConcurrentHashMap<>();

	private volatile long timeoutPerShutdownPhase = 10000;

	private volatile boolean running;

	private volatile @Nullable ConfigurableListableBeanFactory beanFactory;

	private volatile @Nullable Set<String> stoppedBeans;

	// Just for keeping a strong reference to the registered CRaC Resource, if any
	private @Nullable Object cracResource;


	public DefaultLifecycleProcessor() {
		if (!NativeDetector.inNativeImage() && ClassUtils.isPresent("org.crac.Core", getClass().getClassLoader())) {
			this.cracResource = new CracDelegate().registerResource();
		}
		else if (checkpointOnRefresh) {
			throw new IllegalStateException(
					"Checkpoint on refresh requires a CRaC-enabled JVM and 'org.crac:crac' on the classpath");
		}
	}


	/**
	 * Switch to concurrent startup for each given phase (group of {@link SmartLifecycle}
	 * beans with the same 'phase' value) with corresponding timeouts.
	 * <p><b>Note: By default, the startup for every phase will be sequential without
	 * a timeout. Calling this setter with timeouts for the given phases switches to a
	 * mode where the beans in these phases will be started concurrently, cancelling
	 * the startup if the corresponding timeout is not met for any of these phases.</b>
	 * <p>For an actual concurrent startup, a bootstrap {@code Executor} needs to be
	 * set for the application context, typically through a "bootstrapExecutor" bean.
	 * @param phasesWithTimeouts a map of phase values (matching
	 * {@link SmartLifecycle#getPhase()}) and corresponding timeout values
	 * (in milliseconds)
	 * @since 6.2.6
	 * @see SmartLifecycle#getPhase()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBootstrapExecutor()
	 */
	public void setConcurrentStartupForPhases(Map<Integer, Long> phasesWithTimeouts) {
		this.concurrentStartupForPhases.putAll(phasesWithTimeouts);
	}

	/**
	 * Switch to concurrent startup for a specific phase (group of {@link SmartLifecycle}
	 * beans with the same 'phase' value) with a corresponding timeout.
	 * <p><b>Note: By default, the startup for every phase will be sequential without
	 * a timeout. Calling this setter with a timeout for the given phase switches to a
	 * mode where the beans in this phase will be started concurrently, cancelling
	 * the startup if the corresponding timeout is not met for this phase.</b>
	 * <p>For an actual concurrent startup, a bootstrap {@code Executor} needs to be
	 * set for the application context, typically through a "bootstrapExecutor" bean.
	 * @param phase the phase value (matching {@link SmartLifecycle#getPhase()})
	 * @param timeout the corresponding timeout value (in milliseconds)
	 * @since 6.2.6
	 * @see SmartLifecycle#getPhase()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBootstrapExecutor()
	 */
	public void setConcurrentStartupForPhase(int phase, long timeout) {
		this.concurrentStartupForPhases.put(phase, timeout);
	}

	/**
	 * Specify the maximum time allotted for the shutdown of each given phase
	 * (group of {@link SmartLifecycle} beans with the same 'phase' value).
	 * <p>In case of no specific timeout configured, the default timeout per
	 * shutdown phase will apply: 10000 milliseconds (10 seconds) as of 6.2.
	 * @param phasesWithTimeouts a map of phase values (matching
	 * {@link SmartLifecycle#getPhase()}) and corresponding timeout values
	 * (in milliseconds)
	 * @since 6.2
	 * @see SmartLifecycle#getPhase()
	 * @see #setTimeoutPerShutdownPhase
	 */
	public void setTimeoutsForShutdownPhases(Map<Integer, Long> phasesWithTimeouts) {
		this.timeoutsForShutdownPhases.putAll(phasesWithTimeouts);
	}

	/**
	 * Specify the maximum time allotted for the shutdown of a specific phase
	 * (group of {@link SmartLifecycle} beans with the same 'phase' value).
	 * <p>In case of no specific timeout configured, the default timeout per
	 * shutdown phase will apply: 10000 milliseconds (10 seconds) as of 6.2.
	 * @param phase the phase value (matching {@link SmartLifecycle#getPhase()})
	 * @param timeout the corresponding timeout value (in milliseconds)
	 * @since 6.2
	 * @see SmartLifecycle#getPhase()
	 * @see #setTimeoutPerShutdownPhase
	 */
	public void setTimeoutForShutdownPhase(int phase, long timeout) {
		this.timeoutsForShutdownPhases.put(phase, timeout);
	}

	/**
	 * Specify the maximum time allotted in milliseconds for the shutdown of any
	 * phase (group of {@link SmartLifecycle} beans with the same 'phase' value).
	 * <p>The default value is 10000 milliseconds (10 seconds) as of 6.2.
	 * @see SmartLifecycle#getPhase()
	 */
	public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
			throw new IllegalArgumentException(
					"DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
		}
		if (!this.concurrentStartupForPhases.isEmpty() && clbf.getBootstrapExecutor() == null) {
			throw new IllegalStateException("'bootstrapExecutor' needs to be configured for concurrent startup");
		}
		this.beanFactory = clbf;
	}

	private ConfigurableListableBeanFactory getBeanFactory() {
		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
		Assert.state(beanFactory != null, "No BeanFactory available");
		return beanFactory;
	}

	private Executor getBootstrapExecutor() {
		Executor executor = getBeanFactory().getBootstrapExecutor();
		Assert.state(executor != null, "No 'bootstrapExecutor' available");
		return executor;
	}

	private @Nullable Long determineConcurrentStartup(int phase) {
		return this.concurrentStartupForPhases.get(phase);
	}

	private long determineShutdownTimeout(int phase) {
		Long timeout = this.timeoutsForShutdownPhases.get(phase);
		return (timeout != null ? timeout : this.timeoutPerShutdownPhase);
	}


	// Lifecycle implementation

	/**
	 * Start all registered beans that implement {@link Lifecycle} and are <i>not</i>
	 * already running. Any bean that implements {@link SmartLifecycle} will be
	 * started within its 'phase', and all phases will be ordered from lowest to
	 * highest value. All beans that do not implement {@link SmartLifecycle} will be
	 * started in the default phase 0. A bean declared as a dependency of another bean
	 * will be started before the dependent bean regardless of the declared phase.
	 */
	@Override
	public void start() {
		this.stoppedBeans = null;
		startBeans(false);
		// If any bean failed to explicitly start, the exception propagates here.
		// The caller may choose to subsequently call stop() if appropriate.
		this.running = true;
	}

	/**
	 * Stop all registered beans that implement {@link Lifecycle} and <i>are</i>
	 * currently running. Any bean that implements {@link SmartLifecycle} will be
	 * stopped within its 'phase', and all phases will be ordered from highest to
	 * lowest value. All beans that do not implement {@link SmartLifecycle} will be
	 * stopped in the default phase 0. A bean declared as dependent on another bean
	 * will be stopped before the dependency bean regardless of the declared phase.
	 */
	@Override
	public void stop() {
		stopBeans(false);
		this.running = false;
	}

	@Override
	public void onRefresh() {
		if (checkpointOnRefresh) {
			checkpointOnRefresh = false;
			new CracDelegate().checkpointRestore();
		}
		if (exitOnRefresh) {
			Runtime.getRuntime().halt(0);
		}

		this.stoppedBeans = null;
		try {
			startBeans(true);
		}
		catch (ApplicationContextException ex) {
			// Some bean failed to auto-start within context refresh:
			// stop already started beans on context refresh failure.
			stopBeans(false);
			throw ex;
		}
		this.running = true;
	}

	@Override
	public void onRestart() {
		this.stoppedBeans = null;
		if (this.running) {
			stopBeans(true);
		}
		startBeans(true);
		this.running = true;
	}

	@Override
	public void onPause() {
		if (this.running) {
			stopBeans(true);
			this.running = false;
		}
	}

	@Override
	public void onClose() {
		stopBeans(false);
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	// Internal helpers

	void stopForRestart() {
		if (this.running) {
			this.stoppedBeans = ConcurrentHashMap.newKeySet();
			stopBeans(false);
			this.running = false;
		}
	}

	void restartAfterStop() {
		if (this.stoppedBeans != null) {
			startBeans(true);
			this.stoppedBeans = null;
			this.running = true;
		}
	}

	private void startBeans(boolean autoStartupOnly) {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new TreeMap<>();

		lifecycleBeans.forEach((beanName, bean) -> {
			if (!autoStartupOnly || isAutoStartupCandidate(beanName, bean)) {
				int startupPhase = getPhase(bean);
				phases.computeIfAbsent(
						startupPhase, phase -> new LifecycleGroup(phase, lifecycleBeans, autoStartupOnly, false))
						.add(beanName, bean);
			}
		});

		if (!phases.isEmpty()) {
			phases.values().forEach(LifecycleGroup::start);
		}
	}

	private boolean isAutoStartupCandidate(String beanName, Lifecycle bean) {
		Set<String> stoppedBeans = this.stoppedBeans;
		return (stoppedBeans != null ? stoppedBeans.contains(beanName) :
				(bean instanceof SmartLifecycle smartLifecycle && smartLifecycle.isAutoStartup()));
	}

	/**
	 * Start the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that it depends on are started first.
	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to start
	 */
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName,
			boolean autoStartupOnly, @Nullable List<CompletableFuture<?>> futures) {

		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null && bean != this) {
			String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
			for (String dependency : dependenciesForBean) {
				doStart(lifecycleBeans, dependency, autoStartupOnly, futures);
			}
			if (!bean.isRunning() && (!autoStartupOnly || toBeStarted(beanName, bean))) {
				if (futures != null) {
					futures.add(CompletableFuture.runAsync(() -> doStart(beanName, bean), getBootstrapExecutor()));
				}
				else {
					doStart(beanName, bean);
				}
			}
		}
	}

	private void doStart(String beanName, Lifecycle bean) {
		if (logger.isTraceEnabled()) {
			logger.trace("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
		}
		try {
			bean.start();
		}
		catch (Throwable ex) {
			throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Successfully started bean '" + beanName + "'");
		}
	}

	private boolean toBeStarted(String beanName, Lifecycle bean) {
		Set<String> stoppedBeans = this.stoppedBeans;
		return (stoppedBeans != null ? stoppedBeans.contains(beanName) :
				(!(bean instanceof SmartLifecycle smartLifecycle) || smartLifecycle.isAutoStartup()));
	}

	private void stopBeans(boolean pauseableOnly) {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new TreeMap<>(Comparator.reverseOrder());

		lifecycleBeans.forEach((beanName, bean) -> {
			int shutdownPhase = getPhase(bean);
			phases.computeIfAbsent(
					shutdownPhase, phase -> new LifecycleGroup(phase, lifecycleBeans, false, pauseableOnly))
					.add(beanName, bean);
		});

		if (!phases.isEmpty()) {
			phases.values().forEach(LifecycleGroup::stop);
		}
	}

	/**
	 * Stop the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that depends on it are stopped first.
	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to stop
	 */
	private void doStop(Map<String, ? extends Lifecycle> lifecycleBeans, final String beanName,
			boolean pauseableOnly, final CountDownLatch latch, final Set<String> countDownBeanNames) {

		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null) {
			String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
			for (String dependentBean : dependentBeans) {
				doStop(lifecycleBeans, dependentBean, pauseableOnly, latch, countDownBeanNames);
			}
			try {
				if (bean.isRunning()) {
					Set<String> stoppedBeans = this.stoppedBeans;
					if (stoppedBeans != null) {
						stoppedBeans.add(beanName);
					}
					if (bean instanceof SmartLifecycle smartLifecycle) {
						if (!pauseableOnly || smartLifecycle.isPauseable()) {
							if (logger.isTraceEnabled()) {
								logger.trace("Asking bean '" + beanName + "' of type [" +
										bean.getClass().getName() + "] to stop");
							}
							countDownBeanNames.add(beanName);
							smartLifecycle.stop(() -> {
								latch.countDown();
								countDownBeanNames.remove(beanName);
								if (logger.isDebugEnabled()) {
									logger.debug("Bean '" + beanName + "' completed its stop procedure");
								}
							});
						}
						else {
							// Don't wait for beans that aren't pauseable...
							latch.countDown();
						}
					}
					else if (!pauseableOnly) {
						if (logger.isTraceEnabled()) {
							logger.trace("Stopping bean '" + beanName + "' of type [" +
									bean.getClass().getName() + "]");
						}
						bean.stop();
						if (logger.isDebugEnabled()) {
							logger.debug("Successfully stopped bean '" + beanName + "'");
						}
					}
				}
				else if (bean instanceof SmartLifecycle) {
					// Don't wait for beans that aren't running...
					latch.countDown();
				}
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to stop bean '" + beanName + "'", ex);
				}
				if (bean instanceof SmartLifecycle) {
					latch.countDown();
				}
			}
		}
	}


	// Overridable hooks

	/**
	 * Retrieve all applicable Lifecycle beans: all singletons that have already been created,
	 * as well as all SmartLifecycle beans (even if they are marked as lazy-init).
	 * @return the Map of applicable beans, with bean names as keys and bean instances as values
	 */
	protected Map<String, Lifecycle> getLifecycleBeans() {
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		Map<String, Lifecycle> beans = new LinkedHashMap<>();
		String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
		for (String beanName : beanNames) {
			String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
			boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
			String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
			if ((beanFactory.containsSingleton(beanNameToRegister) &&
					(!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) ||
					matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
				Object bean = beanFactory.getBean(beanNameToCheck);
				if (bean != this && bean instanceof Lifecycle lifecycle) {
					beans.put(beanNameToRegister, lifecycle);
				}
			}
		}
		return beans;
	}

	private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
		Class<?> beanType = beanFactory.getType(beanName);
		return (beanType != null && targetType.isAssignableFrom(beanType));
	}

	/**
	 * Determine the lifecycle phase of the given bean.
	 * <p>The default implementation checks for the {@link Phased} interface, using
	 * a default of 0 otherwise. Can be overridden to apply other/further policies.
	 * @param bean the bean to introspect
	 * @return the phase (an integer value)
	 * @see Phased#getPhase()
	 * @see SmartLifecycle
	 */
	protected int getPhase(Lifecycle bean) {
		return (bean instanceof Phased phased ? phased.getPhase() : 0);
	}


	/**
	 * Helper class for maintaining a group of Lifecycle beans that should be started
	 * and stopped together based on their 'phase' value (or the default value of 0).
	 * The group is expected to be created in an ad-hoc fashion and group members are
	 * expected to always have the same 'phase' value.
	 */
	private class LifecycleGroup {

		private final int phase;

		private final Map<String, ? extends Lifecycle> lifecycleBeans;

		private final boolean autoStartupOnly;

		private final boolean pauseableOnly;

		private final List<LifecycleGroupMember> members = new ArrayList<>();

		private int smartMemberCount;

		public LifecycleGroup(int phase, Map<String, ? extends Lifecycle> lifecycleBeans,
				boolean autoStartupOnly, boolean pauseableOnly) {

			this.phase = phase;
			this.lifecycleBeans = lifecycleBeans;
			this.autoStartupOnly = autoStartupOnly;
			this.pauseableOnly = pauseableOnly;
		}

		public void add(String name, Lifecycle bean) {
			this.members.add(new LifecycleGroupMember(name, bean));
			if (bean instanceof SmartLifecycle) {
				this.smartMemberCount++;
			}
		}

		public void start() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Starting beans in phase " + this.phase);
			}
			Long concurrentStartup = determineConcurrentStartup(this.phase);
			List<CompletableFuture<?>> futures = (concurrentStartup != null ? new ArrayList<>() : null);
			for (LifecycleGroupMember member : this.members) {
				doStart(this.lifecycleBeans, member.name, this.autoStartupOnly, futures);
			}
			if (concurrentStartup != null && !CollectionUtils.isEmpty(futures)) {
				try {
					CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
							.get(concurrentStartup, TimeUnit.MILLISECONDS);
				}
				catch (Exception ex) {
					if (ex instanceof ExecutionException exEx) {
						Throwable cause = exEx.getCause();
						if (cause instanceof ApplicationContextException acEx) {
							throw acEx;
						}
					}
					throw new ApplicationContextException("Failed to start beans in phase " + this.phase +
							" within timeout of " + concurrentStartup + "ms", ex);
				}
			}
		}

		public void stop() {
			if (this.members.isEmpty()) {
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping beans in phase " + this.phase);
			}
			CountDownLatch latch = new CountDownLatch(this.smartMemberCount);
			Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());
			Set<String> lifecycleBeanNames = new HashSet<>(this.lifecycleBeans.keySet());
			for (LifecycleGroupMember member : this.members) {
				if (lifecycleBeanNames.contains(member.name)) {
					doStop(this.lifecycleBeans, member.name, this.pauseableOnly, latch, countDownBeanNames);
				}
				else if (member.bean instanceof SmartLifecycle) {
					// Already removed: must have been a dependent bean from another phase
					latch.countDown();
				}
			}
			try {
				long shutdownTimeout = determineShutdownTimeout(this.phase);
				if (!latch.await(shutdownTimeout, TimeUnit.MILLISECONDS)) {
					// Count is still >0 after timeout
					if (!countDownBeanNames.isEmpty() && logger.isInfoEnabled()) {
						logger.info("Shutdown phase " + this.phase + " ends with " + countDownBeanNames.size() +
								" bean" + (countDownBeanNames.size() > 1 ? "s" : "") +
								" still running after timeout of " + shutdownTimeout + "ms: " + countDownBeanNames);
					}
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}


	/**
	 * A simple record of a LifecycleGroup member.
	 */
	private record LifecycleGroupMember(String name, Lifecycle bean) {}


	/**
	 * Inner class to avoid a hard dependency on Project CRaC at runtime.
	 * @since 6.1
	 * @see org.crac.Core
	 */
	private class CracDelegate {

		public Object registerResource() {
			logger.debug("Registering JVM checkpoint/restore callback for Spring-managed lifecycle beans");
			CracResourceAdapter resourceAdapter = new CracResourceAdapter();
			org.crac.Core.getGlobalContext().register(resourceAdapter);
			return resourceAdapter;
		}

		public void checkpointRestore() {
			logger.info("Triggering JVM checkpoint/restore");
			try {
				Core.checkpointRestore();
			}
			catch (UnsupportedOperationException ex) {
				throw new ApplicationContextException("CRaC checkpoint not supported on current JVM", ex);
			}
			catch (CheckpointException ex) {
				throw new ApplicationContextException("Failed to take CRaC checkpoint on refresh", ex);
			}
			catch (RestoreException ex) {
				throw new ApplicationContextException("Failed to restore CRaC checkpoint on refresh", ex);
			}
		}
	}


	/**
	 * Resource adapter for Project CRaC, triggering a stop-and-restart cycle
	 * for Spring-managed lifecycle beans around a JVM checkpoint/restore.
	 * @since 6.1
	 * @see #stopForRestart()
	 * @see #restartAfterStop()
	 */
	private class CracResourceAdapter implements org.crac.Resource {

		private @Nullable CyclicBarrier barrier;

		@Override
		public void beforeCheckpoint(org.crac.Context<? extends org.crac.Resource> context) {
			// A non-daemon thread for preventing an accidental JVM shutdown before the checkpoint
			this.barrier = new CyclicBarrier(2);

			Thread thread = new Thread(() -> {
				awaitPreventShutdownBarrier();
				// Checkpoint happens here
				awaitPreventShutdownBarrier();
			}, "prevent-shutdown");

			thread.setDaemon(false);
			thread.start();
			awaitPreventShutdownBarrier();

			logger.debug("Stopping Spring-managed lifecycle beans before JVM checkpoint");
			stopForRestart();
		}

		@Override
		public void afterRestore(org.crac.Context<? extends org.crac.Resource> context) {
			logger.info("Restarting Spring-managed lifecycle beans after JVM restore");
			restartAfterStop();

			// Barrier for prevent-shutdown thread not needed anymore
			this.barrier = null;

			if (!checkpointOnRefresh) {
				logger.info("Spring-managed lifecycle restart completed (restored JVM running for " +
						CRaCMXBean.getCRaCMXBean().getUptimeSinceRestore() + " ms)");
			}
		}

		private void awaitPreventShutdownBarrier() {
			try {
				if (this.barrier != null) {
					this.barrier.await();
				}
			}
			catch (Exception ex) {
				logger.trace("Exception from prevent-shutdown barrier", ex);
			}
		}
	}

}
