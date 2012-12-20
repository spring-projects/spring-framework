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

package org.springframework.scheduling.quartz;

import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A Spring {@link FactoryBean} for creating a Quartz {@link org.quartz.JobDetail}
 * instance, supporting bean-style usage for JobDetail configuration.
 *
 * <p><code>JobDetail(Impl)</code> itself is already a JavaBean but lacks
 * sensible defaults. This class uses the Spring bean name as job name,
 * and the Quartz default group ("DEFAULT") as job group if not specified.
 *
 * <p><b>NOTE:</b> This FactoryBean works against both Quartz 1.x and Quartz 2.0/2.1,
 * in contrast to the older {@link JobDetailBean} class.
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

	private String name;

	private String group;

	private Class<?> jobClass;

	private JobDataMap jobDataMap = new JobDataMap();

	private boolean durability = false;

	private String description;

	private String beanName;

	private ApplicationContext applicationContext;

	private String applicationContextJobDataKey;

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
	@SuppressWarnings("rawtypes")
	public void setJobClass(Class jobClass) {
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
	 * @param jobDataAsMap Map with String keys and any objects as values
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
	 * Set a textual description for this job.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

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


	public void afterPropertiesSet() {
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

		/*
		JobDetailImpl jdi = new JobDetailImpl();
		jdi.setName(this.name);
		jdi.setGroup(this.group);
		jdi.setJobClass(this.jobClass);
		jdi.setJobDataMap(this.jobDataMap);
		jdi.setDurability(this.durability);
		jdi.setDescription(this.description);
		this.jobDetail = jdi;
		*/

		Class<?> jobDetailClass;
		try {
			jobDetailClass = getClass().getClassLoader().loadClass("org.quartz.impl.JobDetailImpl");
		}
		catch (ClassNotFoundException ex) {
			jobDetailClass = JobDetail.class;
		}
		BeanWrapper bw = new BeanWrapperImpl(jobDetailClass);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("name", this.name);
		pvs.add("group", this.group);
		pvs.add("jobClass", this.jobClass);
		pvs.add("jobDataMap", this.jobDataMap);
		pvs.add("durability", this.durability);
		pvs.add("description", this.description);
		bw.setPropertyValues(pvs);
		this.jobDetail = (JobDetail) bw.getWrappedInstance();
	}


	public JobDetail getObject() {
		return this.jobDetail;
	}

	public Class<?> getObjectType() {
		return JobDetail.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
