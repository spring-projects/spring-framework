/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.remoting.jaxws;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 2.5
 */
public class JaxWsSupportTests extends TestCase {

	public void testJaxWsPortAccess() throws Exception {
		GenericApplicationContext ac = new GenericApplicationContext();

		GenericBeanDefinition serviceDef = new GenericBeanDefinition();
		serviceDef.setBeanClass(OrderServiceImpl.class);
		ac.registerBeanDefinition("service", serviceDef);

		GenericBeanDefinition exporterDef = new GenericBeanDefinition();
		exporterDef.setBeanClass(SimpleJaxWsServiceExporter.class);
		exporterDef.getPropertyValues().addPropertyValue("baseAddress", "http://localhost:9999/");
		ac.registerBeanDefinition("exporter", exporterDef);

		GenericBeanDefinition clientDef = new GenericBeanDefinition();
		clientDef.setBeanClass(JaxWsPortProxyFactoryBean.class);
		clientDef.getPropertyValues().addPropertyValue("wsdlDocumentUrl", "http://localhost:9999/OrderService?wsdl");
		clientDef.getPropertyValues().addPropertyValue("namespaceUri", "http://jaxws.remoting.springframework.org/");
		clientDef.getPropertyValues().addPropertyValue("username", "juergen");
		clientDef.getPropertyValues().addPropertyValue("password", "hoeller");
		clientDef.getPropertyValues().addPropertyValue("serviceName", "OrderService");
		clientDef.getPropertyValues().addPropertyValue("serviceInterface", OrderService.class);
		clientDef.getPropertyValues().addPropertyValue("lookupServiceOnStartup", Boolean.FALSE);
		ac.registerBeanDefinition("client", clientDef);

		GenericBeanDefinition serviceFactoryDef = new GenericBeanDefinition();
		serviceFactoryDef.setBeanClass(LocalJaxWsServiceFactoryBean.class);
		serviceFactoryDef.getPropertyValues().addPropertyValue("wsdlDocumentUrl", "http://localhost:9999/OrderService?wsdl");
		serviceFactoryDef.getPropertyValues().addPropertyValue("namespaceUri", "http://jaxws.remoting.springframework.org/");
		serviceFactoryDef.getPropertyValues().addPropertyValue("serviceName", "OrderService");
		ac.registerBeanDefinition("orderService", serviceFactoryDef);

		ac.registerBeanDefinition("accessor", new RootBeanDefinition(ServiceAccessor.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);

		try {
			ac.refresh();

			OrderService orderService = (OrderService) ac.getBean("client", OrderService.class);
			assertTrue(orderService instanceof BindingProvider);
			((BindingProvider) orderService).getRequestContext();

			String order = orderService.getOrder(1000);
			assertEquals("order 1000", order);
			try {
				orderService.getOrder(0);
				fail("Should have thrown OrderNotFoundException");
			}
			catch (OrderNotFoundException ex) {
				// expected
			}

			ServiceAccessor serviceAccessor = (ServiceAccessor) ac.getBean("accessor", ServiceAccessor.class);
			order = serviceAccessor.orderService.getOrder(1000);
			assertEquals("order 1000", order);
			try {
				serviceAccessor.orderService.getOrder(0);
				fail("Should have thrown OrderNotFoundException");
			}
			catch (OrderNotFoundException ex) {
				// expected
			}
		}
		catch (BeanCreationException ex) {
			if ("exporter".equals(ex.getBeanName()) && ex.getRootCause() instanceof ClassNotFoundException) {
				// ignore - probably running on JDK < 1.6 without the JAX-WS impl present
			}
			else {
				throw ex;
			}
		}
		finally {
			ac.close();
		}
	}


	public static class ServiceAccessor {

		@WebServiceRef
		public OrderService orderService;

		public OrderService myService;

		@WebServiceRef(value=OrderServiceService.class, wsdlLocation = "http://localhost:9999/OrderService?wsdl")
		public void setMyService(OrderService myService) {
			this.myService = myService;
		}
	}


	@WebServiceClient(targetNamespace = "http://jaxws.remoting.springframework.org/", name="OrderService")
	public static class OrderServiceService extends Service {

		public OrderServiceService() throws MalformedURLException {
			super(new URL("http://localhost:9999/OrderService?wsdl"),
					new QName("http://jaxws.remoting.springframework.org/", "OrderService"));
		}

		public OrderServiceService(URL wsdlDocumentLocation, QName serviceName) {
			super(wsdlDocumentLocation, serviceName);
		}
	}

}
