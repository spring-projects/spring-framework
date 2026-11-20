/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.support;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
class ProxyFactoryBeanTests {

	private final ProxyFactoryBean factoryBean = new ProxyFactoryBean();


	@Test
	void noType() {
		factoryBean.setType(null);
		assertThatIllegalArgumentException().isThrownBy(factoryBean::afterPropertiesSet);
	}

	@Test
	void noHostname() {
		factoryBean.setHostname("");
		assertThatIllegalArgumentException().isThrownBy(factoryBean::afterPropertiesSet);
	}

	@Test
	void noPort() {
		factoryBean.setHostname("example.com");
		assertThatIllegalArgumentException().isThrownBy(factoryBean::afterPropertiesSet);
	}

	@Test
	void normal() {
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
