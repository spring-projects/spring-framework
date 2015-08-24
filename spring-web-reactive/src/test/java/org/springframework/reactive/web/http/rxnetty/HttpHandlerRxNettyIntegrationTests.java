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
package org.springframework.reactive.web.http.rxnetty;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.springframework.reactive.web.http.EchoHandler;
import org.springframework.reactive.web.http.AbstractHttpHandlerIntegrationTestCase;

/**
 * @author Rossen Stoyanchev
 */
public class HttpHandlerRxNettyIntegrationTests extends AbstractHttpHandlerIntegrationTestCase {

	private static HttpServer<ByteBuf, ByteBuf> httpServer;


	@BeforeClass
	public static void startServer() throws Exception {
		RequestHandlerAdapter requestHandler = new RequestHandlerAdapter(new EchoHandler());
		httpServer = HttpServer.newServer(port);
		httpServer.start(requestHandler::handle);
	}

	@AfterClass
	public static void stopServer() throws Exception {
		httpServer.shutdown();
	}

}