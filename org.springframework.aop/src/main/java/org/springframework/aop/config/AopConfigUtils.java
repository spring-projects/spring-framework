/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.JdkVersion;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator can be registered yet multiple concrete 
 * implementations are available. Therefore this class wraps a simple escalation 
 * protocol, allowing classes to request a particular auto-proxy creator and know
 * that class, <code>or a subclass thereof</code>, will eventually be resident
 * in the application context.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see AopNamespaceUtils
 */
public abstract class AopConfigUtils {

	/**
	 * The bean name of the internally managed auto-proxy creator.
	 */
	public static final String AUTO_PROXY_CREATOR_BEAN_NAME =
			"org.springframework.aop.config.internalAutoProxyCreator";

	/**
	 * The class name of the <code>AnnotationAwareAspectJAutoProxyCreator</code> class.
	 * Only available with AspectJ and Java 5.
	 */
	private static final String ASPECTJ_ANNOTATION_AUTO_PROXY_CREATOR_CLASS_NAME =
			"org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator";


	/**
	 * Stores the auto proxy creator classes in escalation order.
	 */
	private static final List APC_PRIORITY_LIST = new ArrayList();

	/**
	 * Setup the escalation list.
	 */
	static {
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class.getName());
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class.getName());
		APC_PRIORITY_LIST.add(ASPECTJ_ANNOTATION_AUTO_PROXY_CREATOR_CLASS_NAME);
	}


	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object source) {
		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}

	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
	}

	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object source) {
		return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
	}

	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}

	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, Object source) {
		Class cls = getAspectJAnnotationAutoProxyCreatorClassIfPossible();
		return registerOrEscalateApcAsRequired(cls, registry, source);
	}

	public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			definition.getPropertyValues().addPropertyValue("proxyTargetClass", Boolean.TRUE);
		}
	}


	private static BeanDefinition registerOrEscalateApcAsRequired(Class cls, BeanDefinitionRegistry registry, Object source) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls.getName());
				if (currentPriority < requiredPriority) {
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			return null;
		}
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().addPropertyValue("order", new Integer(Ordered.HIGHEST_PRECEDENCE));
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}

	private static Class getAspectJAnnotationAutoProxyCreatorClassIfPossible() {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			throw new IllegalStateException(
					"AnnotationAwareAspectJAutoProxyCreator is only available on Java 1.5 and higher");
		}
		try {
			return ClassUtils.forName(
					ASPECTJ_ANNOTATION_AUTO_PROXY_CREATOR_CLASS_NAME, AopConfigUtils.class.getClassLoader());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Unable to load Java 1.5 dependent class [" +
					ASPECTJ_ANNOTATION_AUTO_PROXY_CREATOR_CLASS_NAME + "]", ex);
		}
	}

	private static int findPriorityForClass(String className) {
		Assert.notNull(className, "Class name must not be null");
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			String str = (String) APC_PRIORITY_LIST.get(i);
			if (className.equals(str)) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"Class name [" + className + "] is not a known auto-proxy creator class");
	}

}
