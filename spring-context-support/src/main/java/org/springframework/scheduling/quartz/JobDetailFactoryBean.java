/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A Spring {@link FactoryBean} for creating a Quartz {@link org.quartz.JobDetail}
 * instance, supporting bean-style usage for JobDetail configuration.
 *
 * <p>{@code JobDetail(Impl)} itself is already a JavaBean but lacks
 * sensible defaults. This class uses the Spring bean name as job name,
 * and the Quartz default group ("DEFAULT") as job group if not specified.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setName
 * @see #setGroup
 * @see org.springframework.beans.factory.BeanNameAware
 * @see org.quartz.Scheduler#DEFAULT_GROUP
 */
public class JobDetailFactoryBean
		implements FactoryBean<JobDetail>, BeanNameAware, ApplicationContextAware, InitializingBean {

	@Nullable
	private String name;

	@Nullable
	private String group;

	@Nullable
	private Class<? extends Job> jobClass;

	private JobDataMap jobDataMap = new JobDataMap();

	private boolean durability = false;

	private boolean requestsRecovery = false;

	@Nullable
	private String description;

	@Nullable
	private String beanName;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private String applicationContextJobDataKey;

	@Nullable
	private JobDetail jobDetail;


	/**
	 * Specify the job's name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Specify the job's group.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Specify the job's implementation class.
	 */
	public void setJobClass(Class<? extends Job> jobClass) {
		this.jobClass = jobClass;
	}

	/**
	 * Set the job's JobDataMap.
	 * @see #setJobDataAsMap
	 */
	public void setJobDataMap(JobDataMap jobDataMap) {
		this.jobDataMap = jobDataMap;
	}

	/**
	 * Return the job's JobDataMap.
	 */
	public JobDataMap getJobDataMap() {
		return this.jobDataMap;
	}

	/**
	 * Register objects in the JobDataMap via a given Map.
	 * <p>These objects will be available to this Job only,
	 * in contrast to objects in the SchedulerContext.
	 * <p>Note: When using persistent Jobs whose JobDetail will be kept in the
	 * database, do not put Spring-managed beans or an ApplicationContext
	 * reference into the JobDataMap but rather into the SchedulerContext.
	 * @param jobDataAsMap a Map with String keys and any objects as values
	 * (for example Spring-managed beans)
	 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean#setSchedulerContextAsMap
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		getJobDataMap().putAll(jobDataAsMap);
	}

	/**
	 * Specify the job's durability, i.e. whether it should remain stored
	 * in the job store even if no triggers point to it anymore.
	 */
	public void setDurability(boolean durability) {
		this.durability = durability;
	}

	/**
	 * Set the recovery flag for this job, i.e. whether the job should
	 * get re-executed if a 'recovery' or 'fail-over' situation is encountered.
	 */
	public void setRequestsRecovery(boolean requestsRecovery) {
		this.requestsRecovery = requestsRecovery;
	}

	/**
	 * Set a textual description for this job.
	 */
	public void setDescription(String description) {
		this.description = description;
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
	 * @see org.springframework.scheduling.quartz.SchedulerFactoryBean#setApplicationContextSchedulerContextKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextJobDataKey(String applicationContextJobDataKey) {
		this.applicationContextJobDataKey = applicationContextJobDataKey;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.jobClass, "Property 'jobClass' is required");

		if (this.name == null) {
			this.name = this.beanName;
		}
		if (this.group == null) {
			this.group = Scheduler.DEFAULT_GROUP;
		}
		if (this.applicationContextJobDataKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
						"JobDetailBean needs to be set up in an ApplicationContext " +
						"to be able to handle an 'applicationContextJobDataKey'");
			}
			getJobDataMap().put(this.applicationContextJobDataKey, this.applicationContext);
		}

		JobDetailImpl jdi = new JobDetailImpl();
		jdi.setName(this.name != null ? this.name : toString());
		jdi.setGroup(this.group);
		jdi.setJobClass(this.jobClass);
		jdi.setJobDataMap(this.jobDataMap);
		jdi.setDurability(this.durability);
		jdi.setRequestsRecovery(this.requestsRecovery);
		jdi.setDescription(this.description);
		this.jobDetail = jdi;
	}


	@Override
	@Nullable
	public JobDetail getObject() {
		return this.jobDetail;
	}

	@Override
	public Class<?> getObjectType() {
		return JobDetail.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
