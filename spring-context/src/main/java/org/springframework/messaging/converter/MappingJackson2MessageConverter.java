/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author Rossen Stoyanchev
 * @sicne 4.0
 */
public class MappingJackson2MessageConverter implements MessageConverter<Object> {

	private ObjectMapper objectMapper = new ObjectMapper();

	private Type defaultObjectType = Map.class;

	private Class<?> defaultMessagePayloadClass = byte[].class;


	/**
	 * Set the default target Object class to convert to in
	 * {@link #fromMessage(Message, Class)}.
	 */
	public void setDefaultObjectClass(Type defaultObjectType) {
		Assert.notNull(defaultObjectType, "defaultObjectType is required");
		this.defaultObjectType = defaultObjectType;
	}

	/**
	 * Set the type of Message payload to convert to in {@link #toMessage(Object)}.
	 * @param payloadClass either byte[] or String
	 */
	public void setDefaultTargetPayloadClass(Class<?> payloadClass) {
		Assert.isTrue(byte[].class.equals(payloadClass) || String.class.equals(payloadClass),
				"Payload class must be byte[] or String: " + payloadClass);
		this.defaultMessagePayloadClass = payloadClass;
	}

	@Override
	public Object fromMessage(Message<?> message, Type objectType) {

		JavaType javaType = (objectType != null) ?
				this.objectMapper.constructType(objectType) :
					this.objectMapper.constructType(this.defaultObjectType);

		Object payload = message.getPayload();
		try {
			if (payload instanceof byte[]) {
				return this.objectMapper.readValue((byte[]) payload, javaType);
			}
			else if (payload instanceof String) {
				return this.objectMapper.readValue((String) payload, javaType);
			}
			else {
				throw new IllegalArgumentException("Unexpected message payload type: " + payload);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <P> Message<P> toMessage(Object object) {
		P payload;
		try {
			if (byte[].class.equals(this.defaultMessagePayloadClass)) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				this.objectMapper.writeValue(out, object);
				payload = (P) out.toByteArray();
			}
			else if (String.class.equals(this.defaultMessagePayloadClass)) {
				Writer writer = new StringWriter();
				this.objectMapper.writeValue(writer, object);
				payload = (P) writer.toString();
			}
			else {
				// Should never happen..
				throw new IllegalStateException("Unexpected payload class: " + defaultMessagePayloadClass);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return MessageBuilder.withPayload(payload).build();
	}

}
