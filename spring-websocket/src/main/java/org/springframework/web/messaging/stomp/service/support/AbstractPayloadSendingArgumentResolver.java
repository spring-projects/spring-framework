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

package org.springframework.web.messaging.stomp.service.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.messaging.converter.ContentTypeNotSupportedException;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.stomp.service.MessageMethodArgumentResolver;

import reactor.core.Reactor;
import reactor.util.Assert;

/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractPayloadSendingArgumentResolver implements MessageMethodArgumentResolver {

	private final Reactor reactor;

	private final List<MessageConverter<?>> converters;


	public AbstractPayloadSendingArgumentResolver(Reactor reactor, List<MessageConverter<?>> converters) {
		Assert.notNull(reactor, "reactor is required");
		this.reactor = reactor;
		this.converters = (converters != null) ? converters : new ArrayList<MessageConverter<?>>();
	}

	public Reactor getReactor() {
		return this.reactor;
	}

	public List<MessageConverter<?>> getMessageConverters() {
		return this.converters;
	}

	@SuppressWarnings("unchecked")
	protected byte[] convertToPayload(Object content, MediaType contentType)
			throws IOException, ContentTypeNotSupportedException {

		if (content == null) {
			return null;
		}

		Class<? extends Object> clazz = content.getClass();
		if (byte[].class.equals(clazz)) {
			return (byte[]) content;
		}
		else if (!CollectionUtils.isEmpty(this.converters)) {
			for (MessageConverter converter : getMessageConverters()) {
				if (converter.canConvertToPayload(clazz, contentType)) {
					return converter.convertToPayload(content, contentType);

				}
			}
		}
		throw new ContentTypeNotSupportedException(contentType, clazz);
	}

}