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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}

    // 大体逻辑是，为了 BeanDefinitionRegistryPostProcessor 和 BeanFactoryPostProcessor 能执行对应的钩子方法
    // BeanDefinitionRegistryPostProcessor 本身是 BeanFactoryPostProcessor 的子类，相当于说，多一个钩子
    //      BeanFactoryPostProcessor 的目的，是为了改变 BeanDefinition
    //      BeanDefinitionRegistryPostProcessor 的目的，是为了增减 BeanDefinition
    // 从逻辑执行上，先执行 BeanDefinitionRegistryPostProcessor 的钩子方法，再执行 BeanFactoryPostProcessor 的钩子方法
    //      因为 BeanDefinitionRegistryPostProcessor 会产生 BeanDefinition 的增减，所以先执行。
    // 又因为，相同的 BeanFactoryPostProcessor 和 BeanDefinitionRegistryPostProcessor ，有顺序的说法，所以需要排序
	public static void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
        // 已经执行的 BeanDefinitionRegistryPostProcessor 的集合
		Set<String> processedBeans = new HashSet<>();

		// BeanDefinitionRegistry 类型
		if (beanFactory instanceof BeanDefinitionRegistry) {
		    // 转化为 BeanDefinitionRegistry 对象
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			// BeanDefinitionRegistryPostProcessor 数组
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>(); // regular 翻译为常规。
			// 非 BeanDefinitionRegistryPostProcessor 数组
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;
					// TODO 对于 BeanDefinitionRegistryPostProcessor 类型，在 BeanFactoryPostProcessor 的基础上还有自定义的方法
                    // 需要先调用
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
                    // 添加到 registryProcessors 中
                    registryProcessors.add(registryProcessor);
				} else {
                    // 添加到 regularPostProcessors 中
                    regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
            // 第一步，获得配置的，且实现 PriorityOrdered 接口的 BeanDefinitionRegistryPostProcessor 数组
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) { // 匹配 PriorityOrdered 接口
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			// 先排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 添加到 registryProcessors 中
			registryProcessors.addAll(currentRegistryProcessors);
			// 后执行
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			// 清空 currentRegistryProcessors
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
            // 第二步，获得配置的，且实现 Ordered 接口的 BeanDefinitionRegistryPostProcessor 数组
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName); // 添加到 processedBeans 中
				}
			}
            // 先排序
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            // 添加到 registryProcessors 中
            registryProcessors.addAll(currentRegistryProcessors);
            // 后执行
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            // 清空 currentRegistryProcessors
            currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
            // 最后，不断获取 BeanDefinitionRegistryPostProcessor 数组，直到没有新的
			boolean reiterate = true; // 是否获取到新的
			while (reiterate) {
				reiterate = false;
                // 获得配置的，BeanDefinitionRegistryPostProcessor 数组
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) { // 不包含，说明发现新的
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName); // 添加 processedBeans 中
						reiterate = true;
					}
				}
                // 先排序
                sortPostProcessors(currentRegistryProcessors, beanFactory);
                // 添加到 registryProcessors 中
                registryProcessors.addAll(currentRegistryProcessors);
                // 执行 currentRegistryProcessors
                invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
                // 清空 currentRegistryProcessors
                currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
            // 执行，registryProcessors 数组
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// 执行，regularPostProcessors 数组
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		} else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
        // PriorityOrdered 保证顺序
        List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
        // 使用 Ordered 保证顺序
        List<String> orderedPostProcessorNames = new ArrayList<>();
        // 没有顺序
        List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
		    // 跳过，已经处理的
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
            // PriorityOrdered
            else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
            // 有序 Ordered
            else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			} else {
                // 无序
                nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
        // 第一步，注册所有实现了 PriorityOrdered 的 BeanFactoryPostProcessors
        // 先排序
        sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 后注册
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
        // 第二步，注册所有实现了 Ordered 的 BeanFactoryPostProcessors
        List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
        // 先排序
        sortPostProcessors(orderedPostProcessors, beanFactory);
		// 后注册
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
        // 第三步注册所有无序的 BeanFactoryPostProcessors
        List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
        // 注册，无需排序
        invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
        // TODO 芋艿
		beanFactory.clearMetadataCache();
	}

	// 注册 BeanPostProcessor
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

        // 获取所有的 BeanPostProcessor 的 beanName
        // 这些 beanName 都已经全部加载到容器中去，但是没有实例化
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
        // 记录所有的beanProcessor数量
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		// 注册 BeanPostProcessorChecker，它主要是用于在 BeanPostProcessor 实例化期间记录日志
        // 当 Spring 中高配置的后置处理器还没有注册就已经开始了 bean 的实例化过程，这个时候便会打印 BeanPostProcessorChecker 中的内容
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
        // PriorityOrdered 保证顺序
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
        // MergedBeanDefinitionPostProcessor
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
        // 使用 Ordered 保证顺序
		List<String> orderedPostProcessorNames = new ArrayList<>();
        // 没有顺序
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
            // PriorityOrdered
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                // 调用 getBean 获取 bean 实例对象
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                // 有序 Ordered
				orderedPostProcessorNames.add(ppName);
			} else {
                // 无序
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
        // 第一步，注册所有实现了 PriorityOrdered 的 BeanPostProcessor
        // 先排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
        // 后注册
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
        // 第二步，注册所有实现了 Ordered 的 BeanPostProcessor
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
        // 先排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
        // 后注册
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
        // 第三步注册所有无序的 BeanPostProcessor
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 注册，无需排序
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
        // 最后，注册所有的 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor
        // 先排序
		sortPostProcessors(internalPostProcessors, beanFactory);
		// 后注册
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
        // 加入ApplicationListenerDetector（探测器）
        // 重新注册 BeanPostProcessor 以检测内部 bean，因为 ApplicationListeners 将其移动到处理器链的末尾
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// 获得 Comparator 对象
	    Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) { // 依赖的 Comparator 对象
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) { // 默认 Comparator 对象
			comparatorToUse = OrderComparator.INSTANCE;
		}
		// 排序
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {
	    // 遍历 BeanPostProcessor 数组，注册
		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}

	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
