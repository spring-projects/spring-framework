/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.remoting.jaxrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.ServiceFactory;
import javax.xml.rpc.Stub;

import junit.framework.TestCase;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteLookupFailureException;

import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 18.12.2003
 */
@Deprecated
public class JaxRpcSupportTests extends TestCase {

	public void testLocalJaxRpcServiceFactoryBeanWithServiceNameAndNamespace() throws Exception {
		LocalJaxRpcServiceFactoryBean factory = new LocalJaxRpcServiceFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.afterPropertiesSet();
		assertEquals(MockServiceFactory.service1, factory.getObject());
	}

	public void testLocalJaxRpcServiceFactoryBeanWithServiceNameAndWsdl() throws Exception {
		LocalJaxRpcServiceFactoryBean factory = new LocalJaxRpcServiceFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setServiceName("myService2");
		factory.setWsdlDocumentUrl(new URL("http://myUrl1"));
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());
		assertEquals(MockServiceFactory.service2, factory.getObject());
	}

	public void testLocalJaxRpcServiceFactoryBeanWithServiceNameAndWsdlAndProperties() throws Exception {
		LocalJaxRpcServiceFactoryBean factory = new LocalJaxRpcServiceFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setServiceName("myService2");
		factory.setWsdlDocumentUrl(new URL("http://myUrl1"));
		Properties props = new Properties();
		props.setProperty("myKey", "myValue");
		factory.setJaxRpcServiceProperties(props);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());
		assertEquals(MockServiceFactory.service1, factory.getObject());
	}

	public void testLocalJaxRpcServiceFactoryBeanWithJaxRpcServiceInterface() throws Exception {
		LocalJaxRpcServiceFactoryBean factory = new LocalJaxRpcServiceFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setJaxRpcServiceInterface(IRemoteBean.class);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());
		assertEquals(MockServiceFactory.service2, factory.getObject());
	}

	public void testLocalJaxRpcServiceFactoryBeanWithJaxRpcServiceInterfaceAndWsdl() throws Exception {
		LocalJaxRpcServiceFactoryBean factory = new LocalJaxRpcServiceFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setWsdlDocumentUrl(new URL("http://myUrl1"));
		factory.setJaxRpcServiceInterface(IRemoteBean.class);
		Properties props = new Properties();
		props.setProperty("myKey", "myValue");
		factory.setJaxRpcServiceProperties(props);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());
		assertEquals(MockServiceFactory.service1, factory.getObject());
	}

	public void testJaxRpcPortProxyFactoryBean() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setPortInterface(IRemoteBean.class);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());
		assertTrue(factory.getPortStub() instanceof Stub);

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
	}

	public void testJaxRpcPortProxyFactoryBeanWithProperties() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setUsername("user");
		factory.setPassword("pw");
		factory.setEndpointAddress("ea");
		factory.setMaintainSession(true);
		factory.setPortInterface(IRemoteBean.class);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());

		assertTrue(factory.getPortStub() instanceof Stub);
		Stub stub = (Stub) factory.getPortStub();
		assertEquals("user", stub._getProperty(Stub.USERNAME_PROPERTY));
		assertEquals("pw", stub._getProperty(Stub.PASSWORD_PROPERTY));
		assertEquals("ea", stub._getProperty(Stub.ENDPOINT_ADDRESS_PROPERTY));
		assertTrue(((Boolean) stub._getProperty(Stub.SESSION_MAINTAIN_PROPERTY)).booleanValue());

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
	}

	public void testJaxRpcPortProxyFactoryBeanWithCustomProperties() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setUsername("user");
		factory.setPassword("pw");
		Properties customProps = new Properties();
		customProps.setProperty("myProp", "myValue");
		factory.setCustomProperties(customProps);
		factory.setPortInterface(IRemoteBean.class);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());

		assertTrue(factory.getPortStub() instanceof Stub);
		Stub stub = (Stub) factory.getPortStub();
		assertEquals("user", stub._getProperty(Stub.USERNAME_PROPERTY));
		assertEquals("pw", stub._getProperty(Stub.PASSWORD_PROPERTY));
		assertEquals("myValue", stub._getProperty("myProp"));

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
	}

	public void testJaxRpcPortProxyFactoryBeanWithCustomPropertyMap() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setEndpointAddress("ea");
		factory.setMaintainSession(true);
		Map customProps = new HashMap();
		customProps.put("myProp", new Integer(1));
		factory.setCustomPropertyMap(customProps);
		factory.addCustomProperty("myOtherProp", "myOtherValue");
		factory.setPortInterface(IRemoteBean.class);
		factory.afterPropertiesSet();
		assertTrue("Correct singleton value", factory.isSingleton());

		assertTrue(factory.getPortStub() instanceof Stub);
		Stub stub = (Stub) factory.getPortStub();
		assertEquals("ea", stub._getProperty(Stub.ENDPOINT_ADDRESS_PROPERTY));
		assertTrue(((Boolean) stub._getProperty(Stub.SESSION_MAINTAIN_PROPERTY)).booleanValue());
		assertEquals(new Integer(1), stub._getProperty("myProp"));
		assertEquals("myOtherValue", stub._getProperty("myOtherProp"));

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
	}

	public void testJaxRpcPortProxyFactoryBeanWithDynamicCalls() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(CallMockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setServiceInterface(IBusinessBean.class);
		factory.afterPropertiesSet();
		assertNull(factory.getPortStub());

		assertTrue(factory.getObject() instanceof IBusinessBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		proxy.setName("myName");
	}

	public void testJaxRpcPortProxyFactoryBeanWithDynamicCallsAndProperties() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(CallMockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setUsername("user");
		factory.setPassword("pw");
		factory.setEndpointAddress("ea");
		factory.setMaintainSession(true);
		factory.setServiceInterface(IBusinessBean.class);
		factory.afterPropertiesSet();
		assertNull(factory.getPortStub());

		assertTrue(factory.getObject() instanceof IBusinessBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		proxy.setName("myName");

		verify(CallMockServiceFactory.call1).setProperty(Call.USERNAME_PROPERTY, "user");
		verify(CallMockServiceFactory.call1).setProperty(Call.PASSWORD_PROPERTY, "pw");
		verify(CallMockServiceFactory.call1).setTargetEndpointAddress("ea");
		verify(CallMockServiceFactory.call1).setProperty(Call.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
	}

	public void testJaxRpcPortProxyFactoryBeanWithDynamicCallsAndServiceException() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(CallMockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myServiceX");
		factory.setPortName("myPort");
		factory.setServiceInterface(IRemoteBean.class);
		try {
			factory.afterPropertiesSet();
			fail("Should have thrown RemoteLookupFailureException");
		}
		catch (RemoteLookupFailureException ex) {
			// expected
		}
	}

	public void testJaxRpcPortProxyFactoryBeanWithDynamicCallsAndLazyLookupAndServiceException() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(CallMockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myServiceX");
		factory.setPortName("myPort");
		factory.setServiceInterface(IRemoteBean.class);
		factory.setLookupServiceOnStartup(false);
		factory.afterPropertiesSet();

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		try {
			proxy.setName("exception");
			fail("Should have thrown RemoteException");
		}
		catch (RemoteLookupFailureException ex) {
			// expected
			assertTrue(ex.getCause() instanceof ServiceException);
		}
	}

	public void testJaxRpcPortProxyFactoryBeanWithDynamicCallsAndRemoteException_() throws Exception {
		ExceptionCallMockServiceFactory serviceFactory = new ExceptionCallMockServiceFactory();

		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactory(serviceFactory);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setServiceInterface(IRemoteBean.class);
		factory.afterPropertiesSet();

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();

		try {
			proxy.setName("exception");
			fail("Should have thrown RemoteException");
		}
		catch (RemoteException ex) {
			// expected
		}

		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);

		assertEquals(1, serviceFactory.serviceCount);
	}

	public void testJaxRpcPortProxyFactoryBeanWithDynamicCallsAndRemoteExceptionAndRefresh() throws Exception {
		ExceptionCallMockServiceFactory serviceFactory = new ExceptionCallMockServiceFactory();

		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactory(serviceFactory);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setServiceInterface(IRemoteBean.class);
		factory.setRefreshServiceAfterConnectFailure(true);
		factory.afterPropertiesSet();

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();

		try {
			proxy.setName("exception");
			fail("Should have thrown RemoteException");
		}
		catch (RemoteException ex) {
			// expected
		}

		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);

		assertEquals(2, serviceFactory.serviceCount);
	}

	public void testJaxRpcPortProxyFactoryBeanWithPortInterface() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setPortInterface(IRemoteBean.class);
		factory.setServiceInterface(IBusinessBean.class);
		factory.afterPropertiesSet();
		assertTrue(factory.getObject() instanceof IBusinessBean);
		assertFalse(factory.getObject() instanceof IRemoteBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
	}

	public void testJaxRpcPortProxyFactoryBeanWithPortInterfaceAndServiceException() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myServiceX");
		factory.setPortInterface(IRemoteBean.class);
		factory.setPortName("myPort");
		factory.setServiceInterface(IRemoteBean.class);
		try {
			factory.afterPropertiesSet();
			fail("Should have thrown RemoteLookupFailureException");
		}
		catch (RemoteLookupFailureException ex) {
			// expected
		}
	}

	public void testJaxRpcPortProxyFactoryBeanWithPortInterfaceAndLazyLookupAndServiceException() throws Exception {
		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactoryClass(MockServiceFactory.class);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myServiceX");
		factory.setPortName("myPort");
		factory.setPortInterface(IRemoteBean.class);
		factory.setServiceInterface(IRemoteBean.class);
		factory.setLookupServiceOnStartup(false);
		factory.afterPropertiesSet();

		assertTrue(factory.getObject() instanceof IRemoteBean);
		IRemoteBean proxy = (IRemoteBean) factory.getObject();
		try {
			proxy.setName("exception");
			fail("Should have thrown Service");
		}
		catch (RemoteLookupFailureException ex) {
			// expected
			assertTrue(ex.getCause() instanceof ServiceException);
		}
	}

	public void testJaxRpcPortProxyFactoryBeanWithPortInterfaceAndRemoteException() throws Exception {
		MockServiceFactory serviceFactory = new MockServiceFactory();

		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactory(serviceFactory);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setPortInterface(IRemoteBean.class);
		factory.setServiceInterface(IBusinessBean.class);
		factory.afterPropertiesSet();

		assertTrue(factory.getObject() instanceof IBusinessBean);
		assertFalse(factory.getObject() instanceof IRemoteBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();

		try {
			proxy.setName("exception");
			fail("Should have thrown RemoteAccessException");
		}
		catch (RemoteAccessException ex) {
			// expected
		}

		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);

		assertEquals(1, serviceFactory.serviceCount);
	}

	public void testJaxRpcPortProxyFactoryBeanWithPortInterfaceAndRemoteExceptionAndRefresh() throws Exception {
		ExceptionMockServiceFactory serviceFactory = new ExceptionMockServiceFactory();

		JaxRpcPortProxyFactoryBean factory = new JaxRpcPortProxyFactoryBean();
		factory.setServiceFactory(serviceFactory);
		factory.setNamespaceUri("myNamespace");
		factory.setServiceName("myService1");
		factory.setPortName("myPort");
		factory.setPortInterface(IRemoteBean.class);
		factory.setServiceInterface(IBusinessBean.class);
		factory.setRefreshServiceAfterConnectFailure(true);
		factory.afterPropertiesSet();

		assertTrue(factory.getObject() instanceof IBusinessBean);
		assertFalse(factory.getObject() instanceof IRemoteBean);
		IBusinessBean proxy = (IBusinessBean) factory.getObject();

		try {
			proxy.setName("exception");
			fail("Should have thrown RemoteAccessException");
		}
		catch (RemoteAccessException ex) {
			// expected
		}

		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);
		proxy.setName("myName");
		assertEquals("myName", RemoteBean.name);

		assertEquals(2, serviceFactory.serviceCount);
	}


	public static class MockServiceFactory extends ServiceFactory {

		protected static Service service1;
		protected static Service service2;
		protected int serviceCount = 0;

		public MockServiceFactory() throws Exception {
			service1 = mock(Service.class);
			service2 = mock(Service.class);
			initMocks();
		}

		protected void initMocks() throws Exception {
			RemoteBean remoteBean = new RemoteBean();
			given(service1.getPort(new QName("myNamespace", "myPort"),
					IRemoteBean.class)).willReturn(remoteBean);
		}

		@Override
		public Service createService(QName qName) throws ServiceException {
			if (!"myNamespace".equals(qName.getNamespaceURI()) || !"myService1".equals(qName.getLocalPart())) {
				throw new ServiceException("not supported");
			}
			serviceCount++;
			return service1;
		}

		@Override
		public Service createService(URL url, QName qName) throws ServiceException {
			try {
				if (!(new URL("http://myUrl1")).equals(url) || !"".equals(qName.getNamespaceURI()) ||
						!"myService2".equals(qName.getLocalPart())) {
					throw new ServiceException("not supported");
				}
			}
			catch (MalformedURLException ex) {
			}
			serviceCount++;
			return service2;
		}

		@Override
		public Service loadService(URL url, QName qName, Properties props) throws ServiceException {
			try {
				if (!(new URL("http://myUrl1")).equals(url) || !"".equals(qName.getNamespaceURI()) ||
						!"myService2".equals(qName.getLocalPart())) {
					throw new ServiceException("not supported");
				}
			}
			catch (MalformedURLException ex) {
			}
			if (props == null || !"myValue".equals(props.getProperty("myKey"))) {
				throw new ServiceException("invalid properties");
			}
			serviceCount++;
			return service1;
		}

		@Override
		public Service loadService(Class ifc) throws ServiceException {
			if (!IRemoteBean.class.equals(ifc)) {
				throw new ServiceException("not supported");
			}
			serviceCount++;
			return service2;
		}

		@Override
		public Service loadService(URL url, Class ifc, Properties props) throws ServiceException {
			try {
				if (!(new URL("http://myUrl1")).equals(url) || !IRemoteBean.class.equals(ifc)) {
					throw new ServiceException("not supported");
				}
			}
			catch (MalformedURLException ex) {
			}
			if (props == null || !"myValue".equals(props.getProperty("myKey"))) {
				throw new ServiceException("invalid properties");
			}
			serviceCount++;
			return service1;
		}
	}


	public static class ExceptionMockServiceFactory extends MockServiceFactory {

		public ExceptionMockServiceFactory() throws Exception {
			super();
		}

		@Override
		protected void initMocks() throws Exception {
			super.initMocks();
			given(service1.getPort(new QName("myNamespace", "myPort"),
					IRemoteBean.class)).willReturn(new RemoteBean());
		}
	}


	public static class CallMockServiceFactory extends MockServiceFactory {

		protected static Call call1;

		public CallMockServiceFactory() throws Exception {
			super();
		}

		@Override
		protected void initMocks() throws Exception {
			call1 = mock(Call.class);
			given(service1.createCall(new QName("myNamespace", "myPort"), "setName")).willReturn(call1);
		}
	}


	public static class ExceptionCallMockServiceFactory extends MockServiceFactory {

		protected static Call call1;
		protected static Call call2;

		public ExceptionCallMockServiceFactory() throws Exception {
		}

		@Override
		protected void initMocks() throws Exception {
			call1 = mock(Call.class);
			call2 = mock(Call.class);
			given(service1.createCall(new QName("myNamespace", "myPort"), "setName")).willReturn(call2, call1);
			given(call2.invoke(new Object[] { "exception" })).willThrow(new RemoteException());
		}
	}


	public static interface IBusinessBean {

		public void setName(String name);

	}


	public static interface IRemoteBean extends Remote {

		public void setName(String name) throws RemoteException;

	}


	public static class RemoteBean implements IRemoteBean, Stub {

		private static String name;
		private static Map properties;

		public RemoteBean() {
			properties = new HashMap();
		}

		@Override
		public void setName(String nam) throws RemoteException {
			if ("exception".equals(nam)) {
				throw new RemoteException();
			}
			name = nam;
		}

		@Override
		public void _setProperty(String key, Object o) {
			properties.put(key, o);
		}

		@Override
		public Object _getProperty(String key) {
			return properties.get(key);
		}

		@Override
		public Iterator _getPropertyNames() {
			return properties.keySet().iterator();
		}
	}

}
