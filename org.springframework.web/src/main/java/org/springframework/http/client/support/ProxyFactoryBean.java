/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.client.support;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} that creates a {@link Proxy java.net.Proxy}.
 *
 * @author Arjen Poutsma
 * @since 3.0.4
 * @see	FactoryBean
 * @see Proxy
 */
public class ProxyFactoryBean implements FactoryBean<Proxy>, InitializingBean {

	private Proxy.Type type = Proxy.Type.HTTP;

	private String hostname;

	private int port = -1;

	private Proxy proxy;

	/**
	 * Sets the proxy type. Defaults to {@link Proxy.Type#HTTP}.
	 */
	public void setType(Proxy.Type type) {
		this.type = type;
	}

	/**
	 * Sets the proxy host name.
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	/**
	 * Sets the proxy port.
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public void afterPropertiesSet() throws IllegalArgumentException {
		Assert.notNull(type, "'type' must not be null");
		Assert.hasLength(hostname, "'hostname' must not be empty");
		Assert.isTrue(port >= 0 && port <= 65535, "'port' out of range: " + port);

		SocketAddress socketAddress = new InetSocketAddress(hostname, port);
		this.proxy = new Proxy(type, socketAddress);

	}

	public Proxy getObject() {
		return proxy;
	}

	public Class<?> getObjectType() {
		return Proxy.class;
	}

	public boolean isSingleton() {
		return true;
	}
}
