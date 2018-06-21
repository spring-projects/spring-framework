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

package org.springframework.jms.support.converter;

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base {@link MessageConverter} providing the mapping.
 *
 * @author Marten Deinum
 * @since 5.2.0
 */
public abstract class AbstractMappingMessageConverter implements SmartMessageConverter, BeanClassLoaderAware {

	/**
	 * The default encoding used for writing to text messages: UTF-8.
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";

	private MessageType targetType = MessageType.BYTES;

	@Nullable
	private String encoding;

	@Nullable
	private String encodingPropertyName;

	@Nullable
	private String typeIdPropertyName;

	private Map<String, Class<?>> idClassMappings = new HashMap<>();

	private Map<Class<?>, String> classIdMappings = new HashMap<>();

	@Nullable
	private ClassLoader beanClassLoader;

	/**
	 * Specify whether {@link #toMessage(Object, javax.jms.Session)} should marshal to a
	 * {@link BytesMessage} or a {@link TextMessage}.
	 * <p>The default is {@link MessageType#BYTES}, i.e. this converter marshals to
	 * a {@link BytesMessage}. Note that the default version of this converter
	 * supports {@link MessageType#BYTES} and {@link MessageType#TEXT} only.
	 * @see MessageType#BYTES
	 * @see MessageType#TEXT
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	public MessageType getTargetType() {
		return this.targetType;
	}

	/**
	 * Specify the encoding to use when converting to and from text-based
	 * message body content. The default encoding will be "UTF-8".
	 * <p>When reading from a a text-based message, an encoding may have been
	 * suggested through a special JMS property which will then be preferred
	 * over the encoding set on this MessageConverter instance.
	 * @see #setEncodingPropertyName
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Nullable
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * Specify the name of the JMS message property that carries the encoding from
	 * bytes to String and back is BytesMessage is used during the conversion process.
	 * <p>Default is none. Setting this property is optional; if not set, UTF-8 will
	 * be used for decoding any incoming bytes message.
	 * @see #setEncoding
	 */
	public void setEncodingPropertyName(String encodingPropertyName) {
		this.encodingPropertyName = encodingPropertyName;
	}

	@Nullable
	public String getEncodingPropertyName() {
		return this.encodingPropertyName;
	}

	/**
	 * Specify the name of the JMS message property that carries the type id for the
	 * contained object: either a mapped id value or a raw Java class name.
	 * <p>Default is none. <b>NOTE: This property needs to be set in order to allow
	 * for converting from an incoming message to a Java object.</b>
	 * @see #setTypeIdMappings
	 */
	public void setTypeIdPropertyName(String typeIdPropertyName) {
		this.typeIdPropertyName = typeIdPropertyName;
	}

	@Nullable
	public String getTypeIdPropertyName() {
		return this.typeIdPropertyName;
	}

	/**
	 * Specify mappings from type ids to Java classes, if desired.
	 * This allows for synthetic ids in the type id message property,
	 * instead of transferring Java class names.
	 * <p>Default is no custom mappings, i.e. transferring raw Java class names.
	 * @param typeIdMappings a Map with type id values as keys and Java classes as values
	 */
	public void setTypeIdMappings(Map<String, Class<?>> typeIdMappings) {
		this.idClassMappings = new HashMap<>();
		typeIdMappings.forEach((id, clazz) -> {
			this.idClassMappings.put(id, clazz);
			this.classIdMappings.put(clazz, id);
		});
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set a type id for the given payload object on the given JMS Message.
	 * <p>The default implementation consults the configured type id mapping and
	 * sets the resulting value (either a mapped id or the raw Java class name)
	 * into the configured type id message property.
	 * @param object the payload object to set a type id for
	 * @param message the JMS Message to set the type id on
	 * @throws JMSException if thrown by JMS methods
	 * @see #getTypeForMessage(Message)
	 * @see #setTypeIdPropertyName(String)
	 * @see #setTypeIdMappings(Map)
	 */
	protected void setTypeIdOnMessage(Object object, Message message) throws JMSException {
		if (this.typeIdPropertyName != null) {
			String typeId = this.classIdMappings.get(object.getClass());
			if (typeId == null) {
				typeId = object.getClass().getName();
			}
			message.setStringProperty(this.typeIdPropertyName, typeId);
		}
	}

	/**
	 * Determine the type for the given JMS Message,
	 * typically parsing a type id message property.
	 * <p>The default implementation parses the configured type id property name
	 * and consults the configured type id mapping. This can be overridden with
	 * a different strategy, e.g. doing some heuristics based on message origin.
	 * @param message the JMS Message to set the type id on
	 * @throws JMSException if thrown by JMS methods
	 * @see #setTypeIdOnMessage(Object, Message)
	 * @see #setTypeIdPropertyName(String)
	 * @see #setTypeIdMappings(Map)
	 */
	protected Class<?> getTypeForMessage(Message message) throws JMSException {
		String typeId = message.getStringProperty(this.typeIdPropertyName);
		if (typeId == null) {
			throw new MessageConversionException(
					"Could not find type id property [" + this.typeIdPropertyName + "] on message [" +
							message.getJMSMessageID() + "] from destination [" + message.getJMSDestination() + "]");
		}
		Class<?> mappedClass = this.idClassMappings.get(typeId);
		if (mappedClass != null) {
			return mappedClass;
		}
		try {
			return ClassUtils.forName(typeId, this.beanClassLoader);
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Failed to resolve type id [" + typeId + "]", ex);
		}
	}
}
