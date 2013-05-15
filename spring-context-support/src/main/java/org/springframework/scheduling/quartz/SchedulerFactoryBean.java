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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.RemoteScheduler;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.scheduling.SchedulingException;
import org.springframework.util.CollectionUtils;

/**
 * {@link FactoryBean} that creates and configures a Quartz {@link org.quartz.Scheduler},
 * manages its lifecycle as part of the Spring application context, and exposes the
 * Scheduler as bean reference for dependency injection.
 *
 * <p>Allows registration of JobDetails, Calendars and Triggers, automatically
 * starting the scheduler on initialization and shutting it down on destruction.
 * In scenarios that just require static registration of jobs at startup, there
 * is no need to access the Scheduler instance itself in application code.
 *
 * <p>For dynamic registration of jobs at runtime, use a bean reference to
 * this SchedulerFactoryBean to get direct access to the Quartz Scheduler
 * ({@code org.quartz.Scheduler}). This allows you to create new jobs
 * and triggers, and also to control and monitor the entire Scheduler.
 *
 * <p>Note that Quartz instantiates a new Job for each execution, in
 * contrast to Timer which uses a TimerTask instance that is shared
 * between repeated executions. Just JobDetail descriptors are shared.
 *
 * <p>When using persistent jobs, it is strongly recommended to perform all
 * operations on the Scheduler within Spring-managed (or plain JTA) transactions.
 * Else, database locking will not properly work and might even break.
 * (See {@link #setDataSource setDataSource} javadoc for details.)
 *
 * <p>The preferred way to achieve transactional execution is to demarcate
 * declarative transactions at the business facade level, which will
 * automatically apply to Scheduler operations performed within those scopes.
 * Alternatively, you may add transactional advice for the Scheduler itself.
 *
 * <p>Compatible with Quartz 1.8 as well as Quartz 2.0/2.1, as of Spring 4.0.
 *
 * @author Juergen Hoeller
 * @since 18.02.2004
 * @see #setDataSource
 * @see org.quartz.Scheduler
 * @see org.quartz.SchedulerFactory
 * @see org.quartz.impl.StdSchedulerFactory
 * @see org.springframework.transaction.interceptor.TransactionProxyFactoryBean
 */
