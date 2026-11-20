/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.mailusagesimple;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfiguration {

	// tag::snippet[]
	@Bean
	JavaMailSender mailSender() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("mail.mycompany.example");
		return mailSender;
	}

	@Bean // this is a template message that we can pre-load with default state
	SimpleMailMessage templateMessage() {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("customerservice@mycompany.example");
		message.setSubject("Your order");
		return message;
	}

	@Bean
	SimpleOrderManager orderManager(JavaMailSender mailSender, SimpleMailMessage templateMessage) {
		SimpleOrderManager orderManager = new SimpleOrderManager();
		orderManager.setMailSender(mailSender);
		orderManager.setTemplateMessage(templateMessage);
		return orderManager;
	}
	// end::snippet[]
}
