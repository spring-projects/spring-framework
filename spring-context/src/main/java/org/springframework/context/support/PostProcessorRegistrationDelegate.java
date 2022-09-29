/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		Set<String> processedBeans = new HashSet<>();

		// 对于 BeanDefinitionRegistry 类型的处理，这里是交由 BeanDefinitionRegistryPostProcessor 来处理的
		// 判断 BeanFactory 的类型, 如果是 BeanDefinitionRegistry 的子类,
		// 则交由 BeanDefinitionRegistryPostProcessor 处理, 否则直接按照 BeanFactoryPostProcessor 进行处理
		// 由于 BeanDefinitionRegistryPostProcessor 只能处理 BeanDefinitionRegistry 的子类, 所以这里必须先进行 beanFactory 的区分
		if (beanFactory instanceof BeanDefinitionRegistry) {
			// 以下逻辑看似复杂其实大体就是两步
			// 1.获取所有硬编码的 BeanDefinitionRegistryPostProcessor 类型, 激活 postProcessBeanDefinitionRegistry 方法
			// 2.获取所有配置的 BeanDefinitionRegistryPostProcessor 类型, 激活 postProcessBeanDefinitionRegistry 方法

			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			// 记录通过硬编码方式注册的 BeanFactoryPostProcessor 类型处理器
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			// 记录通过硬编码方式注册的 BeanDefinitionRegistryPostProcessor  类型处理器
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 遍历所有硬编码注册的 BeanFactoryPostProcessor 处理器
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				// 如果是 BeanDefinitionRegistryPostProcessor 类型的处理器
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					// 将 BeanFactoryPostProcessor 强制类型转换为 BeanDefinitionRegistryPostProcessor
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					// 执行 BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry 方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					// 将其保存到 registryProcessors 中，以供后续执行其他 `postProcessBeanFactory(registry)` 方法
					registryProcessors.add(registryProcessor);
				}
				else {
					// 将非 BeanDefinitionRegistryPostProcessor 类型的硬编码处理器注入对象直接保存到 regularPostProcessors 中
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			// 临时变量，用于临时保存 BeanFactory 容器中的 BeanDefinitionRegistryPostProcessor 对象
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			// 获取所有通过配置方式注册的 BeanDefinitionRegistryPostProcessor 的 BeanName
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历所有通过配置方式注册的 BeanDefinitionRegistryPostProcessor 的 BeanName
			for (String ppName : postProcessorNames) {
				// 筛选出 PriorityOrdered 接口的实现类, 用于优先执行
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 从容器中获取 BeanName 对应的 BeanDefinitionRegistryPostProcessor 类型的 Bean 实例, 并将其添加到 currentRegistryProcessors 中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 记录 BeanName 对应的 BeanDefinitionRegistryPostProcessor 已经处理
					processedBeans.add(ppName);
				}
			}
			// 对实现 PriorityOrdered 接口的 BeanDefinitionRegistryPostProcessor 类型的 Bean 进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将其添加到 registryProcessors 中，临时保存起来
			registryProcessors.addAll(currentRegistryProcessors);
			// 执行 postProcessBeanDefinitionRegistry 方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 执行完后将集合清空
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			// 获取所有通过配置方式注册的 BeanDefinitionRegistryPostProcessor 的 BeanName
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			// 遍历所有通过配置方式注册的 BeanDefinitionRegistryPostProcessor 的 BeanName
			for (String ppName : postProcessorNames) {
				// 筛选出未经过处理的 && 实现 Ordered 接口的实现类, 用于第二执行
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					// 从容器中获取 BeanName 对应的 BeanDefinitionRegistryPostProcessor 类型的 Bean 实例, 并将其添加到 currentRegistryProcessors 中
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					// 记录 BeanName 对应的 BeanDefinitionRegistryPostProcessor 已经处理
					processedBeans.add(ppName);
				}
			}
			// 对实现 Ordered 接口的 BeanDefinitionRegistryPostProcessor 类型的 Bean 进行排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			// 将其添加到 registryProcessors 中，临时保存起来
			registryProcessors.addAll(currentRegistryProcessors);
			// 执行 postProcessBeanDefinitionRegistry 方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
			// 执行完后将集合清空
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			// 最后获取没有实现排序接口的 BeanDefinitionRegistryPostProcessor 类型的 Bean 进行激活,
			// 直到所有的 BeanDefinitionRegistryPostProcessors 类型注册的子类全部处理完毕才会退出 while 循环
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				// 获取所有通过配置方式注册的 BeanDefinitionRegistryPostProcessor 的 BeanName
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				// 遍历所有通过配置方式注册的 BeanDefinitionRegistryPostProcessor 的 BeanName
				for (String ppName : postProcessorNames) {
					// 将剩余(未实现 PriorityOrdered 和 Ordered 排序接口的子类)未处理过的 BeanName
					if (!processedBeans.contains(ppName)) {
						// 从容器中获取 BeanName 对应的 BeanDefinitionRegistryPostProcessor 类型的 Bean 实例, 并将其添加到 currentRegistryProcessors 中
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						// 记录 BeanName 对应的 BeanDefinitionRegistryPostProcessor 已经处理
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				// 排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				// 将其添加到 registryProcessors 中，临时保存起来
				registryProcessors.addAll(currentRegistryProcessors);
				// 执行 postProcessBeanDefinitionRegistry 方法
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry, beanFactory.getApplicationStartup());
				// 执行完后将集合清空
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			// 至此, 所有的 BeanDefinitionRegistryPostProcessor 类型对象的 postProcessBeanDefinitionRegistry 方法已经全部激活完毕,
			// 开始执行其 postProcessBeanFactory 方法。
			// registryProcessors 记录的是所有通过硬编码注册的 BeanDefinitionRegistryPostProcess
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			// regularPostProcessors 记录的是所有通过硬编码注册的 BeanFactoryPostProcessor
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		// 如果 beanFactory instanceOf BeanDefinitionRegistry 为 false
		// 则直接执行 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法即可。
		// 因为 BeanDefinitionRegistryPostProcessor 接口中定义的 postProcessBeanDefinitionRegistry 方法需要的参数类型为 BeanDefinitionRegistry 类型的 BeanFactory
		else {
			// Invoke factory processors registered with the context instance.
			// 直接执行使用硬编码注册的 BeanFactoryPostProcessor 的 postProcessBeanFactory 方法
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}
		// 至此, 所有通过硬编码注册的 BeanFactoryPostProcessor 已全部处理完成, 下面开始处理通过配置注册的后置处理器

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		// 获取所有的 BeanFactoryPostProcessor 类型的后置处理器的 BeanName, 用于后续处理
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 创建几个保存不同排序的集合, 按照实现的排序接口进行依次调用
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		// 遍历获取的所有 BeanFactoryPostProcessor 类型的后置处理器的 BeanName
		for (String ppName : postProcessorNames) {
			// 如果在上面已经处理过, 则直接跳过
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		// 排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 执行
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		// 执行实现了 Ordered 排序接口的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		// 排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 执行
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		// 执行没有实现任何排序接口的 BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	/**
	 * registerBeanPostProcessors() 方法作用:
	 * 按照排序接口依次注册 BeanPostProcessor 到IOC容器中。
	 * 与 invokeBeanFactoryPostProcessors() 方法不同的是，这里只是将 BeanPostProcessor 进行实例化并注册到容器中，并没有对其进行激活
	 * @param beanFactory
	 * @param applicationContext
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		// WARNING: Although it may appear that the body of this method can be easily
		// refactored to avoid the use of multiple loops and multiple lists, the use
		// of multiple lists and multiple passes over the names of processors is
		// intentional. We must ensure that we honor the contracts for PriorityOrdered
		// and Ordered processors. Specifically, we must NOT cause processors to be
		// instantiated (via getBean() invocations) or registered in the ApplicationContext
		// in the wrong order.
		//
		// Before submitting a pull request (PR) to change this method, please review the
		// list of all declined PRs involving changes to PostProcessorRegistrationDelegate
		// to ensure that your proposal does not result in a breaking change:
		// https://github.com/spring-projects/spring-framework/issues?q=PostProcessorRegistrationDelegate+is%3Aclosed+label%3A%22status%3A+declined%22

		// 获取所有的 BeanPostProcessor 的 BeanName
		// 这些 beanName 都已经全部加载到容器中去，但是没有实例化
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;

		// BeanPostProcessorChecker 记录实例化期间的日志
		// 可能存在有些情况当 Spring 配置中的后置处理器还没有被注册就已经开始 Bean 的实例化了，这个时候就会打印出 BeanPostProcessorChecker 中设定的信息
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		// 用于保存实现了 priorityOrdered 排序接口的 BeanPostProcessor
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// 用于保存 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		// 用于保存实现了 Ordered 排序接口的 BeanPostProcessor 的 BeanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// 用于保存没有实现任何排序接口的 BeanPostProcessor 的 BeanName
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		// 遍历所有的 BeanPostProcessor 的 BeanName，按照不同筛选条件放入相应的容器中
		for (String ppName : postProcessorNames) {
			// 如果实现了 PriorityOrdered 排序接口
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 根据 BeanName获取 Bean实例并放入到 priorityOrderedPostProcessors 中
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				// 如果是 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor 继续放入到 internalPostProcessors 中
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			// 如果实现了 Ordered 排序接口
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				// 将 BeanName 放入到 orderedPostProcessorNames 中
				orderedPostProcessorNames.add(ppName);
			}
			// 如果没有实现任何排序接口
			else {
				// 将 BeanName放入到 nonOrderedPostProcessorNames 中
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		// 对实现了 PriorityOrdered 排序接口的 BeanPostProcessor 进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 根据排序结果依次注册 BeanPostProcessor 到 BeanFactory中，
		// 实际就是将排序后的 BeanPostProcessor 保存到 AbstractBeanFactory#beanPostProcessor 容器中
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		// 创建一个容器用于存放实现了 Ordered 排序接口的 BeanPostProcessor
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		// 遍历实现了 Ordered 排序接口的 BeanPostProcessor 的 BeanName
		for (String ppName : orderedPostProcessorNames) {
			// 根据 BeanName 获取 Bean 实例放入到 orderedPostProcessors
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			// 如果是 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor 继续放入到 internalPostProcessors 中
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 对实现了 Ordered 排序接口的 BeanPostProcessor 的 Bean 实例进行排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		// 根据排序结果依次注册 BeanPostProcessor 到 BeanFactory 中,
		// 实际就是将排序后的 BeanPostProcessor 保存到 AbstractBeanFactory#beanPostProcessors 容器中
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		// 创建一个容器用于存放没有实现任何排序接口的 BeanPostProcessor
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		// 遍历没有实现任何排序接口的 BeanPostProcessor 的 BeanName
		for (String ppName : nonOrderedPostProcessorNames) {
			// 根据 BeanName 获取 Bean 实例并放入 nonOrderedPostProcessors 中
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			// 如果是 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor 继续放入 internalPostProcessors 中
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		// 注册 BeanPostProcessor 到 BeanFactory 中
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		// 对 MergedBeanDefinitionPostProcessor 类型的 BeanPostProcessor 进行排序
		sortPostProcessors(internalPostProcessors, beanFactory);
		//  注册 BeanPostProcessor 到 BeanFactory 中, 注册时会先执行移除已存在的 BeanPostProcessor 然后在进行添加
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		// 重新注册 ApplicationListenerDetector（探测器），用于探测内部 ApplicationListener 类型的 Bean
		// 在完全初始化 Bean 后，如果是 ApplicationListener 类型且为单例模式，则添加到 Spring 应用上下文
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry, ApplicationStartup applicationStartup) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanDefRegistry = applicationStartup.start("spring.context.beandef-registry.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanDefinitionRegistry(registry);
			postProcessBeanDefRegistry.end();
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			StartupStep postProcessBeanFactory = beanFactory.getApplicationStartup().start("spring.context.bean-factory.post-process")
					.tag("postProcessor", postProcessor::toString);
			postProcessor.postProcessBeanFactory(beanFactory);
			postProcessBeanFactory.end();
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		if (beanFactory instanceof AbstractBeanFactory) {
			// Bulk addition is more efficient against our CopyOnWriteArrayList there
			((AbstractBeanFactory) beanFactory).addBeanPostProcessors(postProcessors);
		}
		else {
			for (BeanPostProcessor postProcessor : postProcessors) {
				beanFactory.addBeanPostProcessor(postProcessor);
			}
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
