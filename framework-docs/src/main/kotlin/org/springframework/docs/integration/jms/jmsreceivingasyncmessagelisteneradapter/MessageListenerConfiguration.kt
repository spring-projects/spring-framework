/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.jms.jmsreceivingasyncmessagelisteneradapter

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.listener.adapter.MessageListenerAdapter

@Configuration
class MessageListenerConfiguration {

	// tag::snippet[]
	@Bean
	fun messageListener(messageDelegate: DefaultTextMessageDelegate) = MessageListenerAdapter(messageDelegate).apply {
		setDefaultListenerMethod("receive")
		// We don't want automatic message context extraction
		setMessageConverter(null)
	}
	// end::snippet[]
}
