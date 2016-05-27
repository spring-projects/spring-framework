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

package org.springframework.http.server.reactive.boot;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.SocketUtils;

/**
 * @author Rossen Stoyanchev
 */
public class HttpServerSupport {

	private String host = "0.0.0.0";

	private int port = -1;

	private HttpHandler httpHandler;

	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		if(this.port == -1) {
			this.port = SocketUtils.findAvailableTcpPort(8080);
		}
		return this.port;
	}

	public void setHandler(HttpHandler handler) {
		this.httpHandler = handler;
	}

	public HttpHandler getHttpHandler() {
		return this.httpHandler;
	}

}