public class SchedulerFactoryBean extends SchedulerAccessor implements FactoryBean<Scheduler>, BeanNameAware,
		ApplicationContextAware, InitializingBean, DisposableBean, SmartLifecycle {

	public static final String PROP_THREAD_COUNT = "org.quartz.threadPool.threadCount";

	public static final int DEFAULT_THREAD_COUNT = 10;


	private static final ThreadLocal<ResourceLoader> configTimeResourceLoaderHolder =
			new ThreadLocal<ResourceLoader>();

	private static final ThreadLocal<Executor> configTimeTaskExecutorHolder =
			new ThreadLocal<Executor>();

	private static final ThreadLocal<DataSource> configTimeDataSourceHolder =
			new ThreadLocal<DataSource>();

	private static final ThreadLocal<DataSource> configTimeNonTransactionalDataSourceHolder =
			new ThreadLocal<DataSource>();

	/**
	 * Return the ResourceLoader for the currently configured Quartz Scheduler,
	 * to be used by ResourceLoaderClassLoadHelper.
	 * <p>This instance will be set before initialization of the corresponding
	 * Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setApplicationContext
	 * @see ResourceLoaderClassLoadHelper
	 */
	public static ResourceLoader getConfigTimeResourceLoader() {
		return configTimeResourceLoaderHolder.get();
	}

	/**
	 * Return the TaskExecutor for the currently configured Quartz Scheduler,
	 * to be used by LocalTaskExecutorThreadPool.
	 * <p>This instance will be set before initialization of the corresponding
	 * Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setTaskExecutor
	 * @see LocalTaskExecutorThreadPool
	 */
	public static Executor getConfigTimeTaskExecutor() {
		return configTimeTaskExecutorHolder.get();
	}

	/**
	 * Return the DataSource for the currently configured Quartz Scheduler,
	 * to be used by LocalDataSourceJobStore.
	 * <p>This instance will be set before initialization of the corresponding
	 * Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	public static DataSource getConfigTimeDataSource() {
		return configTimeDataSourceHolder.get();
	}

	/**
	 * Return the non-transactional DataSource for the currently configured
	 * Quartz Scheduler, to be used by LocalDataSourceJobStore.
	 * <p>This instance will be set before initialization of the corresponding
	 * Scheduler, and reset immediately afterwards. It is thus only available
	 * during configuration.
	 * @see #setNonTransactionalDataSource
	 * @see LocalDataSourceJobStore
	 */
	public static DataSource getConfigTimeNonTransactionalDataSource() {
		return configTimeNonTransactionalDataSourceHolder.get();
	}


	private Class<?> schedulerFactoryClass = StdSchedulerFactory.class;

	private String schedulerName;

	private Resource configLocation;

	private Properties quartzProperties;


	private Executor taskExecutor;

	private DataSource dataSource;

	private DataSource nonTransactionalDataSource;


    private Map schedulerContextMap;

	private ApplicationContext applicationContext;

	private String applicationContextSchedulerContextKey;

	private JobFactory jobFactory;

	private boolean jobFactorySet = false;


	private boolean autoStartup = true;

	private int startupDelay = 0;

	private int phase = Integer.MAX_VALUE;

	private boolean exposeSchedulerInRepository = false;

	private boolean waitForJobsToCompleteOnShutdown = false;


	private Scheduler scheduler;


	/**
	 * Set the Quartz SchedulerFactory implementation to use.
	 * <p>Default is StdSchedulerFactory, reading in the standard
	 * {@code quartz.properties} from {@code quartz.jar}.
	 * To use custom Quartz properties, specify the "configLocation"
	 * or "quartzProperties" bean property on this FactoryBean.
	 * @see org.quartz.impl.StdSchedulerFactory
	 * @see #setConfigLocation
	 * @see #setQuartzProperties
	 */
	public void setSchedulerFactoryClass(Class schedulerFactoryClass) {
		if (schedulerFactoryClass == null || !SchedulerFactory.class.isAssignableFrom(schedulerFactoryClass)) {
			throw new IllegalArgumentException("schedulerFactoryClass must implement [org.quartz.SchedulerFactory]");
		}
		this.schedulerFactoryClass = schedulerFactoryClass;
	}

	/**
	 * Set the name of the Scheduler to create via the SchedulerFactory.
	 * <p>If not specified, the bean name will be used as default scheduler name.
	 * @see #setBeanName
	 * @see org.quartz.SchedulerFactory#getScheduler()
	 * @see org.quartz.SchedulerFactory#getScheduler(String)
	 */
	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
	}

	/**
	 * Set the location of the Quartz properties config file, for example
	 * as classpath resource "classpath:quartz.properties".
	 * <p>Note: Can be omitted when all necessary properties are specified
	 * locally via this bean, or when relying on Quartz' default configuration.
	 * @see #setQuartzProperties
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * Set Quartz properties, like "org.quartz.threadPool.class".
	 * <p>Can be used to override values in a Quartz properties config file,
	 * or to specify all necessary properties locally.
	 * @see #setConfigLocation
	 */
	public void setQuartzProperties(Properties quartzProperties) {
		this.quartzProperties = quartzProperties;
	}


	/**
	 * Set the Spring TaskExecutor to use as Quartz backend.
	 * Exposed as thread pool through the Quartz SPI.
	 * <p>Can be used to assign a JDK 1.5 ThreadPoolExecutor or a CommonJ
	 * WorkManager as Quartz backend, to avoid Quartz's manual thread creation.
	 * <p>By default, a Quartz SimpleThreadPool will be used, configured through
	 * the corresponding Quartz properties.
	 * @see #setQuartzProperties
	 * @see LocalTaskExecutorThreadPool
	 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
	 * @see org.springframework.scheduling.commonj.WorkManagerTaskExecutor
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the default DataSource to be used by the Scheduler. If set,
	 * this will override corresponding settings in Quartz properties.
	 * <p>Note: If this is set, the Quartz settings should not define
	 * a job store "dataSource" to avoid meaningless double configuration.
	 * <p>A Spring-specific subclass of Quartz' JobStoreCMT will be used.
	 * It is therefore strongly recommended to perform all operations on
	 * the Scheduler within Spring-managed (or plain JTA) transactions.
	 * Else, database locking will not properly work and might even break
	 * (e.g. if trying to obtain a lock on Oracle without a transaction).
	 * <p>Supports both transactional and non-transactional DataSource access.
	 * With a non-XA DataSource and local Spring transactions, a single DataSource
	 * argument is sufficient. In case of an XA DataSource and global JTA transactions,
	 * SchedulerFactoryBean's "nonTransactionalDataSource" property should be set,
	 * passing in a non-XA DataSource that will not participate in global transactions.
	 * @see #setNonTransactionalDataSource
	 * @see #setQuartzProperties
	 * @see #setTransactionManager
	 * @see LocalDataSourceJobStore
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Set the DataSource to be used by the Scheduler <i>for non-transactional access</i>.
	 * <p>This is only necessary if the default DataSource is an XA DataSource that will
	 * always participate in transactions: A non-XA version of that DataSource should
	 * be specified as "nonTransactionalDataSource" in such a scenario.
	 * <p>This is not relevant with a local DataSource instance and Spring transactions.
	 * Specifying a single default DataSource as "dataSource" is sufficient there.
	 * @see #setDataSource
	 * @see LocalDataSourceJobStore
	 */
	public void setNonTransactionalDataSource(DataSource nonTransactionalDataSource) {
		this.nonTransactionalDataSource = nonTransactionalDataSource;
	}


	/**
	 * Register objects in the Scheduler context via a given Map.
	 * These objects will be available to any Job that runs in this Scheduler.
	 * <p>Note: When using persistent Jobs whose JobDetail will be kept in the
	 * database, do not put Spring-managed beans or an ApplicationContext
	 * reference into the JobDataMap but rather into the SchedulerContext.
	 * @param schedulerContextAsMap Map with String keys and any objects as
	 * values (for example Spring-managed beans)
	 * @see JobDetailBean#setJobDataAsMap
	 */
	public void setSchedulerContextAsMap(Map schedulerContextAsMap) {
		this.schedulerContextMap = schedulerContextAsMap;
	}

	/**
	 * Set the key of an ApplicationContext reference to expose in the
	 * SchedulerContext, for example "applicationContext". Default is none.
	 * Only applicable when running in a Spring ApplicationContext.
	 * <p>Note: When using persistent Jobs whose JobDetail will be kept in the
	 * database, do not put an ApplicationContext reference into the JobDataMap
	 * but rather into the SchedulerContext.
	 * <p>In case of a QuartzJobBean, the reference will be applied to the Job
	 * instance as bean property. An "applicationContext" attribute will
	 * correspond to a "setApplicationContext" method in that scenario.
	 * <p>Note that BeanFactory callback interfaces like ApplicationContextAware
	 * are not automatically applied to Quartz Job instances, because Quartz
	 * itself is reponsible for the lifecycle of its Jobs.
	 * @see JobDetailBean#setApplicationContextJobDataKey
	 * @see org.springframework.context.ApplicationContext
	 */
	public void setApplicationContextSchedulerContextKey(String applicationContextSchedulerContextKey) {
		this.applicationContextSchedulerContextKey = applicationContextSchedulerContextKey;
	}

	/**
	 * Set the Quartz JobFactory to use for this Scheduler.
	 * <p>Default is Spring's {@link AdaptableJobFactory}, which supports
	 * {@link java.lang.Runnable} objects as well as standard Quartz
	 * {@link org.quartz.Job} instances. Note that this default only applies
	 * to a <i>local</i> Scheduler, not to a RemoteScheduler (where setting
	 * a custom JobFactory is not supported by Quartz).
	 * <p>Specify an instance of Spring's {@link SpringBeanJobFactory} here
	 * (typically as an inner bean definition) to automatically populate a job's
	 * bean properties from the specified job data map and scheduler context.
	 * @see AdaptableJobFactory
	 * @see SpringBeanJobFactory
	 */
	public void setJobFactory(JobFactory jobFactory) {
		this.jobFactory = jobFactory;
		this.jobFactorySet = true;
	}


	/**
	 * Set whether to automatically start the scheduler after initialization.
	 * <p>Default is "true"; set this to "false" to allow for manual startup.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Return whether this scheduler is configured for auto-startup. If "true",
	 * the scheduler will start after the context is refreshed and after the
	 * start delay, if any.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Specify the phase in which this scheduler should be started and
	 * stopped. The startup order proceeds from lowest to highest, and
	 * the shutdown order is the reverse of that. By default this value
	 * is Integer.MAX_VALUE meaning that this scheduler starts as late
	 * as possible and stops as soon as possible.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the phase in which this scheduler will be started and stopped.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	/**
	 * Set the number of seconds to wait after initialization before
	 * starting the scheduler asynchronously. Default is 0, meaning
	 * immediate synchronous startup on initialization of this bean.
	 * <p>Setting this to 10 or 20 seconds makes sense if no jobs
	 * should be run before the entire application has started up.
	 */
	public void setStartupDelay(int startupDelay) {
		this.startupDelay = startupDelay;
	}

	/**
	 * Set whether to expose the Spring-managed {@link Scheduler} instance in the
	 * Quartz {@link SchedulerRepository}. Default is "false", since the Spring-managed
	 * Scheduler is usually exclusively intended for access within the Spring context.
	 * <p>Switch this flag to "true" in order to expose the Scheduler globally.
	 * This is not recommended unless you have an existing Spring application that
	 * relies on this behavior. Note that such global exposure was the accidental
	 * default in earlier Spring versions; this has been fixed as of Spring 2.5.6.
	 */
	public void setExposeSchedulerInRepository(boolean exposeSchedulerInRepository) {
		this.exposeSchedulerInRepository = exposeSchedulerInRepository;
	}

	/**
	 * Set whether to wait for running jobs to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed jobs at the expense of a longer shutdown phase.
	 * @see org.quartz.Scheduler#shutdown(boolean)
	 */
	public void setWaitForJobsToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForJobsToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}


	@Override
	public void setBeanName(String name) {
		if (this.schedulerName == null) {
			this.schedulerName = name;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	//---------------------------------------------------------------------
	// Implementation of InitializingBean interface
	//---------------------------------------------------------------------

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.dataSource == null && this.nonTransactionalDataSource != null) {
			this.dataSource = this.nonTransactionalDataSource;
		}

		if (this.applicationContext != null && this.resourceLoader == null) {
			this.resourceLoader = this.applicationContext;
		}

		// Create SchedulerFactory instance.
		SchedulerFactory schedulerFactory = (SchedulerFactory)
				BeanUtils.instantiateClass(this.schedulerFactoryClass);

		initSchedulerFactory(schedulerFactory);

		if (this.resourceLoader != null) {
			// Make given ResourceLoader available for SchedulerFactory configuration.
			configTimeResourceLoaderHolder.set(this.resourceLoader);
		}
		if (this.taskExecutor != null) {
			// Make given TaskExecutor available for SchedulerFactory configuration.
			configTimeTaskExecutorHolder.set(this.taskExecutor);
		}
		if (this.dataSource != null) {
			// Make given DataSource available for SchedulerFactory configuration.
			configTimeDataSourceHolder.set(this.dataSource);
		}
		if (this.nonTransactionalDataSource != null) {
			// Make given non-transactional DataSource available for SchedulerFactory configuration.
			configTimeNonTransactionalDataSourceHolder.set(this.nonTransactionalDataSource);
		}


		// Get Scheduler instance from SchedulerFactory.
		try {
			this.scheduler = createScheduler(schedulerFactory, this.schedulerName);
			populateSchedulerContext();

			if (!this.jobFactorySet && !(this.scheduler instanceof RemoteScheduler)) {
				// Use AdaptableJobFactory as default for a local Scheduler, unless when
				// explicitly given a null value through the "jobFactory" bean property.
				this.jobFactory = new AdaptableJobFactory();
			}
			if (this.jobFactory != null) {
				if (this.jobFactory instanceof SchedulerContextAware) {
					((SchedulerContextAware) this.jobFactory).setSchedulerContext(this.scheduler.getContext());
				}
				this.scheduler.setJobFactory(this.jobFactory);
			}
		}

		finally {
			if (this.resourceLoader != null) {
				configTimeResourceLoaderHolder.remove();
			}
			if (this.taskExecutor != null) {
				configTimeTaskExecutorHolder.remove();
			}
			if (this.dataSource != null) {
				configTimeDataSourceHolder.remove();
			}
			if (this.nonTransactionalDataSource != null) {
				configTimeNonTransactionalDataSourceHolder.remove();
			}
		}

		registerListeners();
		registerJobsAndTriggers();
	}


	/**
	 * Load and/or apply Quartz properties to the given SchedulerFactory.
	 * @param schedulerFactory the SchedulerFactory to initialize
	 */
	private void initSchedulerFactory(SchedulerFactory schedulerFactory)
			throws SchedulerException, IOException {

		if (!(schedulerFactory instanceof StdSchedulerFactory)) {
			if (this.configLocation != null || this.quartzProperties != null ||
					this.taskExecutor != null || this.dataSource != null) {
				throw new IllegalArgumentException(
						"StdSchedulerFactory required for applying Quartz properties: " + schedulerFactory);
			}
			// Otherwise assume that no initialization is necessary...
			return;
		}

		Properties mergedProps = new Properties();

		if (this.resourceLoader != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_SCHED_CLASS_LOAD_HELPER_CLASS,
					ResourceLoaderClassLoadHelper.class.getName());
		}

		if (this.taskExecutor != null) {
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS,
					LocalTaskExecutorThreadPool.class.getName());
		}
		else {
			// Set necessary default properties here, as Quartz will not apply
			// its default configuration when explicitly given properties.
			mergedProps.setProperty(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
			mergedProps.setProperty(PROP_THREAD_COUNT, Integer.toString(DEFAULT_THREAD_COUNT));
		}

		if (this.configLocation != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Loading Quartz config from [" + this.configLocation + "]");
			}
			PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
		}

		CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);

		if (this.dataSource != null) {
			mergedProps.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, LocalDataSourceJobStore.class.getName());
		}

		// Make sure to set the scheduler name as configured in the Spring configuration.
		if (this.schedulerName != null) {
			mergedProps.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, this.schedulerName);
		}

		((StdSchedulerFactory) schedulerFactory).initialize(mergedProps);
	}

	/**
	 * Create the Scheduler instance for the given factory and scheduler name.
	 * Called by {@link #afterPropertiesSet}.
	 * <p>The default implementation invokes SchedulerFactory's {@code getScheduler}
	 * method. Can be overridden for custom Scheduler creation.
	 * @param schedulerFactory the factory to create the Scheduler with
	 * @param schedulerName the name of the scheduler to create
	 * @return the Scheduler instance
	 * @throws SchedulerException if thrown by Quartz methods
	 * @see #afterPropertiesSet
	 * @see org.quartz.SchedulerFactory#getScheduler
	 */
	protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName)
			throws SchedulerException {

		// Override thread context ClassLoader to work around naive Quartz ClassLoadHelper loading.
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		boolean overrideClassLoader = (this.resourceLoader != null &&
				!this.resourceLoader.getClassLoader().equals(threadContextClassLoader));
		if (overrideClassLoader) {
			currentThread.setContextClassLoader(this.resourceLoader.getClassLoader());
		}
		try {
			SchedulerRepository repository = SchedulerRepository.getInstance();
			synchronized (repository) {
				Scheduler existingScheduler = (schedulerName != null ? repository.lookup(schedulerName) : null);
				Scheduler newScheduler = schedulerFactory.getScheduler();
				if (newScheduler == existingScheduler) {
					throw new IllegalStateException("Active Scheduler of name '" + schedulerName + "' already registered " +
							"in Quartz SchedulerRepository. Cannot create a new Spring-managed Scheduler of the same name!");
				}
				if (!this.exposeSchedulerInRepository) {
					// Need to remove it in this case, since Quartz shares the Scheduler instance by default!
					SchedulerRepository.getInstance().remove(newScheduler.getSchedulerName());
				}
				return newScheduler;
			}
		}
		finally {
			if (overrideClassLoader) {
				// Reset original thread context ClassLoader.
				currentThread.setContextClassLoader(threadContextClassLoader);
			}
		}
	}

	/**
	 * Expose the specified context attributes and/or the current
	 * ApplicationContext in the Quartz SchedulerContext.
	 */
	private void populateSchedulerContext() throws SchedulerException {
		// Put specified objects into Scheduler context.
		if (this.schedulerContextMap != null) {
			this.scheduler.getContext().putAll(this.schedulerContextMap);
		}

		// Register ApplicationContext in Scheduler context.
		if (this.applicationContextSchedulerContextKey != null) {
			if (this.applicationContext == null) {
				throw new IllegalStateException(
					"SchedulerFactoryBean needs to be set up in an ApplicationContext " +
					"to be able to handle an 'applicationContextSchedulerContextKey'");
			}
			this.scheduler.getContext().put(this.applicationContextSchedulerContextKey, this.applicationContext);
		}
	}


	/**
	 * Start the Quartz Scheduler, respecting the "startupDelay" setting.
	 * @param scheduler the Scheduler to start
	 * @param startupDelay the number of seconds to wait before starting
	 * the Scheduler asynchronously
	 */
	protected void startScheduler(final Scheduler scheduler, final int startupDelay) throws SchedulerException {
		if (startupDelay <= 0) {
			logger.info("Starting Quartz Scheduler now");
			scheduler.start();
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Will start Quartz Scheduler [" + scheduler.getSchedulerName() +
						"] in " + startupDelay + " seconds");
			}
			Thread schedulerThread = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(startupDelay * 1000);
					}
					catch (InterruptedException ex) {
						// simply proceed
					}
					if (logger.isInfoEnabled()) {
						logger.info("Starting Quartz Scheduler now, after delay of " + startupDelay + " seconds");
					}
					try {
						scheduler.start();
					}
					catch (SchedulerException ex) {
						throw new SchedulingException("Could not start Quartz Scheduler after delay", ex);
					}
				}
			};
			schedulerThread.setName("Quartz Scheduler [" + scheduler.getSchedulerName() + "]");
			schedulerThread.setDaemon(true);
			schedulerThread.start();
		}
	}


	//---------------------------------------------------------------------
	// Implementation of FactoryBean interface
	//---------------------------------------------------------------------

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public Scheduler getObject() {
		return this.scheduler;
	}

	@Override
	public Class<? extends Scheduler> getObjectType() {
		return (this.scheduler != null) ? this.scheduler.getClass() : Scheduler.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				startScheduler(this.scheduler, this.startupDelay);
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not start Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				this.scheduler.standby();
			}
			catch (SchedulerException ex) {
				throw new SchedulingException("Could not stop Quartz Scheduler", ex);
			}
		}
	}

	@Override
	public void stop(Runnable callback) throws SchedulingException {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() throws SchedulingException {
		if (this.scheduler != null) {
			try {
				return !this.scheduler.isInStandbyMode();
			}
			catch (SchedulerException ex) {
				return false;
			}
		}
		return false;
	}


	//---------------------------------------------------------------------
	// Implementation of DisposableBean interface
	//---------------------------------------------------------------------

	/**
	 * Shut down the Quartz scheduler on bean factory shutdown,
	 * stopping all scheduled jobs.
	 */
	@Override
	public void destroy() throws SchedulerException {
		logger.info("Shutting down Quartz Scheduler");
		this.scheduler.shutdown(this.waitForJobsToCompleteOnShutdown);
	}

}
