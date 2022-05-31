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

package org.springframework.remoting.jaxws;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServiceRef;
import javax.xml.ws.soap.AddressingFeature;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.remoting.RemoteAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * @author Juergen Hoeller
 * @since 2.5
 */
public class JaxWsSupportTests {

	@Test
	public void testJaxWsPortAccess() throws Exception {
		doTestJaxWsPortAccess((WebServiceFeature[]) null);
	}

	@Test
	public void testJaxWsPortAccessWithFeature() throws Exception {
		doTestJaxWsPortAccess(new AddressingFeature());
	}

	private void doTestJaxWsPortAccess(WebServiceFeature... features) throws Exception {
		GenericApplicationContext ac = new GenericApplicationContext();

		GenericBeanDefinition serviceDef = new GenericBeanDefinition();
		serviceDef.setBeanClass(OrderServiceImpl.class);
		ac.registerBeanDefinition("service", serviceDef);

		GenericBeanDefinition exporterDef = new GenericBeanDefinition();
		exporterDef.setBeanClass(SimpleJaxWsServiceExporter.class);
		exporterDef.getPropertyValues().add("baseAddress", "http://localhost:9999/");
		ac.registerBeanDefinition("exporter", exporterDef);

		GenericBeanDefinition clientDef = new GenericBeanDefinition();
		clientDef.setBeanClass(JaxWsPortProxyFactoryBean.class);
		clientDef.getPropertyValues().add("wsdlDocumentUrl", "http://localhost:9999/OrderService?wsdl");
		clientDef.getPropertyValues().add("namespaceUri", "http://jaxws.remoting.springframework.org/");
		clientDef.getPropertyValues().add("username", "juergen");
		clientDef.getPropertyValues().add("password", "hoeller");
		clientDef.getPropertyValues().add("serviceName", "OrderService");
		clientDef.getPropertyValues().add("serviceInterface", OrderService.class);
		clientDef.getPropertyValues().add("lookupServiceOnStartup", Boolean.FALSE);
		if (features != null) {
			clientDef.getPropertyValues().add("portFeatures", features);
		}
		ac.registerBeanDefinition("client", clientDef);

		GenericBeanDefinition serviceFactoryDef = new GenericBeanDefinition();
		serviceFactoryDef.setBeanClass(LocalJaxWsServiceFactoryBean.class);
		serviceFactoryDef.getPropertyValues().add("wsdlDocumentUrl", "http://localhost:9999/OrderService?wsdl");
		serviceFactoryDef.getPropertyValues().add("namespaceUri", "http://jaxws.remoting.springframework.org/");
		serviceFactoryDef.getPropertyValues().add("serviceName", "OrderService");
		ac.registerBeanDefinition("orderService", serviceFactoryDef);

		ac.registerBeanDefinition("accessor", new RootBeanDefinition(ServiceAccessor.class));
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);

		try {
			ac.refresh();

			OrderService orderService = ac.getBean("client", OrderService.class);
			boolean condition = orderService instanceof BindingProvider;
			assertThat(condition).isTrue();
			((BindingProvider) orderService).getRequestContext();

			String order = orderService.getOrder(1000);
			assertThat(order).isEqualTo("order 1000");
			assertThatException()
				.isThrownBy(() -> orderService.getOrder(0))
				.matches(ex -> ex instanceof OrderNotFoundException || ex instanceof RemoteAccessException);
			// ignore RemoteAccessException as probably setup issue with JAX-WS provider vs JAXB

			ServiceAccessor serviceAccessor = ac.getBean("accessor", ServiceAccessor.class);
			order = serviceAccessor.orderService.getOrder(1000);
			assertThat(order).isEqualTo("order 1000");
			assertThatException()
				.isThrownBy(() -> serviceAccessor.orderService.getOrder(0))
				.matches(ex -> ex instanceof OrderNotFoundException || ex instanceof WebServiceException);
			// ignore WebServiceException as probably setup issue with JAX-WS provider vs JAXB
		}
		catch (BeanCreationException ex) {
			if ("exporter".equals(ex.getBeanName()) && ex.getRootCause() instanceof ClassNotFoundException) {
				// ignore - probably running on JDK without the JAX-WS impl present
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

		@WebServiceRef(value = OrderServiceService.class, wsdlLocation = "http://localhost:9999/OrderService?wsdl")
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
