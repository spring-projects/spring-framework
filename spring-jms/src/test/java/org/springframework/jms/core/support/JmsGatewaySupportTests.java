/*
 * Copyright 2002-2005 the original author or authors.
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
package org.springframework.jms.core.support;

import java.util.ArrayList;
import java.util.List;

import javax.jms.ConnectionFactory;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jms.core.JmsTemplate;

/**
 * @author Mark Pollack
 * @since 24.9.2004
 */
public class JmsGatewaySupportTests extends TestCase {

	public void testJmsGatewaySupportWithConnectionFactory() throws Exception {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory mockConnectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		connectionFactoryControl.replay();
		final List test = new ArrayList();
		JmsGatewaySupport gateway = new JmsGatewaySupport() {
			protected void initGateway() {
				test.add("test");
			}
		};
		gateway.setConnectionFactory(mockConnectionFactory);
		gateway.afterPropertiesSet();
		assertEquals("Correct ConnectionFactory", mockConnectionFactory, gateway.getConnectionFactory());
		assertEquals("Correct JmsTemplate", mockConnectionFactory, gateway.getJmsTemplate().getConnectionFactory());
		assertEquals("initGatway called", test.size(), 1);
		connectionFactoryControl.verify();

	}
	public void testJmsGatewaySupportWithJmsTemplate() throws Exception {
		JmsTemplate template = new JmsTemplate();
		final List test = new ArrayList();
		JmsGatewaySupport gateway = new JmsGatewaySupport() {
			protected void initGateway() {
				test.add("test");
			}
		};
		gateway.setJmsTemplate(template);
		gateway.afterPropertiesSet();
		assertEquals("Correct JmsTemplate", template, gateway.getJmsTemplate());
		assertEquals("initGateway called", test.size(), 1);
	}

}
