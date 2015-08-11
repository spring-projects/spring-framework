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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Arjen Poutsma
 */
public class CountingHttpHandler implements HttpHandler {

	private static final Log logger = LogFactory.getLog(CountingHttpHandler.class);

	@Override
	public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		request.getBody().subscribe(new Subscriber<byte[]>() {
			private Subscription subscription;

			private int byteCount = 0;

			@Override
			public void onSubscribe(Subscription s) {
				this.subscription = s;
				this.subscription.request(1);
			}

			@Override
			public void onNext(byte[] bytes) {
				byteCount += bytes.length;
				this.subscription.request(1);
			}

			@Override
			public void onError(Throwable t) {
				logger.error("CountingHttpHandler Error", t);
			}

			@Override
			public void onComplete() {
				logger.info("Processed " + byteCount + " bytes");
			}
		});
		return null;
	}
}
