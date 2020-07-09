/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
public class ProxyFactoryBeanTests {

	ProxyFactoryBean factoryBean;

	@BeforeEach
	public void setUp() {
		factoryBean = new ProxyFactoryBean();
	}

	@Test
	public void noType() {
		factoryBean.setType(null);
		assertThatIllegalArgumentException().isThrownBy(
				factoryBean::afterPropertiesSet);
	}

	@Test
	public void noHostname() {
		factoryBean.setHostname("");
		assertThatIllegalArgumentException().isThrownBy(
				factoryBean::afterPropertiesSet);
	}

	@Test
	public void noPort() {
		factoryBean.setHostname("example.com");
		assertThatIllegalArgumentException().isThrownBy(
				factoryBean::afterPropertiesSet);
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

		assertThat(result.type()).isEqualTo(type);
		InetSocketAddress address = (InetSocketAddress) result.address();
		assertThat(address.getHostName()).isEqualTo(hostname);
		assertThat(address.getPort()).isEqualTo(port);
	}

}
