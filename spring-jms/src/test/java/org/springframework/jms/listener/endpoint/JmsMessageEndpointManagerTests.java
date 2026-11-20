/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.jms.support.QosSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
class JmsMessageEndpointManagerTests {

	@Test
	void isPubSubDomainWithQueue() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(false);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.isPubSubDomain()).isFalse();
		assertThat(endpoint.isReplyPubSubDomain()).isFalse();
	}

	@Test
	void isPubSubDomainWithTopic() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(true);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.isPubSubDomain()).isTrue();
		assertThat(endpoint.isReplyPubSubDomain()).isTrue();
	}

	@Test
	void pubSubDomainCustomForReply() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(true);
		config.setReplyPubSubDomain(false);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.isPubSubDomain()).isTrue();
		assertThat(endpoint.isReplyPubSubDomain()).isFalse();
	}

	@Test
	void customReplyQosSettings() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		QosSettings settings = new QosSettings(1, 3, 5);
		config.setReplyQosSettings(settings);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.getReplyQosSettings()).isNotNull();
		assertThat(endpoint.getReplyQosSettings().getDeliveryMode()).isEqualTo(1);
		assertThat(endpoint.getReplyQosSettings().getPriority()).isEqualTo(3);
		assertThat(endpoint.getReplyQosSettings().getTimeToLive()).isEqualTo(5);
	}

	@Test
	void isPubSubDomainWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		// far from ideal
		assertThatIllegalStateException().isThrownBy(
				endpoint::isPubSubDomain);
	}

	@Test
	void isReplyPubSubDomainWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		// far from ideal
		assertThatIllegalStateException().isThrownBy(
				endpoint::isReplyPubSubDomain);
	}

	@Test
	void getReplyQosSettingsWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		// far from ideal
		assertThatIllegalStateException().isThrownBy(
				endpoint::getReplyQosSettings);
	}

	@Test
	void getMessageConverterNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		assertThat(endpoint.getMessageConverter()).isNull();
	}

	@Test
	void getDestinationResolverNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		assertThat(endpoint.getDestinationResolver()).isNull();
	}
}
