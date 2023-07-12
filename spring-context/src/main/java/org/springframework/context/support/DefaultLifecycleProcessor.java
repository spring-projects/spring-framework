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

import java.time.Duration;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;

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
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of the {@link LifecycleProcessor} strategy.
 *
 * <p>Provides interaction with {@link Lifecycle} and {@link SmartLifecycle} beans in
 * groups for specific phases, on startup/shutdown as well as for explicit start/stop
 * interactions on a {@link org.springframework.context.ConfigurableApplicationContext}.
 * As of 6.1, this also includes support for JVM checkpoint/restore (Project CRaC).
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	/**
	 * Property name for a common context checkpoint: {@value}.
	 * @since 6.1
	 * @see #CHECKPOINT_ON_REFRESH_VALUE
	 * @see org.crac.Core#checkpointRestore()
	 */
	public static final String CHECKPOINT_PROPERTY_NAME = "spring.context.checkpoint";

	/**
	 * Recognized value for the context checkpoint property: {@value}.
	 * @since 6.1
	 * @see #CHECKPOINT_PROPERTY_NAME
	 * @see org.crac.Core#checkpointRestore()
	 */
	public static final String CHECKPOINT_ON_REFRESH_VALUE = "onRefresh";


	private final static boolean checkpointOnRefresh =
			CHECKPOINT_ON_REFRESH_VALUE.equalsIgnoreCase(SpringProperties.getProperty(CHECKPOINT_PROPERTY_NAME));

	private final Log logger = LogFactory.getLog(getClass());

	private volatile long timeoutPerShutdownPhase = 30000;

	private volatile boolean running;

	@Nullable
	private volatile ConfigurableListableBeanFactory beanFactory;

	@Nullable
	private volatile Set<String> stoppedBeans;

	// Just for keeping a strong reference to the registered CRaC Resource, if any
	@Nullable
	private Object cracResource;


	public DefaultLifecycleProcessor() {
		if (!NativeDetector.inNativeImage() && ClassUtils.isPresent("org.crac.Core", getClass().getClassLoader())) {
			this.cracResource = new CracDelegate().registerResource();
		}
	}


	/**
	 * Specify the maximum time allotted in milliseconds for the shutdown of any
	 * phase (group of {@link SmartLifecycle} beans with the same 'phase' value).
	 * <p>The default value is 30000 milliseconds (30 seconds).
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
		this.beanFactory = clbf;
	}

	private ConfigurableListableBeanFactory getBeanFactory() {
		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
		Assert.state(beanFactory != null, "No BeanFactory available");
		return beanFactory;
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
		stopBeans();
		this.running = false;
	}

	@Override
	public void onRefresh() {
		if (checkpointOnRefresh) {
			new CracDelegate().checkpointRestore();
		}

		this.stoppedBeans = null;
		startBeans(true);
		this.running = true;
	}

	@Override
	public void onClose() {
		stopBeans();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	// Internal helpers

	void stopForRestart() {
		if (this.running) {
			this.stoppedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>());
			stopBeans();
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
				int phase = getPhase(bean);
				phases.computeIfAbsent(
						phase,
						p -> new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly)
				).add(beanName, bean);
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
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null && bean != this) {
			String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
			for (String dependency : dependenciesForBean) {
				doStart(lifecycleBeans, dependency, autoStartupOnly);
			}
			if (!bean.isRunning() && (!autoStartupOnly || toBeStarted(beanName, bean))) {
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
		}
	}

	private boolean toBeStarted(String beanName, Lifecycle bean) {
		Set<String> stoppedBeans = this.stoppedBeans;
		return (stoppedBeans != null ? stoppedBeans.contains(beanName) :
				(!(bean instanceof SmartLifecycle smartLifecycle) || smartLifecycle.isAutoStartup()));
	}

	private void stopBeans() {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new TreeMap<>(Comparator.reverseOrder());
		lifecycleBeans.forEach((beanName, bean) -> {
			int shutdownPhase = getPhase(bean);
			phases.computeIfAbsent(
					shutdownPhase,
					p -> new LifecycleGroup(shutdownPhase, this.timeoutPerShutdownPhase, lifecycleBeans, false)
			).add(beanName, bean);
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
			final CountDownLatch latch, final Set<String> countDownBeanNames) {

		Lifecycle bean = lifecycleBeans.remove(beanName);
		if (bean != null) {
			String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
			for (String dependentBean : dependentBeans) {
				doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames);
			}
			try {
				if (bean.isRunning()) {
					Set<String> stoppedBeans = this.stoppedBeans;
					if (stoppedBeans != null) {
						stoppedBeans.add(beanName);
					}
					if (bean instanceof SmartLifecycle smartLifecycle) {
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
			}
		}
	}


	// overridable hooks

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

		private final long timeout;

		private final Map<String, ? extends Lifecycle> lifecycleBeans;

		private final boolean autoStartupOnly;

		private final List<LifecycleGroupMember> members = new ArrayList<>();

		private int smartMemberCount;

		public LifecycleGroup(
				int phase, long timeout, Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {

			this.phase = phase;
			this.timeout = timeout;
			this.lifecycleBeans = lifecycleBeans;
			this.autoStartupOnly = autoStartupOnly;
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
			for (LifecycleGroupMember member : this.members) {
				doStart(this.lifecycleBeans, member.name, this.autoStartupOnly);
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
					doStop(this.lifecycleBeans, member.name, latch, countDownBeanNames);
				}
				else if (member.bean instanceof SmartLifecycle) {
					// Already removed: must have been a dependent bean from another phase
					latch.countDown();
				}
			}
			try {
				latch.await(this.timeout, TimeUnit.MILLISECONDS);
				if (latch.getCount() > 0 && !countDownBeanNames.isEmpty() && logger.isInfoEnabled()) {
					logger.info("Failed to shut down " + countDownBeanNames.size() + " bean" +
							(countDownBeanNames.size() > 1 ? "s" : "") + " with phase value " +
							this.phase + " within timeout of " + this.timeout + "ms: " + countDownBeanNames);
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

		@Nullable
		private CyclicBarrier barrier;

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
			long restartTime = System.nanoTime();
			logger.info("Restarting Spring-managed lifecycle beans after JVM restore");
			restartAfterStop();

			// Barrier for prevent-shutdown thread not needed anymore
			this.barrier = null;

			Duration timeTakenToRestart = Duration.ofNanos(System.nanoTime() - restartTime);
			logger.info("Spring-managed lifecycle restart completed in " + timeTakenToRestart.toMillis() + " ms");
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
