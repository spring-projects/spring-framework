/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.docs.integration.jms.jmsreceivingasync.ExampleListener;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

@Configuration
public class JmsConfiguration {

	// tag::snippet[]
	@Bean
	MessageListenerAdapter messageListener(DefaultMessageDelegate messageDelegate) {
		return new MessageListenerAdapter(messageDelegate);
	}

	@Bean
	DefaultMessageListenerContainer jmsContainer(ConnectionFactory connectionFactory, Destination destination,
			ExampleListener messageListener) {

		DefaultMessageListenerContainer jmsContainer = new DefaultMessageListenerContainer();
		jmsContainer.setConnectionFactory(connectionFactory);
		jmsContainer.setDestination(destination);
		jmsContainer.setMessageListener(messageListener);
		return jmsContainer;
	}
	// end::snippet[]
}
