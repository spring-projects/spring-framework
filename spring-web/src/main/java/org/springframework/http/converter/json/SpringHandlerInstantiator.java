/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.json;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Allows for creating Jackson ({@link JsonSerializer}, {@link JsonDeserializer},
 * {@link KeyDeserializer}, {@link TypeResolverBuilder}, {@link TypeIdResolver})
 * beans with autowiring against a Spring {@link ApplicationContext}.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.1.3
 * @see Jackson2ObjectMapperBuilder#handlerInstantiator(HandlerInstantiator)
 * @see ApplicationContext#getAutowireCapableBeanFactory()
 * @see HandlerInstantiator
 */
public class SpringHandlerInstantiator extends HandlerInstantiator {

	private final AutowireCapableBeanFactory beanFactory;


	/**
	 * Create a new SpringHandlerInstantiator for the given BeanFactory.
	 * @param beanFactory the target BeanFactory
	 */
	public SpringHandlerInstantiator(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> implClass) {
		return (JsonDeserializer<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> implClass) {
		return (KeyDeserializer) this.beanFactory.createBean(implClass);
	}

	@Override
	public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> implClass) {
		return (JsonSerializer<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
		return (TypeResolverBuilder<?>) this.beanFactory.createBean(implClass);
	}

	@Override
	public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
		return (TypeIdResolver) this.beanFactory.createBean(implClass);
	}

}
