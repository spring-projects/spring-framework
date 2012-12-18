/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.http;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link MediaType}
 * descriptors, to automatically convert {@code String} specifications
 * (e.g. {@code "text/html"}) to {@code MediaType} properties.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MediaType
 */
public class MediaTypeEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			setValue(MediaType.parseMediaType(text));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		MediaType mediaType = (MediaType) getValue();
		return (mediaType != null ? mediaType.toString() : "");
	}

}
