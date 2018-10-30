/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.springframework.util.Assert;

/**
 * @author Juergen Hoeller
 */
@WebService(serviceName="OrderService", portName="OrderService",
		endpointInterface = "org.springframework.remoting.jaxws.OrderService")
public class OrderServiceImpl implements OrderService {

	@Resource
	private WebServiceContext webServiceContext;

	@Override
	public String getOrder(int id) throws OrderNotFoundException {
		Assert.notNull(this.webServiceContext, "WebServiceContext has not been injected");
		if (id == 0) {
			throw new OrderNotFoundException("Order 0 not found");
		}
		return "order " + id;
	}

}
