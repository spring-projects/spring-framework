/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link LifecycleProcessor} strategy.
 * 
 * @author Mark Fisher
 * @since 3.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile long shutdownGroupTimeout = 30000;

	private volatile boolean running;

	private volatile ConfigurableListableBeanFactory beanFactory;


	/**
	 * Specify the maximum time allotted for the shutdown of any group of
	 * SmartLifecycle beans (those with the same 'order' value). The default
	 * value is 30 seconds.
	 */
	public void setShutdownGroupTimeout(long shutdownGroupTimeout) {
		this.shutdownGroupTimeout = shutdownGroupTimeout;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isTrue(beanFactory instanceof ConfigurableListableBeanFactory,
				"A ConfigurableListableBeanFactory is required.");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/*
	 * Lifecycle implementation
	 */

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		for (String beanName : new LinkedHashSet<String>(lifecycleBeans.keySet())) {
			doStart(lifecycleBeans, beanName);
		}
		this.running = true;
	}

	public void stop() {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, ShutdownGroup> shutdownGroups = new HashMap<Integer, ShutdownGroup>();
		for (Map.Entry<String, Lifecycle> entry : lifecycleBeans.entrySet()) {
			Lifecycle lifecycle = entry.getValue();
			int shutdownOrder = getShutdownOrder(lifecycle);
			ShutdownGroup group = shutdownGroups.get(shutdownOrder);
			if (group == null) {
				group = new ShutdownGroup(shutdownOrder, this.shutdownGroupTimeout, lifecycleBeans);
				shutdownGroups.put(shutdownOrder, group);
			}
			group.add(entry.getKey(), lifecycle);
		}
		if (shutdownGroups.size() > 0) {
			List<Integer> keys = new ArrayList<Integer>(shutdownGroups.keySet());
			Collections.sort(keys);
			for (Integer key : keys) {
				shutdownGroups.get(key).shutdown();
			}
		}
		this.running = false;
	}

	public void onRefresh() {
		Map<String, SmartLifecycle> lifecycleBeans = getSmartLifecycleBeans();
		for (String beanName : new LinkedHashSet<String>(lifecycleBeans.keySet())) {
			SmartLifecycle bean = lifecycleBeans.get(beanName);
			if (bean != null && bean.isAutoStartup()) {
				String[] dependenciesForBean = this.beanFactory.getDependenciesForBean(beanName);
				for (String dependency : dependenciesForBean) {
					doStart(lifecycleBeans, dependency);
				}
				if (!bean.isRunning()) {
					bean.start();
				}
				lifecycleBeans.remove(beanName);
			}
		}
	}

	/**
	 * Start the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that it depends on are started first.
	 * @param lifecycleBeans Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to start
	 */
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName) {
		Lifecycle bean = lifecycleBeans.get(beanName);
		if (bean != null && !this.equals(bean)) {
			String[] dependenciesForBean = this.beanFactory.getDependenciesForBean(beanName);
			for (String dependency : dependenciesForBean) {
				doStart(lifecycleBeans, dependency);
			}
			if (!bean.isRunning()) {
				bean.start();
			}
			lifecycleBeans.remove(beanName);
		}
	}

	/**
	 * Stop the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that depends on it are stopped first.
	 * @param lifecycleBeans Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to stop
	 */
	private void doStop(Map<String, Lifecycle> lifecycleBeans, String beanName, final CountDownLatch latch) {
		Lifecycle bean = lifecycleBeans.get(beanName);
		if (bean != null) {
			String[] dependentBeans = this.beanFactory.getDependentBeans(beanName);
			for (String dependentBean : dependentBeans) {
				doStop(lifecycleBeans, dependentBean, latch);
			}
			if (bean.isRunning()) {
				if (bean instanceof SmartLifecycle) {
					((SmartLifecycle) bean).stop(new Runnable() {
						public void run() {
							latch.countDown();
						}
					});
				}
				else {
					bean.stop();
				}
			}
			lifecycleBeans.remove(beanName);
		}
	}

	private Map<String, Lifecycle> getLifecycleBeans() {
		String[] beanNames = beanFactory.getSingletonNames();
		Map<String, Lifecycle> beans = new LinkedHashMap<String, Lifecycle>();
		for (String beanName : beanNames) {
			Object bean = beanFactory.getSingleton(beanName);
			if (bean instanceof Lifecycle && !this.equals(bean)) {
				beans.put(beanName, (Lifecycle) bean);
			}
		}
		return beans;
	}

	private Map<String, SmartLifecycle> getSmartLifecycleBeans() {
		String[] beanNames = beanFactory.getSingletonNames();
		Map<String, SmartLifecycle> beans = new LinkedHashMap<String, SmartLifecycle>();
		for (String beanName : beanNames) {
			Object bean = beanFactory.getSingleton(beanName);
			if (bean instanceof SmartLifecycle) {
				beans.put(beanName, (SmartLifecycle) bean);
			}
		}
		return beans;
	}


	private static int getShutdownOrder(Lifecycle bean) {
		return (bean instanceof SmartLifecycle) ?
				((SmartLifecycle) bean).getShutdownOrder() : Integer.MAX_VALUE;
	}


	/**
	 * Helper class for maintaining a group of Lifecycle beans that should be shutdown
	 * together based on their 'shutdownOrder' value (or the default MAX_INTEGER value). 
	 */
	private class ShutdownGroup {

		private final List<ShutdownGroupMember> members = new ArrayList<ShutdownGroupMember>();

		private Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();

		private volatile int smartMemberCount;

		private final int order;

		private final long timeout;


		ShutdownGroup(int order, long timeout, Map<String, Lifecycle> lifecycleBeans) {
			this.order = order;
			this.timeout = timeout;
			this.lifecycleBeans = lifecycleBeans;
		}

		void add(String name, Lifecycle bean) {
			if (bean instanceof SmartLifecycle) {
				this.smartMemberCount++;
			}
			this.members.add(new ShutdownGroupMember(name, bean));
		}

		void shutdown() {
			if (members.size() == 0) {
				return;
			}
			Collections.sort(members);
			final CountDownLatch latch = new CountDownLatch(this.smartMemberCount);
			for (ShutdownGroupMember member : members) {
				if (lifecycleBeans.containsKey(member.name)) {
					doStop(lifecycleBeans, member.name, latch);
				}
				else if (member.bean instanceof SmartLifecycle) {
					// already removed, must have been a dependent
					latch.countDown();
				}
			}
			try {
				latch.await(this.timeout, TimeUnit.MILLISECONDS);
				if (latch.getCount() != 0) {
					if (logger.isWarnEnabled()) {
						logger.warn("failed to shutdown beans with order " +
							this.order + " within timeout of " + this.timeout);
					}
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}


	private static class ShutdownGroupMember implements Comparable<ShutdownGroupMember> {

		private String name;

		private Lifecycle bean;

		ShutdownGroupMember(String name, Lifecycle bean) {
			this.name = name;
			this.bean = bean;
		}

		public int compareTo(ShutdownGroupMember other) {
			int thisOrder = getShutdownOrder(this.bean);
			int otherOrder = getShutdownOrder(other.bean);
			return (thisOrder == otherOrder) ? 0 : (thisOrder < otherOrder) ? -1 : 1;
		}
	}

}
