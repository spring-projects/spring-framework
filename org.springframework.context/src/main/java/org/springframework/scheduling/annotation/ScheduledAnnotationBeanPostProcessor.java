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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.MethodInvokingRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * Bean post-processor that registers methods annotated with
 * {@link Scheduled @Scheduled} to be invoked by a TaskScheduler according
 * to the fixedRate, fixedDelay, or cron expression provided via the annotation.
 *
 * @author Mark Fisher
 * @since 3.0
 * @see Scheduled
 */
public class ScheduledAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered,
		ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	private Object scheduler;

	private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

	private final Map<Runnable, String> cronTasks = new HashMap<Runnable, String>();

	private final Map<Runnable, Long> fixedDelayTasks = new HashMap<Runnable, Long>();

	private final Map<Runnable, Long> fixedRateTasks = new HashMap<Runnable, Long>();


	/**
	 * Set the {@link org.springframework.scheduling.TaskScheduler} that will
	 * invoke the scheduled methods or a
	 * {@link java.util.concurrent.ScheduledExecutorService} to be wrapped
	 * within an instance of
	 * {@link org.springframework.scheduling.concurrent.ConcurrentTaskScheduler}.
	 */
	public void setScheduler(Object scheduler) {
		this.scheduler = scheduler;
	}

	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessAfterInitialization(final Object bean, String beanName) throws BeansException {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (targetClass == null) {
			return bean;
		}
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
					catch (Exception e) {
						throw new IllegalStateException("failed to prepare task", e);
					}
					boolean processedSchedule = false;
					String errorMessage = "Exactly one of 'cron', 'fixedDelay', or 'fixedRate' is required.";
					String cron = annotation.cron();
					if (!"".equals(cron)) {
						processedSchedule = true;
						cronTasks.put(runnable, cron);
					}
					long fixedDelay = annotation.fixedDelay();
					if (fixedDelay >= 0) {
						Assert.isTrue(!processedSchedule, errorMessage);
						processedSchedule = true;
						fixedDelayTasks.put(runnable, new Long(fixedDelay));
					}
					long fixedRate = annotation.fixedRate();
					if (fixedRate >= 0) {
						Assert.isTrue(!processedSchedule, errorMessage);
						processedSchedule = true;
						fixedRateTasks.put(runnable, new Long(fixedRate));
					}
					Assert.isTrue(processedSchedule, errorMessage);
				}
			}
		});
		return bean;
	}

	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (scheduler != null) {
			this.registrar.setScheduler(scheduler);
		}
		this.registrar.setCronTasks(this.cronTasks);
		this.registrar.setFixedDelayTasks(this.fixedDelayTasks);
		this.registrar.setFixedRateTasks(this.fixedRateTasks);
		this.registrar.afterPropertiesSet();
	}

	public void destroy() throws Exception {
		if (this.registrar != null) {
			this.registrar.destroy();
		}
	}

}
