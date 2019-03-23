/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class JmsMessageEndpointManagerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void isPubSubDomainWithQueue() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(false);
		endpoint.setActivationSpecConfig(config);
		assertEquals(false, endpoint.isPubSubDomain());
		assertEquals(false, endpoint.isReplyPubSubDomain());
	}

	@Test
	public void isPubSubDomainWithTopic() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(true);
		endpoint.setActivationSpecConfig(config);
		assertEquals(true, endpoint.isPubSubDomain());
		assertEquals(true, endpoint.isReplyPubSubDomain());
	}

	@Test
	public void pubSubDomainCustomForReply() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		JmsActivationSpecConfig config = new JmsActivationSpecConfig();
		config.setPubSubDomain(true);
		config.setReplyPubSubDomain(false);
		endpoint.setActivationSpecConfig(config);
		assertEquals(true, endpoint.isPubSubDomain());
		assertEquals(false, endpoint.isReplyPubSubDomain());
	}

	@Test
	public void isPubSubDomainWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();

		thrown.expect(IllegalStateException.class); // far from ideal
		endpoint.isPubSubDomain();
	}

	@Test
	public void isReplyPubSubDomainWithNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();

		thrown.expect(IllegalStateException.class); // far from ideal
		endpoint.isReplyPubSubDomain();
	}

	@Test
	public void getMessageConverterNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		assertNull(endpoint.getMessageConverter());
	}

	@Test
	public void getDestinationResolverNoConfig() {
		JmsMessageEndpointManager endpoint = new JmsMessageEndpointManager();
		assertNull(endpoint.getDestinationResolver());
	}
}
