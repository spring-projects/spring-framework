/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.mailusagesimple

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

@Configuration
class MailConfiguration {

	// tag::snippet[]
	@Bean
	fun mailSender(): JavaMailSender {
		return JavaMailSenderImpl().apply {
			host = "mail.mycompany.example"
		}
	}

	@Bean // this is a template message that we can pre-load with default state
	fun templateMessage() = SimpleMailMessage().apply {
		from = "customerservice@mycompany.example"
		subject = "Your order"
	}


	@Bean
	fun orderManager(javaMailSender: JavaMailSender, simpleTemplateMessage: SimpleMailMessage) = SimpleOrderManager().apply {
		mailSender = javaMailSender
		templateMessage = simpleTemplateMessage
	}
	// end::snippet[]
}
