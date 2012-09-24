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

package org.springframework.context.support;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapter for live beans view exposure, building a snapshot of current beans
 * and their dependencies from either a local ApplicationContext (with a
 * local LiveBeansView bean definition) or all registered ApplicationContexts
 * (driven by the "spring.liveBeansView.mbean" environment property).
 *
 * <p>Note: This feature is still in beta and primarily designed for use with
 * SpringSource Tool Suite 3.1.
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see #getSnapshotAsJson()
 * @see org.springframework.web.context.support.LiveBeansViewServlet
 */
public class LiveBeansView implements LiveBeansViewMBean, ApplicationContextAware {

	public static final String MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain";

	public static final String MBEAN_APPLICATION_KEY = "application";

	private static final Set<ConfigurableApplicationContext> applicationContexts =
			new LinkedHashSet<ConfigurableApplicationContext>();

	static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
		String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
		if (mbeanDomain != null) {
			synchronized (applicationContexts) {
				if (applicationContexts.isEmpty()) {
					try {
						MBeanServer server = ManagementFactory.getPlatformMBeanServer();
						server.registerMBean(new LiveBeansView(),
								new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationContext.getApplicationName()));
					}
					catch (Exception ex) {
						throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
					}
				}
				applicationContexts.add(applicationContext);
			}
		}
	}

	static void unregisterApplicationContext(ConfigurableApplicationContext applicationContext) {
		synchronized (applicationContexts) {
			if (applicationContexts.remove(applicationContext) && applicationContexts.isEmpty()) {
				try {
					MBeanServer server = ManagementFactory.getPlatformMBeanServer();
					String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
					server.unregisterMBean(new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationContext.getApplicationName()));
				}
				catch (Exception ex) {
					throw new ApplicationContextException("Failed to unregister LiveBeansView MBean", ex);
				}
			}
		}
	}


	private ConfigurableApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext applicationContext) {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}


	/**
	 * Generate a JSON snapshot of current beans and their dependencies,
	 * finding all active ApplicationContexts through {@link #findApplicationContexts()},
	 * then delegating to {@link #generateJson(java.util.Set)}.
	 */
	public String getSnapshotAsJson() {
		Set<ConfigurableApplicationContext> contexts;
		if (this.applicationContext != null) {
			contexts = Collections.singleton(this.applicationContext);
		}
		else {
			contexts = findApplicationContexts();
		}
		return generateJson(contexts);
	}

	/**
	 * Actually generate a JSON snapshot of the beans in the given ApplicationContexts
	 * @param contexts the set of ApplicationContexts
	 * @return the JSON document
	 */
	protected String generateJson(Set<ConfigurableApplicationContext> contexts) {
		StringBuilder result = new StringBuilder();
		for (ConfigurableApplicationContext context : contexts) {
			result.append("{\n\"context\": \"").append(context.getId()).append("\"\n");
			if (context.getParent() != null) {
				result.append("\"parent\": \"").append(context.getParent().getId()).append("\"\n");
			}
			else {
				result.append("\"parent\": null\n");
			}
			ConfigurableListableBeanFactory bf = context.getBeanFactory();
			String[] beanNames = bf.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				BeanDefinition bd = bf.getBeanDefinition(beanName);
				if (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE &&
						(!bd.isLazyInit() || bf.containsSingleton(beanName))) {
					result.append("{\n\"bean\": \"").append(beanName).append("\"\n");
					String scope = bd.getScope();
					if (!StringUtils.hasText(scope)) {
						scope = BeanDefinition.SCOPE_SINGLETON;
					}
					result.append("\"scope\": \"").append(scope).append("\"\n");
					Class beanType = bf.getType(beanName);
					if (beanType != null) {
						result.append("\"type\": \"").append(beanType.getName()).append("\"\n");
					}
					else {
						result.append("\"type\": null\n");
					}
					result.append("\"resource\": \"").append(bd.getResourceDescription()).append("\"\n");
					result.append("\"dependencies\": [");
					String[] dependencies = bf.getDependenciesForBean(beanName);
					if (dependencies.length > 0) {
						result.append("\"");
					}
					result.append(StringUtils.arrayToDelimitedString(dependencies, "\", \""));
					if (dependencies.length > 0) {
						result.append("\"");
					}
					result.append("]\n}\n");
				}
			}
			result.append("}");
		}
		return result.toString();
	}

	/**
	 * Find all applicable ApplicationContexts for the current application.
	 * <p>Called if no specific ApplicationContext has been set for this LiveBeansView.
	 * @return the set of ApplicationContexts
	 */
	protected Set<ConfigurableApplicationContext> findApplicationContexts() {
		synchronized (applicationContexts) {
			return new LinkedHashSet<ConfigurableApplicationContext>(applicationContexts);
		}
	}

}
