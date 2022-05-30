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

package org.springframework.jms.listener.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.jms.support.QosSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Stephane Nicoll
 */
public class JmsMessageEndpointManagerTests {

	@Test
	public void isPubSubDomainWithQueue() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(false);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.isPubSubDomain()).isFalse();
		assertThat(endpoint.isReplyPubSubDomain()).isFalse();
	}

	@Test
	public void isPubSubDomainWithTopic() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(true);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.isPubSubDomain()).isTrue();
		assertThat(endpoint.isReplyPubSubDomain()).isTrue();
	}

	@Test
	public void pubSubDomainCustomForReply() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(true);
		config.setReplyPubSubDomain(false);
		endpoint.setActivationSpecConfig(config);
		assertThat(endpoint.isPubSubDomain()).isTrue();
		assertThat(endpoint.isReplyPubSubDomain()).isFalse();
	}

	@Test
	public void customReplyQosSettings() {
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
	public void isPubSubDomainWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		// far from ideal
		assertThatIllegalStateException().isThrownBy(
				endpoint::isPubSubDomain);
	}

	@Test
	public void isReplyPubSubDomainWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		// far from ideal
		assertThatIllegalStateException().isThrownBy(
				endpoint::isReplyPubSubDomain);
	}

	@Test
	public void getReplyQosSettingsWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		// far from ideal
		assertThatIllegalStateException().isThrownBy(
				endpoint::getReplyQosSettings);
	}

	@Test
	public void getMessageConverterNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		assertThat(endpoint.getMessageConverter()).isNull();
	}

	@Test
	public void getDestinationResolverNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		assertThat(endpoint.getDestinationResolver()).isNull();
	}
}
