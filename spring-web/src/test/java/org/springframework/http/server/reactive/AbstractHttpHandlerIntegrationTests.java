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

package org.springframework.http.server.reactive;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.http.server.reactive.bootstrap.UndertowHttpServer;

@RunWith(Parameterized.class)
public abstract class AbstractHttpHandlerIntegrationTests {

	protected Log logger = LogFactory.getLog(getClass());

	protected int port;

	@Parameterized.Parameter(0)
	public HttpServer server;


	@Parameterized.Parameters(name = "server [{0}]")
	public static Object[][] arguments() {
		File base = new File(System.getProperty("java.io.tmpdir"));
		return new Object[][] {
				{new JettyHttpServer()},
				{new ReactorHttpServer()},
				{new TomcatHttpServer(base.getAbsolutePath())},
				{new UndertowHttpServer()}
		};
	}


	@Before
	public void setup() throws Exception {
		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();

		// Set dynamically chosen port
		this.port = this.server.getPort();
	}

	@After
	public void tearDown() throws Exception {
		this.server.stop();
		this.port = 0;
	}


	protected abstract HttpHandler createHttpHandler();

}
