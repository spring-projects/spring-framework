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
package org.springframework.reactive.web;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.http.MediaType;
import org.springframework.reactive.web.rxnetty.RequestHandlerAdapter;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Rossen Stoyanchev
 */
public class DispatcherApp {

	public static void main(String[] args) {

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("handlerMapping", SimpleUrlHandlerMapping.class);
		wac.registerSingleton("handlerAdapter", PlainTextHandlerAdapter.class);
		wac.registerSingleton("resultHandler", PlainTextResultHandler.class);
		wac.refresh();

		SimpleUrlHandlerMapping handlerMapping = wac.getBean(SimpleUrlHandlerMapping.class);
		handlerMapping.addHandler("/text", new HelloWorldTextHandler());

		DispatcherHttpHandler dispatcherHandler = new DispatcherHttpHandler();
		dispatcherHandler.initStrategies(wac);

		RequestHandlerAdapter requestHandler = new RequestHandlerAdapter(dispatcherHandler);
		HttpServer<ByteBuf, ByteBuf> server = HttpServer.newServer(8080);
		server.start(requestHandler::handle);
		server.awaitShutdown();
	}


	private static class SimpleUrlHandlerMapping implements HandlerMapping {

		private final Map<String, Object> handlerMap = new HashMap<>();


		public void addHandler(String path, Object handler) {
			this.handlerMap.put(path, handler);
		}

		@Override
		public Object getHandler(ServerHttpRequest request) {
			return this.handlerMap.get(request.getURI().getPath());
		}
	}

	private interface PlainTextHandler {

		Publisher<String> handle(ServerHttpRequest request, ServerHttpResponse response);

	}

	private static class HelloWorldTextHandler implements PlainTextHandler {

		@Override
		public Publisher<String> handle(ServerHttpRequest request, ServerHttpResponse response) {

			return new Publisher<String>() {

				@Override
				public void subscribe(Subscriber<? super String> subscriber) {
					subscriber.onSubscribe(new AbstractSubscription<String>(subscriber) {

						@Override
						protected void requestInternal(long n) {
							invokeOnNext("Hello world.");
							invokeOnComplete();
						}
					});
				}
			};
		}

	}

	private static class PlainTextHandlerAdapter implements HandlerAdapter {

		@Override
		public boolean supports(Object handler) {
			return PlainTextHandler.class.isAssignableFrom(handler.getClass());
		}

		@Override
		public Publisher<HandlerResult> handle(ServerHttpRequest request, ServerHttpResponse response,
				Object handler) {

			PlainTextHandler textHandler = (PlainTextHandler) handler;
			final Publisher<String> resultPublisher = textHandler.handle(request, response);

			return new Publisher<HandlerResult>() {

				@Override
				public void subscribe(Subscriber<? super HandlerResult> handlerResultSubscriber) {
					handlerResultSubscriber.onSubscribe(new AbstractSubscription<HandlerResult>(handlerResultSubscriber) {

						@Override
						protected void requestInternal(long n) {
							resultPublisher.subscribe(new Subscriber<Object>() {

								@Override
								public void onSubscribe(Subscription subscription) {
									subscription.request(Long.MAX_VALUE);
								}

								@Override
								public void onNext(Object result) {
									invokeOnNext(new HandlerResult(result));
								}

								@Override
								public void onError(Throwable error) {
									invokeOnError(error);
								}

								@Override
								public void onComplete() {
									invokeOnComplete();
								}
							});
						}
					});
				}
			};
		}
	}

	private static class PlainTextResultHandler implements HandlerResultHandler {

		@Override
		public boolean supports(HandlerResult result) {
			Object value = result.getReturnValue();
			return (value != null && String.class.equals(value.getClass()));
		}

		@Override
		public Publisher<Void> handleResult(ServerHttpRequest request, ServerHttpResponse response,
				HandlerResult result) {

			response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

			return response.writeWith(new Publisher<byte[]>() {

				@Override
				public void subscribe(Subscriber<? super byte[]> writeSubscriber) {
					writeSubscriber.onSubscribe(new AbstractSubscription<byte[]>(writeSubscriber) {

						@Override
						protected void requestInternal(long n) {
							Charset charset = Charset.forName("UTF-8");
							invokeOnNext(((String) result.getReturnValue()).getBytes(charset));
							invokeOnComplete();
						}
					});
				}
			});
		}
	}


	private static abstract class AbstractSubscription<T> implements Subscription {

		private final Subscriber<? super T> subscriber;

		private volatile boolean terminated;


		public AbstractSubscription(Subscriber<? super T> subscriber) {
			this.subscriber = subscriber;
		}

		protected boolean isTerminated() {
			return this.terminated;
		}

		@Override
		public void request(long n) {
			if (isTerminated()) {
				return;
			}
			if (n > 0) {
				requestInternal(n);
			}
		}

		protected abstract void requestInternal(long n);

		@Override
		public void cancel() {
			this.terminated = true;
		}

		protected void invokeOnNext(T data) {
			this.subscriber.onNext(data);
		}

		protected void invokeOnError(Throwable error) {
			this.terminated = true;
			this.subscriber.onError(error);
		}

		protected void invokeOnComplete() {
			this.terminated = true;
			this.subscriber.onComplete();
		}
	}

}
