/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.event;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;

/**
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 */
class CapturingSynchronizationCallback implements TransactionalApplicationListener.SynchronizationCallback {

	@Nullable ApplicationEvent preEvent;

	@Nullable ApplicationEvent postEvent;

	@Nullable Throwable ex;

	@Override
	public void preProcessEvent(ApplicationEvent event) {
		this.preEvent = event;
	}

	@Override
	public void postProcessEvent(ApplicationEvent event, @Nullable Throwable ex) {
		this.postEvent = event;
		this.ex = ex;
	}

}
