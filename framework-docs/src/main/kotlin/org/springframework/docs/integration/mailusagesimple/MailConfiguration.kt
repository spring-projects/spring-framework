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
