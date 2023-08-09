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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TaskSchedulerRouter;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * Bean post-processor that registers methods annotated with
 * {@link Scheduled @Scheduled} to be invoked by a
 * {@link org.springframework.scheduling.TaskScheduler} according to the
 * "fixedRate", "fixedDelay", or "cron" expression provided via the annotation.
 *
 * <p>This post-processor is automatically registered by Spring's
 * {@code <task:annotation-driven>} XML element and also by the
 * {@link EnableScheduling @EnableScheduling} annotation.
 *
 * <p>Autodetects any {@link SchedulingConfigurer} instances in the container,
 * allowing for customization of the scheduler to be used or for fine-grained
 * control over task registration (e.g. registration of {@link Trigger} tasks).
 * See the {@link EnableScheduling @EnableScheduling} javadocs for complete usage
 * details.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Elizabeth Chatman
 * @author Victor Brown
 * @author Sam Brannen
 * @author Simon Baslé
 * @since 3.0
 * @see Scheduled
 * @see EnableScheduling
 * @see SchedulingConfigurer
 * @see org.springframework.scheduling.TaskScheduler
 * @see org.springframework.scheduling.config.ScheduledTaskRegistrar
 * @see AsyncAnnotationBeanPostProcessor
 */
public class ScheduledAnnotationBeanPostProcessor
		implements ScheduledTaskHolder, MergedBeanDefinitionPostProcessor, DestructionAwareBeanPostProcessor,
		Ordered, EmbeddedValueResolverAware, BeanNameAware, BeanFactoryAware, ApplicationContextAware,
		SmartInitializingSingleton, DisposableBean, ApplicationListener<ApplicationContextEvent> {

	/**
	 * The default name of the {@link TaskScheduler} bean to pick up: {@value}.
	 * <p>Note that the initial lookup happens by type; this is just the fallback
	 * in case of multiple scheduler beans found in the context.
	 * @since 4.2
	 */
	public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = TaskSchedulerRouter.DEFAULT_TASK_SCHEDULER_BEAN_NAME;


	/**
	 * Reactive Streams API present on the classpath?
	 */
	private static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", ScheduledAnnotationBeanPostProcessor.class.getClassLoader());

	protected final Log logger = LogFactory.getLog(getClass());

	private final ScheduledTaskRegistrar registrar;

	@Nullable
	private Object scheduler;

	@Nullable
	private StringValueResolver embeddedValueResolver;

	@Nullable
	private String beanName;

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private TaskSchedulerRouter localScheduler;

	private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));

	private final Map<Object, Set<ScheduledTask>> scheduledTasks = new IdentityHashMap<>(16);

	private final Map<Object, List<Runnable>> reactiveSubscriptions = new IdentityHashMap<>(16);


	/**
	 * Create a default {@code ScheduledAnnotationBeanPostProcessor}.
	 */
	public ScheduledAnnotationBeanPostProcessor() {
		this.registrar = new ScheduledTaskRegistrar();
	}

	/**
	 * Create a {@code ScheduledAnnotationBeanPostProcessor} delegating to the
	 * specified {@link ScheduledTaskRegistrar}.
	 * @param registrar the ScheduledTaskRegistrar to register {@code @Scheduled}
	 * tasks on
	 * @since 5.1
	 */
	public ScheduledAnnotationBeanPostProcessor(ScheduledTaskRegistrar registrar) {
		Assert.notNull(registrar, "ScheduledTaskRegistrar must not be null");
		this.registrar = registrar;
	}


	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	/**
	 * Set the {@link org.springframework.scheduling.TaskScheduler} that will invoke
	 * the scheduled methods, or a {@link java.util.concurrent.ScheduledExecutorService}
	 * to be wrapped as a TaskScheduler.
	 * <p>If not specified, default scheduler resolution will apply: searching for a
	 * unique {@link TaskScheduler} bean in the context, or for a {@link TaskScheduler}
	 * bean named "taskScheduler" otherwise; the same lookup will also be performed for
	 * a {@link ScheduledExecutorService} bean. If neither of the two is resolvable,
	 * a local single-threaded default scheduler will be created within the registrar.
	 * @see #DEFAULT_TASK_SCHEDULER_BEAN_NAME
	 */
	public void setScheduler(Object scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Making a {@link BeanFactory} available is optional; if not set,
	 * {@link SchedulingConfigurer} beans won't get autodetected and
	 * a {@link #setScheduler scheduler} has to be explicitly configured.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Setting an {@link ApplicationContext} is optional: If set, registered
	 * tasks will be activated in the {@link ContextRefreshedEvent} phase;
	 * if not set, it will happen at {@link #afterSingletonsInstantiated} time.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		if (this.beanFactory == null) {
			this.beanFactory = applicationContext;
		}
	}


	@Override
	public void afterSingletonsInstantiated() {
		// Remove resolved singleton classes from cache
		this.nonAnnotatedClasses.clear();

		if (this.applicationContext == null) {
			// Not running in an ApplicationContext -> register tasks early...
			finishRegistration();
		}
	}

	private void finishRegistration() {
		if (this.scheduler != null) {
			this.registrar.setScheduler(this.scheduler);
		}
		else {
			this.localScheduler = new TaskSchedulerRouter();
			this.localScheduler.setBeanName(this.beanName);
			this.localScheduler.setBeanFactory(this.beanFactory);
			this.registrar.setTaskScheduler(this.localScheduler);
		}

		if (this.beanFactory instanceof ListableBeanFactory lbf) {
			Map<String, SchedulingConfigurer> beans = lbf.getBeansOfType(SchedulingConfigurer.class);
			List<SchedulingConfigurer> configurers = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(configurers);
			for (SchedulingConfigurer configurer : configurers) {
				configurer.configureTasks(this.registrar);
			}
		}

		this.registrar.afterPropertiesSet();
	}


	@Override
	public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AopInfrastructureBean || bean instanceof TaskScheduler ||
				bean instanceof ScheduledExecutorService) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}

		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
		if (!this.nonAnnotatedClasses.contains(targetClass) &&
				AnnotationUtils.isCandidateClass(targetClass, List.of(Scheduled.class, Schedules.class))) {
			Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
					(MethodIntrospector.MetadataLookup<Set<Scheduled>>) method -> {
						Set<Scheduled> scheduledAnnotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(
								method, Scheduled.class, Schedules.class);
						return (!scheduledAnnotations.isEmpty() ? scheduledAnnotations : null);
					});
			if (annotatedMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(targetClass);
				if (logger.isTraceEnabled()) {
					logger.trace("No @Scheduled annotations found on bean class: " + targetClass);
				}
			}
			else {
				// Non-empty set of methods
				annotatedMethods.forEach((method, scheduledAnnotations) ->
						scheduledAnnotations.forEach(scheduled -> processScheduled(scheduled, method, bean)));
				if (logger.isTraceEnabled()) {
					logger.trace(annotatedMethods.size() + " @Scheduled methods processed on bean '" + beanName +
							"': " + annotatedMethods);
				}
			}
		}
		return bean;
	}

	/**
	 * Process the given {@code @Scheduled} method declaration on the given bean,
	 * attempting to distinguish {@linkplain #processScheduledAsync(Scheduled, Method, Object)
	 * reactive} methods from {@linkplain #processScheduledSync(Scheduled, Method, Object)
	 * synchronous} methods.
	 * @param scheduled the {@code @Scheduled} annotation
	 * @param method the method that the annotation has been declared on
	 * @param bean the target bean instance
	 * @see #processScheduledSync(Scheduled, Method, Object)
	 * @see #processScheduledAsync(Scheduled, Method, Object)
	 */
	protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
		// Is the method a Kotlin suspending function? Throws if true and the reactor bridge isn't on the classpath.
		// Does the method return a reactive type? Throws if true and it isn't a deferred Publisher type.
		if (reactiveStreamsPresent && ScheduledAnnotationReactiveSupport.isReactive(method)) {
			processScheduledAsync(scheduled, method, bean);
			return;
		}
		processScheduledSync(scheduled, method, bean);
	}

	/**
	 * Process the given {@code @Scheduled} method declaration on the given bean,
	 * as a synchronous method. The method must accept no arguments. Its return value
	 * is ignored (if any), and the scheduled invocations of the method take place
	 * using the underlying {@link TaskScheduler} infrastructure.
	 * @param scheduled the {@code @Scheduled} annotation
	 * @param method the method that the annotation has been declared on
	 * @param bean the target bean instance
	 */
	private void processScheduledSync(Scheduled scheduled, Method method, Object bean) {
		Runnable task;
		try {
			task = createRunnable(bean, method, scheduled.scheduler());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Could not create recurring task for @Scheduled method '" +
					method.getName() + "': " + ex.getMessage());
		}
		processScheduledTask(scheduled, task, method, bean);
	}

	/**
	 * Process the given {@code @Scheduled} bean method declaration which returns
	 * a {@code Publisher}, or the given Kotlin suspending function converted to a
	 * {@code Publisher}. A {@code Runnable} which subscribes to that publisher is
	 * then repeatedly scheduled according to the annotation configuration.
	 * <p>Note that for fixed delay configuration, the subscription is turned into a blocking
	 * call instead. Types for which a {@code ReactiveAdapter} is registered but which cannot
	 * be deferred (i.e. not a {@code Publisher}) are not supported.
	 * @param scheduled the {@code @Scheduled} annotation
	 * @param method the method that the annotation has been declared on, which
	 * must either return a Publisher-adaptable type or be a Kotlin suspending function
	 * @param bean the target bean instance
	 * @see ScheduledAnnotationReactiveSupport
	 */
	private void processScheduledAsync(Scheduled scheduled, Method method, Object bean) {
		Runnable task;
		try {
			task = ScheduledAnnotationReactiveSupport.createSubscriptionRunnable(method, bean, scheduled,
					this.registrar::getObservationRegistry,
					this.reactiveSubscriptions.computeIfAbsent(bean, k -> new CopyOnWriteArrayList<>()));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Could not create recurring task for @Scheduled method '" +
					method.getName() + "': " + ex.getMessage());
		}
		processScheduledTask(scheduled, task, method, bean);
	}

	/**
	 * Parse the {@code Scheduled} annotation and schedule the provided {@code Runnable}
	 * accordingly. The Runnable can represent either a synchronous method invocation
	 * (see {@link #processScheduledSync(Scheduled, Method, Object)}) or an asynchronous
	 * one (see {@link #processScheduledAsync(Scheduled, Method, Object)}).
	 * @param scheduled the {@code @Scheduled} annotation
	 * @param runnable the runnable to be scheduled
	 * @param method the method that the annotation has been declared on
	 * @param bean the target bean instance
	 */
	private void processScheduledTask(Scheduled scheduled, Runnable runnable, Method method, Object bean) {
		try {
			boolean processedSchedule = false;
			String errorMessage =
					"Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";

			Set<ScheduledTask> tasks = new LinkedHashSet<>(4);

			// Determine initial delay
			Duration initialDelay = toDuration(scheduled.initialDelay(), scheduled.timeUnit());
			String initialDelayString = scheduled.initialDelayString();
			if (StringUtils.hasText(initialDelayString)) {
				Assert.isTrue(initialDelay.isNegative(), "Specify 'initialDelay' or 'initialDelayString', not both");
				if (this.embeddedValueResolver != null) {
					initialDelayString = this.embeddedValueResolver.resolveStringValue(initialDelayString);
				}
				if (StringUtils.hasLength(initialDelayString)) {
					try {
						initialDelay = toDuration(initialDelayString, scheduled.timeUnit());
					}
					catch (RuntimeException ex) {
						throw new IllegalArgumentException(
								"Invalid initialDelayString value \"" + initialDelayString + "\" - cannot parse into long");
					}
				}
			}

			// Check cron expression
			String cron = scheduled.cron();
			if (StringUtils.hasText(cron)) {
				String zone = scheduled.zone();
				if (this.embeddedValueResolver != null) {
					cron = this.embeddedValueResolver.resolveStringValue(cron);
					zone = this.embeddedValueResolver.resolveStringValue(zone);
				}
				if (StringUtils.hasLength(cron)) {
					Assert.isTrue(initialDelay.isNegative(), "'initialDelay' not supported for cron triggers");
					processedSchedule = true;
					if (!Scheduled.CRON_DISABLED.equals(cron)) {
						TimeZone timeZone;
						if (StringUtils.hasText(zone)) {
							timeZone = StringUtils.parseTimeZoneString(zone);
						}
						else {
							timeZone = TimeZone.getDefault();
						}
						tasks.add(this.registrar.scheduleCronTask(new CronTask(runnable, new CronTrigger(cron, timeZone))));
					}
				}
			}

			// At this point we don't need to differentiate between initial delay set or not anymore
			if (initialDelay.isNegative()) {
				initialDelay = Duration.ZERO;
			}

			// Check fixed delay
			Duration fixedDelay = toDuration(scheduled.fixedDelay(), scheduled.timeUnit());
			if (!fixedDelay.isNegative()) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				tasks.add(this.registrar.scheduleFixedDelayTask(new FixedDelayTask(runnable, fixedDelay, initialDelay)));
			}

			String fixedDelayString = scheduled.fixedDelayString();
			if (StringUtils.hasText(fixedDelayString)) {
				if (this.embeddedValueResolver != null) {
					fixedDelayString = this.embeddedValueResolver.resolveStringValue(fixedDelayString);
				}
				if (StringUtils.hasLength(fixedDelayString)) {
					Assert.isTrue(!processedSchedule, errorMessage);
					processedSchedule = true;
					try {
						fixedDelay = toDuration(fixedDelayString, scheduled.timeUnit());
					}
					catch (RuntimeException ex) {
						throw new IllegalArgumentException(
								"Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into long");
					}
					tasks.add(this.registrar.scheduleFixedDelayTask(new FixedDelayTask(runnable, fixedDelay, initialDelay)));
				}
			}

			// Check fixed rate
			Duration fixedRate = toDuration(scheduled.fixedRate(), scheduled.timeUnit());
			if (!fixedRate.isNegative()) {
				Assert.isTrue(!processedSchedule, errorMessage);
				processedSchedule = true;
				tasks.add(this.registrar.scheduleFixedRateTask(new FixedRateTask(runnable, fixedRate, initialDelay)));
			}
			String fixedRateString = scheduled.fixedRateString();
			if (StringUtils.hasText(fixedRateString)) {
				if (this.embeddedValueResolver != null) {
					fixedRateString = this.embeddedValueResolver.resolveStringValue(fixedRateString);
				}
				if (StringUtils.hasLength(fixedRateString)) {
					Assert.isTrue(!processedSchedule, errorMessage);
					processedSchedule = true;
					try {
						fixedRate = toDuration(fixedRateString, scheduled.timeUnit());
					}
					catch (RuntimeException ex) {
						throw new IllegalArgumentException(
								"Invalid fixedRateString value \"" + fixedRateString + "\" - cannot parse into long");
					}
					tasks.add(this.registrar.scheduleFixedRateTask(new FixedRateTask(runnable, fixedRate, initialDelay)));
				}
			}

			// Check whether we had any attribute set
			Assert.isTrue(processedSchedule, errorMessage);

			// Finally register the scheduled tasks
			synchronized (this.scheduledTasks) {
				Set<ScheduledTask> regTasks = this.scheduledTasks.computeIfAbsent(bean, key -> new LinkedHashSet<>(4));
				regTasks.addAll(tasks);
			}
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"Encountered invalid @Scheduled method '" + method.getName() + "': " + ex.getMessage());
		}
	}

	/**
	 * Create a {@link Runnable} for the given bean instance,
	 * calling the specified scheduled method.
	 * <p>The default implementation creates a {@link ScheduledMethodRunnable}.
	 * @param target the target bean instance
	 * @param method the scheduled method to call
	 * @since 6.1
	 */
	@SuppressWarnings("deprecation")
	protected Runnable createRunnable(Object target, Method method, @Nullable String qualifier) {
		Runnable runnable = createRunnable(target, method);
		if (runnable != null) {
			return runnable;
		}
		Assert.isTrue(method.getParameterCount() == 0, "Only no-arg methods may be annotated with @Scheduled");
		Method invocableMethod = AopUtils.selectInvocableMethod(method, target.getClass());
		return new ScheduledMethodRunnable(target, invocableMethod, qualifier, this.registrar::getObservationRegistry);
	}

	/**
	 * Create a {@link Runnable} for the given bean instance,
	 * calling the specified scheduled method.
	 * @param target the target bean instance
	 * @param method the scheduled method to call
	 * @since 5.1
	 * @deprecated in favor of {@link #createRunnable(Object, Method, String)}
	 */
	@Deprecated(since = "6.1")
	@Nullable
	protected Runnable createRunnable(Object target, Method method) {
		return null;
	}

	private static Duration toDuration(long value, TimeUnit timeUnit) {
		return Duration.of(value, timeUnit.toChronoUnit());
	}

	private static Duration toDuration(String value, TimeUnit timeUnit) {
		if (isDurationString(value)) {
			return Duration.parse(value);
		}
		return toDuration(Long.parseLong(value), timeUnit);
	}

	private static boolean isDurationString(String value) {
		return (value.length() > 1 && (isP(value.charAt(0)) || isP(value.charAt(1))));
	}

	private static boolean isP(char ch) {
		return (ch == 'P' || ch == 'p');
	}


	/**
	 * Return all currently scheduled tasks, from {@link Scheduled} methods
	 * as well as from programmatic {@link SchedulingConfigurer} interaction.
	 * <p>Note that this includes upcoming scheduled subscriptions for reactive
	 * methods but doesn't cover any currently active subscription for such methods.
	 * @since 5.0.2
	 */
	@Override
	public Set<ScheduledTask> getScheduledTasks() {
		Set<ScheduledTask> result = new LinkedHashSet<>();
		synchronized (this.scheduledTasks) {
			Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
			for (Set<ScheduledTask> tasks : allTasks) {
				result.addAll(tasks);
			}
		}
		result.addAll(this.registrar.getScheduledTasks());
		return result;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) {
		Set<ScheduledTask> tasks;
		List<Runnable> liveSubscriptions;
		synchronized (this.scheduledTasks) {
			tasks = this.scheduledTasks.remove(bean);
			liveSubscriptions = this.reactiveSubscriptions.remove(bean);
		}
		if (tasks != null) {
			for (ScheduledTask task : tasks) {
				task.cancel(false);
			}
		}
		if (liveSubscriptions != null) {
			for (Runnable subscription : liveSubscriptions) {
				subscription.run();  // equivalent to cancelling the subscription
			}
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		synchronized (this.scheduledTasks) {
			return (this.scheduledTasks.containsKey(bean) || this.reactiveSubscriptions.containsKey(bean));
		}
	}

	@Override
	public void destroy() {
		synchronized (this.scheduledTasks) {
			Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
			for (Set<ScheduledTask> tasks : allTasks) {
				for (ScheduledTask task : tasks) {
					task.cancel(false);
				}
			}
			this.scheduledTasks.clear();
			Collection<List<Runnable>> allLiveSubscriptions = this.reactiveSubscriptions.values();
			for (List<Runnable> liveSubscriptions : allLiveSubscriptions) {
				for (Runnable liveSubscription : liveSubscriptions) {
					liveSubscription.run();  // equivalent to cancelling the subscription
				}
			}
		}
		this.registrar.destroy();
		if (this.localScheduler != null) {
			this.localScheduler.destroy();
		}
	}


	/**
	 * Reacts to {@link ContextRefreshedEvent} as well as {@link ContextClosedEvent}:
	 * performing {@link #finishRegistration()} and early cancelling of scheduled tasks,
	 * respectively.
	 */
	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			if (event instanceof ContextRefreshedEvent) {
				// Running in an ApplicationContext -> register tasks this late...
				// giving other ContextRefreshedEvent listeners a chance to perform
				// their work at the same time (e.g. Spring Batch's job registration).
				finishRegistration();
			}
			else if (event instanceof ContextClosedEvent) {
				synchronized (this.scheduledTasks) {
					Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
					for (Set<ScheduledTask> tasks : allTasks) {
						for (ScheduledTask task : tasks) {
							// At this early point, let in-progress tasks complete still
							task.cancel(false);
						}
					}
				}
			}
		}
	}

}
