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

package org.springframework.orm.jpa.support;

import java.util.concurrent.Callable;

import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

/**
 * An interceptor with asynchronous web requests used in OpenSessionInViewFilter and
 * OpenSessionInViewInterceptor.
 *
 * Ensures the following:
 * 1) The session is bound/unbound when "callable processing" is started
 * 2) The session is closed if an async request times out or an error occurred
 *
 * @author Rossen Stoyanchev
 * @since 3.2.5
 */
class AsyncRequestInterceptor implements CallableProcessingInterceptor, DeferredResultProcessingInterceptor {

	private static final Log logger = LogFactory.getLog(AsyncRequestInterceptor.class);

	private final EntityManagerFactory emFactory;

	private final EntityManagerHolder emHolder;

	private volatile boolean timeoutInProgress;

	private volatile boolean errorInProgress;


	public AsyncRequestInterceptor(EntityManagerFactory emFactory, EntityManagerHolder emHolder) {
		this.emFactory = emFactory;
		this.emHolder = emHolder;
	}


	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
		bindEntityManager();
	}

	public void bindEntityManager() {
		this.timeoutInProgress = false;
		this.errorInProgress = false;
		TransactionSynchronizationManager.bindResource(this.emFactory, this.emHolder);
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, @Nullable Object concurrentResult) {
		TransactionSynchronizationManager.unbindResource(this.emFactory);
	}

	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) {
		this.timeoutInProgress = true;
		return RESULT_NONE;  // give other interceptors a chance to handle the timeout
	}

	@Override
	public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) {
		this.errorInProgress = true;
		return RESULT_NONE;  // give other interceptors a chance to handle the error
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
		closeEntityManager();
	}

	private void closeEntityManager() {
		if (this.timeoutInProgress || this.errorInProgress) {
			logger.debug("Closing JPA EntityManager after async request timeout/error");
			EntityManagerFactoryUtils.closeEntityManager(this.emHolder.getEntityManager());
		}
	}


	// Implementation of DeferredResultProcessingInterceptor methods

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) {
		this.timeoutInProgress = true;
		return true;  // give other interceptors a chance to handle the timeout
	}

	@Override
	public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult, Throwable t) {
		this.errorInProgress = true;
		return true;  // give other interceptors a chance to handle the error
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) {
		closeEntityManager();
	}

}
