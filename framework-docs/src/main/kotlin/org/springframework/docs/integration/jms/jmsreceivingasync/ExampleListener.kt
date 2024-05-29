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

package org.springframework.docs.integration.jms.jmsreceivingasync

import jakarta.jms.JMSException
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.TextMessage

// tag::snippet[]
class ExampleListener : MessageListener {

	override fun onMessage(message: Message) {
		if (message is TextMessage) {
			try {
				println(message.text)
			} catch (ex: JMSException) {
				throw RuntimeException(ex)
			}
		} else {
			throw IllegalArgumentException("Message must be of type TextMessage")
		}
	}
}
// end::snippet[]