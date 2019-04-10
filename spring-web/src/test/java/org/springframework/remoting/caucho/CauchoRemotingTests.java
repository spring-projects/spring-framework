/*
 * Copyright 2002-2016 the original author or authors.
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 16.05.2003
 */
public class CauchoRemotingTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void hessianProxyFactoryBeanWithClassInsteadOfInterface() throws Exception {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		exception.expect(IllegalArgumentException.class);
		factory.setServiceInterface(TestBean.class);
	}

	@Test
	public void hessianProxyFactoryBeanWithAccessError() throws Exception {
		HessianProxyFactoryBean factory = new HessianProxyFactoryBean();
		factory.setServiceInterface(ITestBean.class);
		factory.setServiceUrl("http://localhosta/testbean");
		factory.afterPropertiesSet();

		assertTrue("Correct singleton value", factory.isSingleton());
		assertTrue(factory.getObject() instanceof ITestBean);
		ITestBean bean = (ITestBean) factory.getObject();

		exception.expect(RemoteAccessException.class);
		bean.setName("test");
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

		assertTrue("Correct singleton value", factory.isSingleton());
		assertTrue(factory.getObject() instanceof ITestBean);
		ITestBean bean = (ITestBean) factory.getObject();

		exception.expect(RemoteAccessException.class);
		bean.setName("test");
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
		assertTrue("Correct singleton value", factory.isSingleton());
		assertTrue(factory.getObject() instanceof ITestBean);
		ITestBean bean = (ITestBean) factory.getObject();

		assertEquals("test", proxyFactory.user);
		assertEquals("bean", proxyFactory.password);
		assertTrue(proxyFactory.overloadEnabled);

		exception.expect(RemoteAccessException.class);
		bean.setName("test");
	}

	@Test
	public void simpleHessianServiceExporter() throws IOException {
		final int port = SocketUtils.findAvailableTcpPort();

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
			assertEquals("tb", proxy.getName());
			proxy.setName("test");
			assertEquals("test", proxy.getName());
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
