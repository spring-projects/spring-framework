/*
 * Copyright 2002-2010 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package org.springframework.jms.support.converter;

import static org.codehaus.jackson.map.type.TypeFactory.collectionType;
import static org.codehaus.jackson.map.type.TypeFactory.mapType;
import static org.codehaus.jackson.map.type.TypeFactory.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;

import org.codehaus.jackson.type.JavaType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link JavaTypeMapper} using hard coded message
 * properties to store and retrieve the content type information.
 * 
 * @author Mark Pollack
 * @author Sam Nelson
 * @author Dave Syer
 **/
public class DefaultJavaTypeMapper implements JavaTypeMapper, InitializingBean {

	public static final String CLASSID_PROPERTY_NAME = "__TypeId__";
	public static final String CONTENT_CLASSID_PROPERTY_NAME = "__ContentTypeId__";
	public static final String KEY_CLASSID_PROPERTY_NAME = "__KeyTypeId__";

	private Map<String, Class<?>> idClassMapping = new HashMap<String, Class<?>>();
	private Map<Class<?>, String> classIdMapping = new HashMap<Class<?>, String>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JavaType toJavaType(Message message) throws JMSException {
		JavaType classType = getClassIdType(retrieveHeader(message,
				CLASSID_PROPERTY_NAME));
		if (!classType.isContainerType()) {
			return classType;
		}

		JavaType contentClassType = getClassIdType(retrieveHeader(message,
				CONTENT_CLASSID_PROPERTY_NAME));
		if (classType.getKeyType() == null) {
			return collectionType(
					(Class<? extends Collection>) classType.getRawClass(),
					contentClassType);
		}

		JavaType keyClassType = getClassIdType(retrieveHeader(message,
				KEY_CLASSID_PROPERTY_NAME));
		JavaType mapType = mapType(
				(Class<? extends Map>) classType.getRawClass(), keyClassType,
				contentClassType);
		return mapType;

	}

	private JavaType getClassIdType(String classId) {
		if (this.idClassMapping.containsKey(classId)) {
			return type(idClassMapping.get(classId));
		}

		try {
			return type(ClassUtils
					.forName(classId, getClass().getClassLoader()));
		} catch (ClassNotFoundException e) {
			throw new MessageConversionException(
					"failed to resolve class name. Class not found [" + classId
							+ "]", e);
		} catch (LinkageError e) {
			throw new MessageConversionException(
					"failed to resolve class name. Linkage error [" + classId
							+ "]", e);
		}
	}

	private String retrieveHeader(Message message, String headerName)
			throws JMSException {
		String classId = message.getStringProperty(headerName);
		if (classId == null) {
			throw new MessageConversionException(
					"failed to convert Message content. Could not resolve "
							+ headerName + " in header");
		}
		return classId;
	}

	public void setIdClassMapping(Map<String, Class<?>> idClassMapping) {
		this.idClassMapping = idClassMapping;
	}

	public void fromJavaType(JavaType javaType, Message message)
			throws JMSException {
		addHeader(message, CLASSID_PROPERTY_NAME,
				(Class<?>) javaType.getRawClass());

		if (javaType.isContainerType()) {
			addHeader(message, CONTENT_CLASSID_PROPERTY_NAME, javaType
					.getContentType().getRawClass());
		}

		if (javaType.getKeyType() != null) {
			addHeader(message, KEY_CLASSID_PROPERTY_NAME, javaType.getKeyType()
					.getRawClass());
		}
	}

	public void afterPropertiesSet() throws Exception {
		validateIdTypeMapping();
	}

	private void addHeader(Message message, String headerName, Class<?> clazz)
			throws JMSException {
		if (classIdMapping.containsKey(clazz)) {
			message.setStringProperty(headerName, classIdMapping.get(clazz));
		} else {
			message.setStringProperty(headerName, clazz.getName());
		}
	}

	private void validateIdTypeMapping() {
		Map<String, Class<?>> finalIdClassMapping = new HashMap<String, Class<?>>();
		for (Entry<String, Class<?>> entry : idClassMapping.entrySet()) {
			String id = entry.getKey();
			Class<?> clazz = entry.getValue();
			finalIdClassMapping.put(id, clazz);
			classIdMapping.put(clazz, id);
		}
		this.idClassMapping = finalIdClassMapping;
	}

}
