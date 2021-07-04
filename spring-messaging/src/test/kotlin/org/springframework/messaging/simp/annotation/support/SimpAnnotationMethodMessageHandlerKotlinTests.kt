/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.simp.annotation.support

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import java.util.Collections
import java.util.HashMap

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.springframework.context.support.StaticApplicationContext
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.converter.MessageConverter
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Controller

import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.handler.annotation.MessageExceptionHandler

/**
 * Kotlin test fixture for [SimpAnnotationMethodMessageHandler].
 *
 * @author Sebastien Deleuze
 */
class SimpAnnotationMethodMessageHandlerKotlinTests {

	lateinit var messageHandler: TestSimpAnnotationMethodMessageHandler

	lateinit var testController: TestController

    val channel = mockk<SubscribableChannel>(relaxed = true)

    val converter = mockk<MessageConverter>(relaxed = true)

    @BeforeEach
    fun setup() {
        val brokerTemplate = SimpMessagingTemplate(channel)
        brokerTemplate.messageConverter = converter
        messageHandler = TestSimpAnnotationMethodMessageHandler(brokerTemplate, channel, channel)
        messageHandler.applicationContext = StaticApplicationContext()
        messageHandler.afterPropertiesSet()
        testController = TestController()
    }

    @Test
    fun nullableHeaderWithHeader() {
        val message = createMessage("/nullableHeader", Collections.singletonMap("foo", "bar"))
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
        assertThat(testController.exception).isNull()
		assertThat(testController.header).isEqualTo("bar")
    }

    @Test
    fun nullableHeaderWithoutHeader() {
        val message = createMessage("/nullableHeader", Collections.emptyMap())
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
		assertThat(testController.exception).isNull()
		assertThat(testController.header).isNull()
    }

    @Test
    fun nonNullableHeaderWithHeader() {
        val message = createMessage("/nonNullableHeader", Collections.singletonMap("foo", "bar"))
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
		assertThat(testController.header).isEqualTo("bar")
    }

    @Test
    fun nonNullableHeaderWithoutHeader() {
        val message = createMessage("/nonNullableHeader", Collections.emptyMap())
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
        assertThat(testController.exception).isNotNull()
        assertThat(testController.exception).isInstanceOf(MessageHandlingException::class.java)
    }

    @Test
    fun nullableHeaderNotRequiredWithHeader() {
        val message = createMessage("/nullableHeaderNotRequired", Collections.singletonMap("foo", "bar"))
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
		assertThat(testController.exception).isNull()
		assertThat(testController.header).isEqualTo("bar")
    }

    @Test
    fun nullableHeaderNotRequiredWithoutHeader() {
        val message = createMessage("/nullableHeaderNotRequired", Collections.emptyMap())
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
		assertThat(testController.exception).isNull()
		assertThat(testController.header).isNull()
    }

    @Test
    fun nonNullableHeaderNotRequiredWithHeader() {
        val message = createMessage("/nonNullableHeaderNotRequired", Collections.singletonMap("foo", "bar"))
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
		assertThat(testController.header).isEqualTo("bar")
    }

    @Test
    fun nonNullableHeaderNotRequiredWithoutHeader() {
        val message = createMessage("/nonNullableHeaderNotRequired", Collections.emptyMap())
        messageHandler.registerHandler(testController)
        messageHandler.handleMessage(message)
        assertThat(testController.exception).isNotNull()
		assertThat(testController.exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createMessage(destination: String, headers: Map<String, String?>): Message<ByteArray> {
        val accessor = SimpMessageHeaderAccessor.create()
        accessor.sessionId = "session1"
        accessor.sessionAttributes = HashMap()
        accessor.destination = destination
        for (entry in headers.entries) accessor.setHeader(entry.key, entry.value)

        return MessageBuilder.withPayload(ByteArray(0)).setHeaders(accessor).build()
    }

    class TestSimpAnnotationMethodMessageHandler(brokerTemplate: SimpMessageSendingOperations,
                                                         clientInboundChannel: SubscribableChannel,
                                                         clientOutboundChannel: MessageChannel) :
            SimpAnnotationMethodMessageHandler(clientInboundChannel, clientOutboundChannel, brokerTemplate) {

        fun registerHandler(handler: Any) {
            super.detectHandlerMethods(handler)
        }
    }

    @Suppress("unused")
    @Controller
    @MessageMapping
    class TestController {

        var header: String? = null
        var exception: Throwable? = null

        @MessageMapping("/nullableHeader")
        fun nullableHeader(@Header("foo") foo: String?) {
            header = foo
        }

        @MessageMapping("/nonNullableHeader")
        fun nonNullableHeader(@Header("foo") foo: String) {
            header = foo
        }

        @MessageMapping("/nullableHeaderNotRequired")
        fun nullableHeaderNotRequired(@Header("foo", required = false) foo: String?) {
            header = foo
        }

        @MessageMapping("/nonNullableHeaderNotRequired")
        fun nonNullableHeaderNotRequired(@Header("foo", required = false) foo: String) {
            header = foo
        }

        @MessageExceptionHandler
        fun handleIllegalArgumentException(exception: Throwable) {
            this.exception = exception
        }
    }

}
