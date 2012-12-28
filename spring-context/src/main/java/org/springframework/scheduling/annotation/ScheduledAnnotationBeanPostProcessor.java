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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringValueResolver;

/**
 * Bean post-processor that registers methods annotated with @{@link Scheduled}
 * to be invoked by a {@link org.springframework.scheduling.TaskScheduler} according
 * to the "fixedRate", "fixedDelay", or "cron" expression provided via the annotation.
 *
 * <p>This post-processor is automatically registered by Spring's
 * {@code <task:annotation-driven>} XML element, and also by the @{@link EnableScheduling}
 * annotation.
 *
 * <p>Auto-detects any {@link SchedulingConfigurer} instances in the container,
 * allowing for customization of the scheduler to be used or for fine-grained control
 * over task registration (e.g. registration of {@link Trigger} tasks.
 * See @{@link EnableScheduling} Javadoc for complete usage details.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see Scheduled
 * @see EnableScheduling
 * @see SchedulingConfigurer
 * @see org.springframework.scheduling.TaskScheduler
 * @see org.springframework.scheduling.config.ScheduledTaskRegistrar
 */
public class ScheduledAnnotationBeanPostProcessor
		implements BeanPostProcessor, Ordered, EmbeddedValueResolverAware, ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	private Object scheduler;

	private StringValueResolver embeddedValueResolver;

	private ApplicationContext applicationContext;

	private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();


	/**
	 * Set the {@link org.springframework.scheduling.TaskScheduler} that will invoke
	 * the scheduled methods, or a {@link java.util.concurrent.ScheduledExecutorService}
	 * to be wrapped as a TaskScheduler.
	 */
	public void setScheduler(Object scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(final Object bean, String beanName) {
		final Class<?> targetClass = AopUtils.getTargetClass(bean);
		ReflectionUtils.doWithMethods(targetClass, new MethodCallback() {
			@Override
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Scheduled annotation = AnnotationUtils.getAnnotation(method, Scheduled.class);
				if (annotation != null) {
					Assert.isTrue(void.class.equals(method.getReturnType()),
							"Only void-returning methods may be annotated with @Scheduled.");
					Assert.isTrue(method.getParameterTypes().length == 0,
							"Only no-arg methods may be annotated with @Scheduled.");
					if (AopUtils.isJdkDynamicProxy(bean)) {
						try {
							// found a @Scheduled method on the target class for this JDK proxy -> is it
							// also present on the proxy itself?
							method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
						}
						catch (SecurityException ex) {
							ReflectionUtils.handleReflectionException(ex);
						}
						catch (NoSuchMethodException ex) {
							throw new IllegalStateException(String.format(
									"@Scheduled method '%s' found on bean target class '%s', " +
									"but not found in any interface(s) for bean JDK proxy. Either " +
									"pull the method up to an interface or switch to subclass (CGLIB) " +
									"proxies by setting proxy-target-class/proxyTargetClass " +
									"attribute to 'true'", method.getName(), targetClass.getSimpleName()));
						}
					}
					Runnable runnable = new ScheduledMethodRunnable(bean, method);
					boolean processedSchedule = false;
					String errorMessage = "Exactly one of the 'cron', 'fixedDelay', or 'fixedRate' attributes is required.";
					String cron = annotation.cron();
					if (!"".equals(cron)) {
						processedSchedule = true;
						if (embeddedValueResolver != null) {
							cron = embeddedValueResolver.resolveStringValue(cron);
						}
						registrar.addCronTask(new CronTask(runnable, cron));
					}
					long initialDelay = annotation.initialDelay();
					long fixedDelay = annotation.fixedDelay();
					if (fixedDelay >= 0) {
						Assert.isTrue(!processedSchedule, errorMessage);
						processedSchedule = true;
						registrar.addFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay));
					}
					long fixedRate = annotation.fixedRate();
					if (fixedRate >= 0) {
						Assert.isTrue(!processedSchedule, errorMessage);
						processedSchedule = true;
						registrar.addFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay));
					}
					Assert.isTrue(processedSchedule, errorMessage);
				}
			}
		});
		return bean;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() != this.applicationContext) {
			return;
		}

		Map<String, SchedulingConfigurer> configurers =
				this.applicationContext.getBeansOfType(SchedulingConfigurer.class);

		if (this.scheduler != null) {
			this.registrar.setScheduler(this.scheduler);
		}

		for (SchedulingConfigurer configurer : configurers.values()) {
			configurer.configureTasks(this.registrar);
		}

		if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
			Map<String, ? super Object> schedulers = new HashMap<String, Object>();
			schedulers.putAll(applicationContext.getBeansOfType(TaskScheduler.class));
			schedulers.putAll(applicationContext.getBeansOfType(ScheduledExecutorService.class));
			if (schedulers.size() == 0) {
				// do nothing -> fall back to default scheduler
			}
			else if (schedulers.size() == 1) {
				this.registrar.setScheduler(schedulers.values().iterator().next());
			}
			else if (schedulers.size() >= 2){
				throw new IllegalStateException(
						"More than one TaskScheduler and/or ScheduledExecutorService  " +
						"exist within the context. Remove all but one of the beans; or " +
						"implement the SchedulingConfigurer interface and call " +
						"ScheduledTaskRegistrar#setScheduler explicitly within the " +
						"configureTasks() callback. Found the following beans: " + schedulers.keySet());
			}
		}

		this.registrar.afterPropertiesSet();
	}

	@Override
	public void destroy() throws Exception {
		if (this.registrar != null) {
			this.registrar.destroy();
		}
	}

}
