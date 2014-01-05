/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Convenience subclass of Quartz's {@link org.quartz.JobDetail} class,
 * making bean-style usage easier.
 *
 * <p>{@code JobDetail} itself is already a JavaBean but lacks
 * sensible defaults. This class uses the Spring bean name as job name,
 * and the Quartz default group ("DEFAULT") as job group if not specified.
 *
 * <p><b>NOTE: This convenience subclass does not work against Quartz 2.0.</b>
 * Use Quartz 2.0's native {@code JobDetailImpl} class or the new Quartz 2.0
 * builder API instead. Alternatively, switch to Spring's {@link JobDetailFactoryBean}
 * which largely is a drop-in replacement for this class and its properties and
 * consistently works against Quartz 1.x as well as Quartz 2.x.
 *
 * @author Juergen Hoeller
 * @since 18.02.2004
 * @see #setName
 * @see #setGroup
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.quartz.Scheduler#DEFAULT_GROUP
 */
@SuppressWarnings({"serial", "rawtypes"})
public class JobDetailBean extends JobDetail
		implements BeanNameAware, ApplicationContextAware, InitializingBean {

	private Class<?> actualJobClass;

	private String beanName;

	private ApplicationContext applicationContext;

	private String applicationContextJobDataKey;


	/**
	 * Overridden to support any job class, to allow a custom JobFactory
	 * to adapt the given job class to the Quartz Job interface.
	 * @see SchedulerFactoryBean#setJobFactory
	 */
	@Override
	public void setJobClass(Class jobClass) {
		if (jobClass != null && !Job.class.isAssignableFrom(jobClass)) {
			super.setJobClass(DelegatingJob.class);
			this.actualJobClass = jobClass;
		}
		else {
			super.setJobClass(jobClass);
		}
	}

	/**
	 * Overridden to support any job class, to allow a custom JobFactory
	 * to adapt the given job class to the Quartz Job interface.
	 */
	@Override
	public Class getJobClass() {
		return (this.actualJobClass != null ? this.actualJobClass : super.getJobClass());
	}

	/**
	 * Register objects in the JobDataMap via a given Map.
	 * <p>These objects will be available to this Job only,
	 * in contrast to objects in the SchedulerContext.
	 * <p>Note: When using persistent Jobs whose JobDetail will be kept in the
	 * database, do not put Spring-managed beans or an ApplicationContext
	 * reference into the JobDataMap but rather into the SchedulerContext.
	 * @param jobDataAsMap Map with String keys and any objects as values
	 * (for example Spring-managed beans)
	 * @see SchedulerFactoryBean#setSchedulerContextAsMap
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		getJobDataMap().putAll(jobDataAsMap);
	}

	/**
	 * Set a list of JobListener names for this job, referring to
	 * non-global JobListeners registered with the Scheduler.
	 * <p>A JobListener name always refers to the name returned
	 * by the JobListener implementation.
	 * @see SchedulerFactoryBean#setJobListeners
	 * @see org.quartz.JobListener#getName
	 * @deprecated as of Spring 4.0, since it only works on Quartz 1.x
	 */
	@Deprecated
	public void setJobListenerNames(String... names) {
		for (String name : names) {
			addJobListener(name);
		}
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Set the key of an ApplicationContext reference to expose in the JobDataMap,
	 * for example "applicationContext". Default is none.
	 * Only applicable when running in a Spring ApplicationContext.
	 * <p>In case of a QuartzJobBean, the reference will be applied to the Job
	 * instance as bean property. An "applicationContext" attribute will correspond
	 * to a "setApplicationContext" method in that scenario.
	 * <p>Note that BeanFactory callback interfaces like ApplicationContextAware
	 * are not automatically applied to Quartz Job instances, because Quartz
	 * itself is responsible for the lifecycle of its Jobs.
	 * <p><b>Note: When using persistent job stores where JobDetail contents will
	 * be kept in the database, do not put an ApplicationContext reference into
	 * the JobDataMap but rather into the SchedulerContext.</b>
	 * @see SchedulerFactoryBean#setApplicationContextSchedulerContextKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextJobDataKey(String applicationContextJobDataKey) {
		this.applicationContextJobDataKey = applicationContextJobDataKey;
	}


	@Override
	public void afterPropertiesSet() {
		if (getName() == null) {
			setName(this.beanName);
		}
		if (getGroup() == null) {
			setGroup(Scheduler.DEFAULT_GROUP);
		}
		if (this.applicationContextJobDataKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
					"JobDetailBean needs to be set up in an ApplicationContext " +
					"to be able to handle an 'applicationContextJobDataKey'");
			}
			getJobDataMap().put(this.applicationContextJobDataKey, this.applicationContext);
		}
	}

}
