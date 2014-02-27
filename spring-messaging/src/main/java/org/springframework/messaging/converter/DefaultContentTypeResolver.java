/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * A default {@link ContentTypeResolver} that checks the
 * {@link MessageHeaders#CONTENT_TYPE} header or falls back to a default, if a default is
 * configured.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultContentTypeResolver implements ContentTypeResolver {

	private MimeType defaultMimeType;


	/**
	 * Set the default MIME type to use, if the message headers don't have one.
	 * By default this property is set to {@code null}.
	 */
	public void setDefaultMimeType(MimeType defaultMimeType) {
		this.defaultMimeType = defaultMimeType;
	}

	/**
	 * Return the default MIME type to use.
	 */
	public MimeType getDefaultMimeType() {
		return this.defaultMimeType;
	}

	@Override
	public MimeType resolve(MessageHeaders headers) {
		Object mimeType = null;
		if (headers != null) {
			mimeType = headers.get(MessageHeaders.CONTENT_TYPE);
			if(mimeType == null) {
				return this.defaultMimeType;
			}
			if(String.class.isAssignableFrom(mimeType.getClass())) {
				mimeType = MimeType.valueOf((String)mimeType);
			}
			if (!MimeType.class.isAssignableFrom(mimeType.getClass())) {
				throw new IllegalArgumentException("Incorrect type for mime type header. Expected " +
						"[class org.springframework.util.MimeType] or [class java.lang.String] " +
						"but actual type is [" + mimeType.getClass() + "]");
			}
		}
		return (MimeType)mimeType;
	}

	@Override
	public String toString() {
		return "DefaultContentTypeResolver[" + "defaultMimeType=" + this.defaultMimeType + "]";
	}
}
