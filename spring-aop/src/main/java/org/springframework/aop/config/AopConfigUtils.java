/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling registration of AOP auto-proxy creators.
 *
 * <p>Only a single auto-proxy creator should be registered yet multiple concrete
 * implementations are available. This class provides a simple escalation protocol,
 * allowing a caller to request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
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
	 * Stores the auto proxy creator classes in escalation order.
	 */
	private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3);

	static {
		// Set up the escalation list...
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
	}


	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAspectJAutoProxyCreatorIfNecessary(
			BeanDefinitionRegistry registry, @Nullable Object source) {

		return registerOrEscalateApcAsRequired(AspectJAwareAdvisorAutoProxyCreator.class, registry, source);
	}

	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
		return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
	}

	@Nullable
	public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, @Nullable Object source) {
		return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
	}

    /**
     * 在自动判断使用代理方式的情况下，强制使用 CGLIB 代理
     *
     * @param registry 注册表
     */
	public static void forceAutoProxyCreatorToUseClassProxying(BeanDefinitionRegistry registry) {
	    // 如果有 AUTO_PROXY_CREATOR_BEAN_NAME 对应的 BeanDefinition
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			// 设置，开启 CGLIB 代理目前类
            // From 《Spring 源码深度解析》P172
            // Spring AOP 部分使用 JDK 动态代理或者 CGLIB 来为目标对象创建代理。（建议尽量使用 JDK 的动态代理）
            // 如果被代理的目标对象实现了至少一个接口，则会使用 JDK 动态代理。所有该目标类型实现的接口都讲被代理。
            // 若该目标对象没有实现任何接口，则创建一个 CGLIB 代理。
            // 如果你希望强制使用 CGLIB 代理，（例如希望代理目标对象的所有方法，而不只是实现自接口的方法）那也可以。但是需要考虑以下两个方法：
            //      1> 无法通知(advise) Final 方法，因为它们不能被覆盖。
            //      2> 你需要将 CGLIB 二进制发型包放在 classpath 下面。
            // 为什么 Spring 默认使用 JDK 的动态代理呢？笔者猜测原因如下：
            //      1> 使用 JDK 原生支持，减少三方依赖
            //      2> JDK8 开始后，JDK 代理的性能差距 CGLIB 的性能不会太多。可参见：https://www.cnblogs.com/haiq/p/4304615.html
			definition.getPropertyValues().add("proxyTargetClass", Boolean.TRUE);
		}
	}

    /**
     * 参见 https://www.cnblogs.com/chihirotan/p/7356683.html
     *
     * @see org.springframework.aop.framework.AopContext 配合来实现
     *
     * @param registry 注册表
     */
	public static void forceAutoProxyCreatorToExposeProxy(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition definition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			// 设置 "exposeProxy" 属性为 true
			definition.getPropertyValues().add("exposeProxy", Boolean.TRUE);
		}
	}

    /**
     * 注册或者升级 AnnotationAwareAspectJAutoProxyCreator 类
     *
     * 升级的定义是，如果要求的 cls 的优先级高于目前已存在的 AbstractAdvisorAutoProxyCreator 类对应的 BeanDefinition 对象，则需要进行升级 BeanDefinition 成 cls 。
     *
     * cls 对应的优先级，见 {@link #APC_PRIORITY_LIST}
     *
     * @param cls 目标的 AbstractAdvisorAutoProxyCreator 类
     * @param registry 注册表
     * @param source
     * @return 新建（注册）的 BeanDefinition 对象。如果未新建，则返回 null
     */
	@Nullable
	private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

		// 已存在 AUTO_PROXY_CREATOR_BEAN_NAME 对应的 BeanDefinition 对象
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName()); // 当前的优先级
				int requiredPriority = findPriorityForClass(cls); // 要求的优先级
                // 当目前的优先级过低时，使用 cls 进行替换 className
				if (currentPriority < requiredPriority) {
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			// 返回 null ，意味着不用再注册了。
			return null;
		}

		// 创建 RootBeanDefinition 对象
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.setSource(source);
		beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 注册 RootBeanDefinition 到 registry 中
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
		return beanDefinition;
	}

	private static int findPriorityForClass(Class<?> clazz) {
		return APC_PRIORITY_LIST.indexOf(clazz);
	}

	private static int findPriorityForClass(@Nullable String className) {
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			Class<?> clazz = APC_PRIORITY_LIST.get(i);
			if (clazz.getName().equals(className)) {
				return i;
			}
		}
		throw new IllegalArgumentException(
				"Class name [" + className + "] is not a known auto-proxy creator class");
	}

}
