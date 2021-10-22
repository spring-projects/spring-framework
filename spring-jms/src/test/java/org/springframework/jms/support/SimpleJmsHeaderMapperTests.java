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

package org.springframework.jms.support;

import java.util.Date;
import java.util.Map;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import org.junit.jupiter.api.Test;

import org.springframework.jms.StubTextMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Mark Fisher
 * @author Gary Russel
 * @author Stephane Nicoll
 */
public class SimpleJmsHeaderMapperTests {

	private final SimpleJmsHeaderMapper mapper = new SimpleJmsHeaderMapper();


	// Outbound mapping

	@Test
	public void jmsReplyToMappedFromHeader() throws JMSException {
		Destination replyTo = new Destination() {};
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.REPLY_TO, replyTo).build();

		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isNotNull();
		assertThat(jmsMessage.getJMSReplyTo()).isSameAs(replyTo);
	}

	@Test
	public void JmsReplyToIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.REPLY_TO, "not-a-destination").build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isNull();
	}

	@Test
	public void jmsCorrelationIdMappedFromHeader() throws JMSException {
		String jmsCorrelationId = "ABC-123";
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.CORRELATION_ID, jmsCorrelationId).build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNotNull();
		assertThat(jmsMessage.getJMSCorrelationID()).isEqualTo(jmsCorrelationId);
	}

	@Test
	public void jmsCorrelationIdNumberConvertsToString() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.CORRELATION_ID, 123).build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isEqualTo("123");
	}

	@Test
	public void jmsCorrelationIdIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.CORRELATION_ID, new Date()).build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNull();
	}

	@Test
	public void jmsTypeMappedFromHeader() throws JMSException {
		String jmsType = "testing";
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.TYPE, jmsType).build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSType()).isNotNull();
		assertThat(jmsMessage.getJMSType()).isEqualTo(jmsType);
	}

	@Test
	public void jmsTypeIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.TYPE, 123).build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSType()).isNull();
	}

	@Test
	public void jmsReadOnlyPropertiesNotMapped() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.DESTINATION, new Destination() {})
				.setHeader(JmsHeaders.DELIVERY_MODE, DeliveryMode.NON_PERSISTENT)
				.setHeader(JmsHeaders.EXPIRATION, 1000L)
				.setHeader(JmsHeaders.MESSAGE_ID, "abc-123")
				.setHeader(JmsHeaders.PRIORITY, 9)
				.setHeader(JmsHeaders.REDELIVERED, true)
				.setHeader(JmsHeaders.TIMESTAMP, System.currentTimeMillis())
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSDestination()).isNull();
		assertThat(jmsMessage.getJMSDeliveryMode()).isEqualTo(DeliveryMode.PERSISTENT);
		assertThat(jmsMessage.getJMSExpiration()).isEqualTo(0);
		assertThat(jmsMessage.getJMSMessageID()).isNull();
		assertThat(jmsMessage.getJMSPriority()).isEqualTo(jakarta.jms.Message.DEFAULT_PRIORITY);
		assertThat(jmsMessage.getJMSRedelivered()).isFalse();
		assertThat(jmsMessage.getJMSTimestamp()).isEqualTo(0);
	}

	@Test
	public void contentTypePropertyMappedFromHeader() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(MessageHeaders.CONTENT_TYPE, "foo")
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty(JmsHeaderMapper.CONTENT_TYPE_PROPERTY);
		assertThat(value).isNotNull();
		assertThat(value).isEqualTo("foo");
	}

	@Test
	public void userDefinedPropertyMappedFromHeader() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader("foo", 123)
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("foo");
		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(Integer.class);
		assertThat(((Integer) value).intValue()).isEqualTo(123);
	}

	@Test
	public void userDefinedPropertyMappedFromHeaderWithCustomPrefix() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader("foo", 123)
				.build();
		mapper.setOutboundPrefix("custom_");
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("custom_foo");
		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(Integer.class);
		assertThat(((Integer) value).intValue()).isEqualTo(123);
	}

	@Test
	public void userDefinedPropertyWithUnsupportedType() throws JMSException {
		Destination destination = new Destination() {};
		Message<String> message = initBuilder()
				.setHeader("destination", destination)
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("destination");
		assertThat(value).isNull();
	}

	@Test
	public void attemptToReadDisallowedCorrelationIdPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public String getJMSCorrelationID() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.CORRELATION_ID);
	}

	@Test
	public void attemptToReadDisallowedDestinationPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public Destination getJMSDestination() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.DESTINATION);
	}

	@Test
	public void attemptToReadDisallowedDeliveryModePropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public int getJMSDeliveryMode() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.DELIVERY_MODE);
	}

	@Test
	public void attemptToReadDisallowedExpirationPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public long getJMSExpiration() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.EXPIRATION);
	}

	@Test
	public void attemptToReadDisallowedMessageIdPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public String getJMSMessageID() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.MESSAGE_ID);
	}

	@Test
	public void attemptToReadDisallowedPriorityPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public int getJMSPriority() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.PRIORITY);
	}

	@Test
	public void attemptToReadDisallowedReplyToPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public Destination getJMSReplyTo() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.REPLY_TO);
	}

	@Test
	public void attemptToReadDisallowedRedeliveredPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public boolean getJMSRedelivered() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.REDELIVERED);
	}

	@Test
	public void attemptToReadDisallowedTypePropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public String getJMSType() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.TYPE);
	}

	@Test
	public void attemptToReadDisallowedTimestampPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public long getJMSTimestamp() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, JmsHeaders.TIMESTAMP);
	}

	@Test
	public void attemptToReadDisallowedUserPropertyIsNotFatal() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public Object getObjectProperty(String name) throws JMSException {
				if (name.equals("fail")) {
					throw new JMSException("illegal property");
				}
				else {
					return super.getObjectProperty(name);
				}
			}
		};
		jmsMessage.setBooleanProperty("fail", true);
		assertAttemptReadDisallowedPropertyIsNotFatal(jmsMessage, "fail");
	}


	// Inbound mapping

	@Test
	public void jmsCorrelationIdMappedToHeader() throws JMSException {
		String correlationId = "ABC-123";
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSCorrelationID(correlationId);
		assertInboundHeader(jmsMessage, JmsHeaders.CORRELATION_ID, correlationId);
	}

	@Test
	public void destinationMappedToHeader() throws JMSException {
		Destination destination = new Destination() {};
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSDestination(destination);
		assertInboundHeader(jmsMessage, JmsHeaders.DESTINATION, destination);
	}

	@Test
	public void jmsDeliveryModeMappedToHeader() throws JMSException {
		int deliveryMode = 1;
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSDeliveryMode(deliveryMode);
		assertInboundHeader(jmsMessage, JmsHeaders.DELIVERY_MODE, deliveryMode);
	}

	@Test
	public void jmsExpirationMappedToHeader() throws JMSException {
		long expiration = 1000L;
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSExpiration(expiration);
		assertInboundHeader(jmsMessage, JmsHeaders.EXPIRATION, expiration);
	}

	@Test
	public void jmsMessageIdMappedToHeader() throws JMSException {
		String messageId = "ID:ABC-123";
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSMessageID(messageId);
		assertInboundHeader(jmsMessage, JmsHeaders.MESSAGE_ID, messageId);
	}

	@Test
	public void jmsPriorityMappedToHeader() throws JMSException {
		int priority = 8;
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSPriority(priority);
		assertInboundHeader(jmsMessage, JmsHeaders.PRIORITY, priority);
	}

	@Test
	public void jmsReplyToMappedToHeader() throws JMSException {
		Destination replyTo = new Destination() {};
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSReplyTo(replyTo);
		assertInboundHeader(jmsMessage, JmsHeaders.REPLY_TO, replyTo);
	}

	@Test
	public void jmsTypeMappedToHeader() throws JMSException {
		String type = "testing";
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSType(type);
		assertInboundHeader(jmsMessage, JmsHeaders.TYPE, type);
	}

	@Test
	public void jmsTimestampMappedToHeader() throws JMSException {
		long timestamp = 123L;
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSTimestamp(timestamp);
		assertInboundHeader(jmsMessage, JmsHeaders.TIMESTAMP, timestamp);
	}

	@Test
	public void contentTypePropertyMappedToHeader() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setStringProperty("content_type", "foo");
		assertInboundHeader(jmsMessage, MessageHeaders.CONTENT_TYPE, "foo");
	}

	@Test
	public void userDefinedPropertyMappedToHeader() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setIntProperty("foo", 123);
		assertInboundHeader(jmsMessage, "foo", 123);
	}

	@Test
	public void userDefinedPropertyMappedToHeaderWithCustomPrefix() throws JMSException {
		jakarta.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setIntProperty("foo", 123);
		mapper.setInboundPrefix("custom_");
		assertInboundHeader(jmsMessage, "custom_foo", 123);
	}

	@Test
	public void propertyMappingExceptionIsNotFatal() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader("foo", 123)
				.setHeader("bad", 456)
				.setHeader("bar", 789)
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public void setObjectProperty(String name, Object value) throws JMSException {
				if (name.equals("bad")) {
					throw new JMSException("illegal property");
				}
				super.setObjectProperty(name, value);
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object foo = jmsMessage.getObjectProperty("foo");
		assertThat(foo).isNotNull();
		Object bar = jmsMessage.getObjectProperty("bar");
		assertThat(bar).isNotNull();
		Object bad = jmsMessage.getObjectProperty("bad");
		assertThat(bad).isNull();
	}

	@Test
	public void illegalArgumentExceptionIsNotFatal() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader("foo", 123)
				.setHeader("bad", 456)
				.setHeader("bar", 789)
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public void setObjectProperty(String name, Object value) throws JMSException {
				if (name.equals("bad")) {
					throw new IllegalArgumentException("illegal property");
				}
				super.setObjectProperty(name, value);
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object foo = jmsMessage.getObjectProperty("foo");
		assertThat(foo).isNotNull();
		Object bar = jmsMessage.getObjectProperty("bar");
		assertThat(bar).isNotNull();
		Object bad = jmsMessage.getObjectProperty("bad");
		assertThat(bad).isNull();
	}

	@Test
	public void attemptToWriteDisallowedReplyToPropertyIsNotFatal() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.REPLY_TO, new Destination() {})
				.setHeader("foo", "bar")
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public void setJMSReplyTo(Destination replyTo) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToWriteDisallowedTypePropertyIsNotFatal() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.TYPE, "someType")
				.setHeader("foo", "bar")
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public void setJMSType(String type) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSType()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToWriteDisallowedCorrelationIdStringPropertyIsNotFatal() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.CORRELATION_ID, "abc")
				.setHeader("foo", "bar")
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public void setJMSCorrelationID(String correlationId) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToWriteDisallowedCorrelationIdNumberPropertyIsNotFatal() throws JMSException {
		Message<String> message = initBuilder()
				.setHeader(JmsHeaders.CORRELATION_ID, 123)
				.setHeader("foo", "bar")
				.build();
		jakarta.jms.Message jmsMessage = new StubTextMessage() {
			@Override
			public void setJMSCorrelationID(String correlationId) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}


	private void assertInboundHeader(jakarta.jms.Message jmsMessage, String headerId, Object value) {
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object headerValue = headers.get(headerId);
		if (value == null) {
			assertThat(headerValue).isNull();
		}
		else {
			assertThat(headerValue).isNotNull();
			assertThat(headerValue.getClass()).isEqualTo(value.getClass());
			assertThat(headerValue).isEqualTo(value);
		}
	}

	private void assertAttemptReadDisallowedPropertyIsNotFatal(jakarta.jms.Message jmsMessage, String headerId)
			throws JMSException {
		jmsMessage.setStringProperty("foo", "bar");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		assertThat(headers.get(headerId)).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	private MessageBuilder<String> initBuilder() {
		return MessageBuilder.withPayload("test");
	}

}
