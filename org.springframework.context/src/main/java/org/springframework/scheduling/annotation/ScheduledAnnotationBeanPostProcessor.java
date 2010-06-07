/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.MethodInvokingRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringValueResolver;

/**
 * Bean post-processor that registers methods annotated with {@link Scheduled @Scheduled}
 * to be invoked by a {@link org.springframework.scheduling.TaskScheduler} according
 * to the "fixedRate", "fixedDelay", or "cron" expression provided via the annotation.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 * @see Scheduled
 * @see org.springframework.scheduling.TaskScheduler
 */
public class ScheduledAnnotationBeanPostProcessor
		implements BeanPostProcessor, Ordered, EmbeddedValueResolverAware, ApplicationContextAware,
		ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	private Object scheduler;

	private StringValueResolver embeddedValueResolver;

	private ApplicationContext applicationContext;

	private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

	private final Map<Runnable, String> cronTasks = new HashMap<Runnable, String>();

	private final Map<Runnable, Long> fixedDelayTasks = new HashMap<Runnable, Long>();

	private final Map<Runnable, Long> fixedRateTasks = new HashMap<Runnable, Long>();


	/**
	 * Set the {@link org.springframework.scheduling.TaskScheduler} that will invoke
	 * the scheduled methods, or a {@link java.util.concurrent.ScheduledExecutorService}
	 * to be wrapped as a TaskScheduler.
	 */
	public void setScheduler(Object scheduler) {
		this.scheduler = scheduler;
	}

	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}


	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	public Object postProcessAfterInitialization(final Object bean, String beanName) {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		ReflectionUtils.doWithMethods(targetClass, new MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Scheduled annotation = AnnotationUtils.getAnnotation(method, Scheduled.class);
				if (annotation != null) {
					Assert.isTrue(void.class.equals(method.getReturnType()),
							"Only void-returning methods may be annotated with @Scheduled.");
					Assert.isTrue(method.getParameterTypes().length == 0,
							"Only no-arg methods may be annotated with @Scheduled.");
					MethodInvokingRunnable runnable = new MethodInvokingRunnable();
					runnable.setTargetObject(bean);
					runnable.setTargetMethod(method.getName());
					runnable.setArguments(new Object[0]);
					try {
						runnable.prepare();
					}
					catch (Exception ex) {
						throw new IllegalStateException("failed to prepare task", ex);
					}
					boolean processedSchedule = false;
					String errorMessage = "Exactly one of 'cron', 'fixedDelay', or 'fixedRate' is required.";
					String cron = annotation.cron();
					if (!"".equals(cron)) {
						processedSchedule = true;
						if (embeddedValueResolver != null) {
							cron = embeddedValueResolver.resolveStringValue(cron);
						}
						cronTasks.put(runnable, cron);
					}
					long fixedDelay = annotation.fixedDelay();
					if (fixedDelay >= 0) {
						Assert.isTrue(!processedSchedule, errorMessage);
						processedSchedule = true;
						fixedDelayTasks.put(runnable, fixedDelay);
					}
					long fixedRate = annotation.fixedRate();
					if (fixedRate >= 0) {
						Assert.isTrue(!processedSchedule, errorMessage);
						processedSchedule = true;
						fixedRateTasks.put(runnable, fixedRate);
					}
					Assert.isTrue(processedSchedule, errorMessage);
				}
			}
		});
		return bean;
	}

	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext) {
			if (this.scheduler != null) {
				this.registrar.setScheduler(this.scheduler);
			}
			this.registrar.setCronTasks(this.cronTasks);
			this.registrar.setFixedDelayTasks(this.fixedDelayTasks);
			this.registrar.setFixedRateTasks(this.fixedRateTasks);
			this.registrar.afterPropertiesSet();
		}
	}

	public void destroy() throws Exception {
		if (this.registrar != null) {
			this.registrar.destroy();
		}
	}

}
