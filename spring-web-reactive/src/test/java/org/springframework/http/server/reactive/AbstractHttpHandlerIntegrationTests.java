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

package org.springframework.http.server.reactive;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.http.server.reactive.boot.HttpServer;
import org.springframework.http.server.reactive.boot.JettyHttpServer;
import org.springframework.http.server.reactive.boot.ReactorHttpServer;
import org.springframework.http.server.reactive.boot.RxNettyHttpServer;
import org.springframework.http.server.reactive.boot.TomcatHttpServer;
import org.springframework.http.server.reactive.boot.UndertowHttpServer;
import org.springframework.util.SocketUtils;


@RunWith(Parameterized.class)
public abstract class AbstractHttpHandlerIntegrationTests {

	protected int port;

	@Parameterized.Parameter(0)
	public HttpServer server;


	@Parameterized.Parameters(name = "server [{0}]")
	public static Object[][] arguments() {
		return new Object[][] {
				{new JettyHttpServer()},
				{new RxNettyHttpServer()},
//				{new ReactorHttpServer()},
				{new TomcatHttpServer()},
				{new UndertowHttpServer()}
		};
	}


	@Before
	public void setup() throws Exception {
		this.port = SocketUtils.findAvailableTcpPort();
		this.server.setPort(this.port);
		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();
	}

	protected abstract HttpHandler createHttpHandler();

	@After
	public void tearDown() throws Exception {
		this.server.stop();
	}

}
