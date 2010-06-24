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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Arjen Poutsma
 */
public class ProxyFactoryBeanTest {

	ProxyFactoryBean factoryBean;

	@Before
	public void setUp() {
		factoryBean = new ProxyFactoryBean();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noType() {
		factoryBean.setType(null);
		factoryBean.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noHostname() {
		factoryBean.setHostname("");
		factoryBean.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noPort() {
		factoryBean.setHostname("example.com");
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void normal() {
		Proxy.Type type = Proxy.Type.HTTP;
		factoryBean.setType(type);
		String hostname = "example.com";
		factoryBean.setHostname(hostname);
		int port = 8080;
		factoryBean.setPort(port);
		factoryBean.afterPropertiesSet();

		Proxy result = factoryBean.getObject();

		assertEquals(type, result.type());
		InetSocketAddress address = (InetSocketAddress) result.address();
		assertEquals(hostname, address.getHostName());
		assertEquals(port, address.getPort());
	}

}
