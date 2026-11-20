/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.springframework.util.Assert;

/**
 * Simple Quartz {@link org.quartz.Job} adapter that delegates to a
 * given {@link java.lang.Runnable} instance.
 *
 * <p>Typically used in combination with property injection on the
 * Runnable instance, receiving parameters from the Quartz JobDataMap
 * that way instead of via the JobExecutionContext.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see SpringBeanJobFactory
 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
 */
public class DelegatingJob implements Job {

	private final Runnable delegate;


	/**
	 * Create a new DelegatingJob.
	 * @param delegate the Runnable implementation to delegate to
	 */
	public DelegatingJob(Runnable delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Return the wrapped Runnable implementation.
	 */
	public final Runnable getDelegate() {
		return this.delegate;
	}


	/**
	 * Delegates execution to the underlying Runnable.
	 */
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		this.delegate.run();
	}

}
