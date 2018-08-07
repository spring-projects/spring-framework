/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceProvider;

/**
 * Simple exporter for JAX-WS services, autodetecting annotated service beans
 * (through the JAX-WS {@link javax.jws.WebService} annotation) and exporting
 * them with a configured base address (by default "http://localhost:8080/")
 * using the JAX-WS provider's built-in publication support. The full address
 * for each service will consist of the base address with the service name
 * appended (e.g. "http://localhost:8080/OrderService").
 *
 * <p>Note that this exporter will only work if the JAX-WS runtime actually
 * supports publishing with an address argument, i.e. if the JAX-WS runtime
 * ships an internal HTTP server.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.jws.WebService
 * @see javax.xml.ws.Endpoint#publish(String)
 * @see SimpleHttpServerJaxWsServiceExporter
 */
public class SimpleJaxWsServiceExporter extends AbstractJaxWsServiceExporter {

	/**
	 * The default base address.
	 */
	public static final String DEFAULT_BASE_ADDRESS = "http://localhost:8080/";

	private String baseAddress = DEFAULT_BASE_ADDRESS;


	/**
	 * Set the base address for exported services.
	 * Default is "http://localhost:8080/".
	 * <p>For each actual publication address, the service name will be
	 * appended to this base address. E.g. service name "OrderService"
	 * -> "http://localhost:8080/OrderService".
	 * @see javax.xml.ws.Endpoint#publish(String)
	 * @see javax.jws.WebService#serviceName()
	 */
	public void setBaseAddress(String baseAddress) {
		this.baseAddress = baseAddress;
	}


	@Override
	protected void publishEndpoint(Endpoint endpoint, WebService annotation) {
		endpoint.publish(calculateEndpointAddress(endpoint, annotation.serviceName()));
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation) {
		endpoint.publish(calculateEndpointAddress(endpoint, annotation.serviceName()));
	}

	/**
	 * Calculate the full endpoint address for the given endpoint.
	 * @param endpoint the JAX-WS Provider Endpoint object
	 * @param serviceName the given service name
	 * @return the full endpoint address
	 */
	protected String calculateEndpointAddress(Endpoint endpoint, String serviceName) {
		String fullAddress = this.baseAddress + serviceName;
		if (endpoint.getClass().getName().startsWith("weblogic.")) {
			// Workaround for WebLogic 10.3
			fullAddress = fullAddress + "/";
		}
		return fullAddress;
	}

}
