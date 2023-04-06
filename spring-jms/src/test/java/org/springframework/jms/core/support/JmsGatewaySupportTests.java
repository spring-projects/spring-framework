/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jms.core.support;

import java.util.ArrayList;
import java.util.List;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Pollack
 * @since 24.9.2004
 */
class JmsGatewaySupportTests {

	@Test
	void testJmsGatewaySupportWithConnectionFactory() throws Exception {
		ConnectionFactory mockConnectionFactory = mock();
		final List<String> test = new ArrayList<>(1);
		JmsGatewaySupport gateway = new JmsGatewaySupport() {
			@Override
			protected void initGateway() {
				test.add("test");
			}
		};
		gateway.setConnectionFactory(mockConnectionFactory);
		gateway.afterPropertiesSet();
		assertThat(gateway.getConnectionFactory()).as("Correct ConnectionFactory").isEqualTo(mockConnectionFactory);
		assertThat(gateway.getJmsTemplate().getConnectionFactory()).as("Correct JmsTemplate").isEqualTo(mockConnectionFactory);
		assertThat(test).as("initGateway called").hasSize(1);
	}

	@Test
	void testJmsGatewaySupportWithJmsTemplate() throws Exception {
		JmsTemplate template = new JmsTemplate();
		final List<String> test = new ArrayList<>(1);
		JmsGatewaySupport gateway = new JmsGatewaySupport() {
			@Override
			protected void initGateway() {
				test.add("test");
			}
		};
		gateway.setJmsTemplate(template);
		gateway.afterPropertiesSet();
		assertThat(gateway.getJmsTemplate()).as("Correct JmsTemplate").isEqualTo(template);
		assertThat(test).as("initGateway called").hasSize(1);
	}

}
