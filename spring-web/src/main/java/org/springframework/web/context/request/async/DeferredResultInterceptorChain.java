/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request.async;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.web.context.request.NativeWebRequest;

/**
 * Assists with the invocation of {@link DeferredResultProcessingInterceptor}'s.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class DeferredResultInterceptorChain {

	private static final Log logger = LogFactory.getLog(DeferredResultInterceptorChain.class);

	private final List<DeferredResultProcessingInterceptor> interceptors;

	private int preProcessingIndex = -1;


	public DeferredResultInterceptorChain(List<DeferredResultProcessingInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public void applyBeforeConcurrentHandling(NativeWebRequest request, DeferredResult<?> deferredResult)
			throws Exception {

		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			interceptor.beforeConcurrentHandling(request, deferredResult);
		}
	}

	public void applyPreProcess(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			interceptor.preProcess(request, deferredResult);
			this.preProcessingIndex++;
		}
	}

	public @Nullable Object applyPostProcess(NativeWebRequest request, DeferredResult<?> deferredResult,
			@Nullable Object concurrentResult) {

		try {
			for (int i = this.preProcessingIndex; i >= 0; i--) {
				this.interceptors.get(i).postProcess(request, deferredResult, concurrentResult);
			}
		}
		catch (Throwable ex) {
			return ex;
		}
		return concurrentResult;
	}

	public void triggerAfterTimeout(NativeWebRequest request, DeferredResult<?> deferredResult) throws Exception {
		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			if (deferredResult.isSetOrExpired()) {
				return;
			}
			if (!interceptor.handleTimeout(request, deferredResult)){
				break;
			}
		}
	}

	/**
	 * Determine if further error handling should be bypassed.
	 * @return {@code true} to continue error handling, or false to bypass any further
	 * error handling
	 */
	public boolean triggerAfterError(NativeWebRequest request, DeferredResult<?> deferredResult, Throwable ex)
			throws Exception {

		for (DeferredResultProcessingInterceptor interceptor : this.interceptors) {
			if (deferredResult.isSetOrExpired()) {
				return false;
			}
			if (!interceptor.handleError(request, deferredResult, ex)){
				return false;
			}
		}
		return true;
	}

	public void triggerAfterCompletion(NativeWebRequest request, DeferredResult<?> deferredResult) {
		for (int i = this.preProcessingIndex; i >= 0; i--) {
			try {
				this.interceptors.get(i).afterCompletion(request, deferredResult);
			}
			catch (Throwable ex) {
				logger.trace("Ignoring failure in afterCompletion method", ex);
			}
		}
	}

}
