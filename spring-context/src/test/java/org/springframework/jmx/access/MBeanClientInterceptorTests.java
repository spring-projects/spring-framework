/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jmx.access;

import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.net.BindException;
import java.util.HashMap;
import java.util.Map;

import javax.management.Descriptor;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jmx.AbstractMBeanServerTests;
import org.springframework.jmx.IJmxTestBean;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.AbstractReflectiveMBeanInfoAssembler;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Chris Beams
 */
class MBeanClientInterceptorTests extends AbstractMBeanServerTests {

	protected static final String OBJECT_NAME = "spring:test=proxy";

	protected JmxTestBean target;

	protected boolean runTests = true;

	@Override
	public void onSetUp() throws Exception {
		target = new JmxTestBean();
		target.setAge(100);
		target.setName("Rob Harrop");

		MBeanExporter adapter = new MBeanExporter();
		Map<String, Object> beans = new HashMap<>();
		beans.put(OBJECT_NAME, target);
		adapter.setServer(getServer());
		adapter.setBeans(beans);
		adapter.setAssembler(new ProxyTestAssembler());
		start(adapter);
	}

	protected MBeanServerConnection getServerConnection() throws Exception {
		return getServer();
	}

	protected IJmxTestBean getProxy() throws Exception {
		MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
		factory.setServer(getServerConnection());
		factory.setProxyInterface(IJmxTestBean.class);
		factory.setObjectName(OBJECT_NAME);
		factory.afterPropertiesSet();
		return (IJmxTestBean) factory.getObject();
	}

	@Test
	void proxyClassIsDifferent() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		assertThat(proxy.getClass()).as("The proxy class should be different than the base class").isNotSameAs(IJmxTestBean.class);
	}

	@Test
	void differentProxiesSameClass() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy1 = getProxy();
		IJmxTestBean proxy2 = getProxy();

		assertThat(proxy2).as("The proxies should NOT be the same").isNotSameAs(proxy1);
		assertThat(proxy2.getClass()).as("The proxy classes should be the same").isSameAs(proxy1.getClass());
	}

	@Test
	void getAttributeValue() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy1 = getProxy();
		int age = proxy1.getAge();
		assertThat(age).as("The age should be 100").isEqualTo(100);
	}

	@Test
	void setAttributeValue() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		proxy.setName("Rob Harrop");
		assertThat(target.getName()).as("The name of the bean should have been updated").isEqualTo("Rob Harrop");
	}

	@Test
	void setAttributeValueWithRuntimeException() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		assertThatIllegalArgumentException().isThrownBy(() -> proxy.setName("Juergen"));
	}

	@Test
	void setAttributeValueWithCheckedException() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		assertThatExceptionOfType(ClassNotFoundException.class).isThrownBy(() -> proxy.setName("Juergen Class"));
	}

	@Test
	void setAttributeValueWithIOException() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		assertThatIOException().isThrownBy(() -> proxy.setName("Juergen IO"));
	}

	@Test
	void setReadOnlyAttribute() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		assertThatExceptionOfType(InvalidInvocationException.class).isThrownBy(() -> proxy.setAge(900));
	}

	@Test
	void invokeNoArgs() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		long result = proxy.myOperation();
		assertThat(result).as("The operation should return 1").isEqualTo(1);
	}

	@Test
	void invokeArgs() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean proxy = getProxy();
		int result = proxy.add(1, 2);
		assertThat(result).as("The operation should return 3").isEqualTo(3);
	}

	@Test
	void invokeUnexposedMethodWithException() throws Exception {
		assumeTrue(runTests);
		IJmxTestBean bean = getProxy();
		assertThatExceptionOfType(InvalidInvocationException.class).isThrownBy(() -> bean.dontExposeMe());
	}

	@Test
	void lazyConnectionToRemote() throws Exception {
		assumeTrue(runTests);

		final int port = SocketUtils.findAvailableTcpPort();

		JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://localhost:" + port);
		JMXConnectorServer connector = JMXConnectorServerFactory.newJMXConnectorServer(url, null, getServer());

		MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
		factory.setServiceUrl(url.toString());
		factory.setProxyInterface(IJmxTestBean.class);
		factory.setObjectName(OBJECT_NAME);
		factory.setConnectOnStartup(false);
		factory.setRefreshOnConnectFailure(true);
		// should skip connection to the server
		factory.afterPropertiesSet();
		IJmxTestBean bean = (IJmxTestBean) factory.getObject();

		// now start the connector
		try {
			connector.start();
		}
		catch (BindException ex) {
			System.out.println("Skipping remainder of JMX LazyConnectionToRemote test because binding to local port ["
					+ port + "] failed: " + ex.getMessage());
			return;
		}

		// should now be able to access data via the lazy proxy
		try {
			assertThat(bean.getName()).isEqualTo("Rob Harrop");
			assertThat(bean.getAge()).isEqualTo(100);
		}
		finally {
			connector.stop();
		}
	}

	@Test
	void mxBeanAttributeAccess() throws Exception {
		MBeanClientInterceptor interceptor = new MBeanClientInterceptor();
		interceptor.setServer(ManagementFactory.getPlatformMBeanServer());
		interceptor.setObjectName("java.lang:type=Memory");
		interceptor.setManagementInterface(MemoryMXBean.class);
		MemoryMXBean proxy = ProxyFactory.getProxy(MemoryMXBean.class, interceptor);
		assertThat(proxy.getHeapMemoryUsage().getMax()).isGreaterThan(0);
	}

	@Test
	void mxBeanOperationAccess() throws Exception {
		MBeanClientInterceptor interceptor = new MBeanClientInterceptor();
		interceptor.setServer(ManagementFactory.getPlatformMBeanServer());
		interceptor.setObjectName("java.lang:type=Threading");
		ThreadMXBean proxy = ProxyFactory.getProxy(ThreadMXBean.class, interceptor);
		assertThat(proxy.getThreadInfo(Thread.currentThread().getId()).getStackTrace()).isNotNull();
	}


	private static class ProxyTestAssembler extends AbstractReflectiveMBeanInfoAssembler {

		@Override
		protected boolean includeReadAttribute(Method method, String beanKey) {
			return true;
		}

		@Override
		protected boolean includeWriteAttribute(Method method, String beanKey) {
			if ("setAge".equals(method.getName())) {
				return false;
			}
			return true;
		}

		@Override
		protected boolean includeOperation(Method method, String beanKey) {
			if ("dontExposeMe".equals(method.getName())) {
				return false;
			}
			return true;
		}

		@SuppressWarnings("unused")
		protected String getOperationDescription(Method method) {
			return method.getName();
		}

		@SuppressWarnings("unused")
		protected String getAttributeDescription(PropertyDescriptor propertyDescriptor) {
			return propertyDescriptor.getDisplayName();
		}

		@SuppressWarnings("unused")
		protected void populateAttributeDescriptor(Descriptor descriptor, Method getter, Method setter) {
		}

		@SuppressWarnings("unused")
		protected void populateOperationDescriptor(Descriptor descriptor, Method method) {
		}

		@SuppressWarnings({ "unused", "rawtypes" })
		protected String getDescription(String beanKey, Class beanClass) {
			return "";
		}

		@SuppressWarnings({ "unused", "rawtypes" })
		protected void populateMBeanDescriptor(Descriptor mbeanDescriptor, String beanKey, Class beanClass) {
		}
	}

}
