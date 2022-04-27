/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.remoting.caucho;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.caucho.hessian.client.HessianProxyFactory;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.remoting.RemoteAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 16.05.2003
 */
@SuppressWarnings("deprecation")
public class CauchoRemotingTests {

	@Test
	public void hessianProxyFactoryBeanWithClassInsteadOfInterface() throws Exception {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setServiceInterface(TestBean.class));
	}

	@Test
	public void hessianProxyFactoryBeanWithAccessError() throws Exception {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceInterface(ITestBean.class);
		factory.setServiceUrl("http://localhosta/testbean");
		factory.afterPropertiesSet();

		assertThat(factory.isSingleton()).as("Correct singleton value").isTrue();
		boolean condition = factory.getObject() instanceof ITestBean;
		assertThat(condition).isTrue();
		ITestBean bean = (ITestBean) factory.getObject();

		assertThatExceptionOfType(RemoteAccessException.class).isThrownBy(() ->
				bean.setName("test"));
	}

	@Test
	public void hessianProxyFactoryBeanWithAuthenticationAndAccessError() throws Exception {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceInterface(ITestBean.class);
		factory.setServiceUrl("http://localhosta/testbean");
		factory.setUsername("test");
		factory.setPassword("bean");
		factory.setOverloadEnabled(true);
		factory.afterPropertiesSet();

		assertThat(factory.isSingleton()).as("Correct singleton value").isTrue();
		boolean condition = factory.getObject() instanceof ITestBean;
		assertThat(condition).isTrue();
		ITestBean bean = (ITestBean) factory.getObject();

		assertThatExceptionOfType(RemoteAccessException.class).isThrownBy(() ->
				bean.setName("test"));
	}

	@Test
	public void hessianProxyFactoryBeanWithCustomProxyFactory() throws Exception {
		TestHessianProxyFactory proxyFactory = new TestHessianProxyFactory();
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceInterface(ITestBean.class);
		factory.setServiceUrl("http://localhosta/testbean");
		factory.setProxyFactory(proxyFactory);
		factory.setUsername("test");
		factory.setPassword("bean");
		factory.setOverloadEnabled(true);
		factory.afterPropertiesSet();
		assertThat(factory.isSingleton()).as("Correct singleton value").isTrue();
		boolean condition = factory.getObject() instanceof ITestBean;
		assertThat(condition).isTrue();
		ITestBean bean = (ITestBean) factory.getObject();

		assertThat(proxyFactory.user).isEqualTo("test");
		assertThat(proxyFactory.password).isEqualTo("bean");
		assertThat(proxyFactory.overloadEnabled).isTrue();

		assertThatExceptionOfType(RemoteAccessException.class).isThrownBy(() ->
				bean.setName("test"));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void simpleHessianServiceExporter() throws IOException {
		final int port = org.springframework.util.SocketUtils.findAvailableTcpPort();

		TestBean tb = new TestBean("tb");
		SimpleHessianServiceExporter exporter = new SimpleHessianServiceExporter();
		exporter.setService(tb);
		exporter.setServiceInterface(ITestBean.class);
		exporter.setDebug(true);
		exporter.prepare();

		HttpServer server = HttpServer.create(new InetSocketAddress(port), -1);
		server.createContext("/hessian", exporter);
		server.start();
		try {
			HessianClientInterceptor client = new HessianClientInterceptor();
			client.setServiceUrl("http://localhost:" + port + "/hessian");
			client.setServiceInterface(ITestBean.class);
			//client.setHessian2(true);
			client.prepare();
			ITestBean proxy = ProxyFactory.getProxy(ITestBean.class, client);
			assertThat(proxy.getName()).isEqualTo("tb");
			proxy.setName("test");
			assertThat(proxy.getName()).isEqualTo("test");
		}
		finally {
			server.stop(Integer.MAX_VALUE);
		}
	}


	private static class TestHessianProxyFactory extends HessianProxyFactory {

		private String user;
		private String password;
		private boolean overloadEnabled;

		@Override
		public void setUser(String user) {
			this.user = user;
		}

		@Override
		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public void setOverloadEnabled(boolean overloadEnabled) {
			this.overloadEnabled = overloadEnabled;
		}
	}

}
