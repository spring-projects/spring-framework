/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.orm.hibernate5.support;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;

import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

/**
 * An interceptor with asynchronous web requests used in OpenSessionInViewFilter and
 * OpenSessionInViewInterceptor.
 *
 * Ensures the following:
 * 1) The session is bound/unbound when "callable processing" is started
 * 2) The session is closed if an async request times out
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
class AsyncRequestInterceptor extends CallableProcessingInterceptorAdapter implements DeferredResultProcessingInterceptor {

	private static final Log logger = LogFactory.getLog(AsyncRequestInterceptor.class);

	private final SessionFactory sessionFactory;

	private final SessionHolder sessionHolder;

	private volatile boolean timeoutInProgress;


	public AsyncRequestInterceptor(SessionFactory sessionFactory, SessionHolder sessionHolder) {
		this.sessionFactory = sessionFactory;
		this.sessionHolder = sessionHolder;
	}


	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
		bindSession();
	}

	public void bindSession() {
		this.timeoutInProgress = false;
		TransactionSynchronizationManager.bindResource(this.sessionFactory, this.sessionHolder);
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
		TransactionSynchronizationManager.unbindResource(this.sessionFactory);
	}

	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) {
		this.timeoutInProgress = true;
		return RESULT_NONE;  // give other interceptors a chance to handle the timeout
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
		closeAfterTimeout();
	}

	private void closeAfterTimeout() {
		if (this.timeoutInProgress) {
			logger.debug("Closing Hibernate Session after async request timeout");
			SessionFactoryUtils.closeSession(this.sessionHolder.getSession());
		}
	}


	// Implementation of DeferredResultProcessingInterceptor methods

	@Override
	public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult) {
	}

	@Override
	public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) {
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult, Object result) {
	}

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) {
		this.timeoutInProgress = true;
		return true;  // give other interceptors a chance to handle the timeout
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) {
		closeAfterTimeout();
	}

}
