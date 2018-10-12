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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

    /**
     * 带有 @Aspect 注解的 Bean 名字的集合
     */
	@Nullable
	private volatile List<String> aspectBeanNames;

    /**
     * 单例 Bean 对应的 Advisor 增强数组的集合
     *
     * KEY：单例 Bean 的名字
     * VALUE：Advisor 增强的数组
     */
	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

    /**
     * 非单例 Bean 对应的 MetadataAwareAspectInstanceFactory 数组的集合
     *
     * {@link #advisorsCache} 相反
     *
     * KEY：单例 Bean 的名字
     * VALUE：MetadataAwareAspectInstanceFactory 增强的数组
     */
	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	public List<Advisor> buildAspectJAdvisors() {
	    // 第一种情况，aspectBeanNames 为 null ，说明未初始化，则需要进行初始化
		List<String> aspectNames = this.aspectBeanNames;
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();
					// 获取所有 Bean 名字的集合
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					// 遍历所有 Bean 名字，找出对应的增强方法
					for (String beanName : beanNames) {
					    // 不合法的 Bean 则略过，由子类定义规则，默认 BeanFactoryAspectJAdvisorsBuilder 返回 true 。子类的实现，可见 BeanFactoryAspectJAdvisorsBuilderAdapter 。
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
                        // 获得 Bean 对象的类型
						Class<?> beanType = this.beanFactory.getType(beanName);
						if (beanType == null) {
							continue;
						}
						// 如果存在对应的 @Aspect 注解
						if (this.advisorFactory.isAspect(beanType)) {
						    // 添加到 aspectNames 中
							aspectNames.add(beanName);
							// 创建 AspectMetadata 对象
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							// 根据不同类型，不同处理
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) { // 单例
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName); // BeanFactory 开头
								// 解析有 AOP 注解中的增强方法 【重要】
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 记录到 advisorsCache 或 aspectFactoryCache 缓存中
								if (this.beanFactory.isSingleton(beanName)) { // Bean 自身是单例
									this.advisorsCache.put(beanName, classAdvisors);
								} else { // Bean 是 BeanFactory TODO 芋艿，这个情况，未调试过
									this.aspectFactoryCache.put(beanName, factory);
								}
                                // 添加结果到 advisors 中
                                advisors.addAll(classAdvisors);
							} else {
								// Per target or per this.
								if (this.beanFactory.isSingleton(beanName)) { // 要求非单例，即 Prototype 类型 TODO 芋艿，这个情况，未调试过
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName); // Prototype 开头
								// 记录到 aspectFactoryCache 缓存中
								this.aspectFactoryCache.put(beanName, factory);
                                // 解析 @Aspect 注解中的增强方法 【重要】
                                // 添加结果到 advisors 中
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					this.aspectBeanNames = aspectNames;
					return advisors;
				}
			}
		}

		// aspectBeanNames 非 null ，说明已经初始化

        // 第二种情况，aspectNames 数组大小为 0 ，返回空数组
		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}

		// 第三种情况，aspectNames 数组大小大于 1 ，查找对应的 Advisor 数组
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				advisors.addAll(cachedAdvisors);
			} else {
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
