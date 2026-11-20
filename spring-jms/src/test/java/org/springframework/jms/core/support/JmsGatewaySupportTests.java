/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
@SuppressWarnings("removal")
class JmsGatewaySupportTests {

	@Test
	void jmsGatewaySupportWithConnectionFactory() {
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
	void jmsGatewaySupportWithJmsTemplate() {
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
