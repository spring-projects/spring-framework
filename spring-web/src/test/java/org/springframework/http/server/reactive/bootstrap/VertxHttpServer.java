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

package org.springframework.http.server.reactive.bootstrap;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

import org.springframework.http.server.reactive.VertxHttpHandlerAdapter;

/**
 * @author Yevhenii Melnyk
 */
public class VertxHttpServer extends HttpServerSupport implements HttpServer {


	private Vertx vertx;

	private VertxHttpHandlerAdapter handler;

	private AtomicBoolean running = new AtomicBoolean();


	@Override
	public void afterPropertiesSet() throws Exception {
		vertx = Vertx.vertx();
		handler = new VertxHttpHandlerAdapter(getHttpHandler());
	}

	@Override
	public void start() {
		if (!running.get()) {
			vertx.deployVerticle(new ReactiveVerticle(), res -> {
				if (res.succeeded()) {
					running.set(true);
				}
			});
		}
	}

	@Override
	public void stop() {
		if (running.get()) {
			vertx.close(res -> {
				if (res.succeeded()) {
					running.set(false);
				}
			});
		}
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}


	private class ReactiveVerticle extends AbstractVerticle {
		@Override
		public void start() throws Exception {
			vertx.createHttpServer().
					requestHandler(request -> handler.apply(request).subscribe())
					.listen(getPort(), getHost());
		}
	}

}
