/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscriber;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Adapters for RxJava's {@link rx.Single} and {@link rx.Observable} to be
 * supported as return values in Spring MVC.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class RxJavaReturnValueAdapter implements ResponseBodyEmitterAdapter, DeferredResultAdapter {

	private static final MediaType SSE_MEDIA_TYPE = new MediaType("text", "event-stream");


	@Override
	public DeferredResult<?> adaptToDeferredResult(Object returnValue) {
		Assert.isInstanceOf(Single.class, returnValue);
		Single<?> single = (Single<?>) returnValue;
		DeferredResult<Object> result = createDeferredResult();
		single.subscribe(new SingleSubscriberAdapter(result));
		return result;
	}

	protected DeferredResult<Object> createDeferredResult() {
		return new DeferredResult<Object>();
	}

	@Override
	public ResponseBodyEmitter adaptToEmitter(Object returnValue, ServerHttpResponse response) {
		Assert.isInstanceOf(Observable.class, returnValue);
		ResponseBodyEmitter emitter = createEmitter(response);
		((Observable<?>) returnValue).subscribe(new SubscriberAdapter(emitter));
		return emitter;
	}

	protected ResponseBodyEmitter createEmitter(ServerHttpResponse response) {
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType != null && contentType.isCompatibleWith(SSE_MEDIA_TYPE)) {
			return new SseEmitter();
		}
		else {
			return new ResponseBodyEmitter();
		}
	}


	private static class SingleSubscriberAdapter extends SingleSubscriber<Object> implements Runnable {

		private final DeferredResult<Object> deferredResult;


		public SingleSubscriberAdapter(DeferredResult<Object> deferredResult) {
			this.deferredResult = deferredResult;
			this.deferredResult.onTimeout(this);
			this.deferredResult.onCompletion(this);
		}


		@Override
		public void onSuccess(Object value) {
			this.deferredResult.setResult(value);
		}

		@Override
		public void onError(Throwable error) {
			this.deferredResult.setErrorResult(error);
		}

		@Override
		public void run() {
			unsubscribe();
		}
	}

	private static class SubscriberAdapter extends Subscriber<Object> implements Runnable {

		private final ResponseBodyEmitter emitter;


		public SubscriberAdapter(ResponseBodyEmitter emitter) {
			this.emitter = emitter;
			this.emitter.onTimeout(this);
			this.emitter.onCompletion(this);
		}


		@Override
		public void onNext(Object value) {
			try {
				this.emitter.send(value);
			}
			catch (IOException ex) {
				unsubscribe();
			}
		}

		@Override
		public void onError(Throwable ex) {
			this.emitter.completeWithError(ex);
		}

		@Override
		public void onCompleted() {
			this.emitter.complete();
		}

		@Override
		public void run() {
			unsubscribe();
		}
	}

}
