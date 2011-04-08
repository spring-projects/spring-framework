/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.condition;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * @author Arjen Poutsma
 */
class ConsumesRequestCondition extends AbstractRequestCondition {

	private final MediaType mediaType;

	ConsumesRequestCondition(String mediaType) {
		this.mediaType = MediaType.parseMediaType(mediaType);
	}

	public boolean match(HttpServletRequest request) {
		MediaType contentType;
		if (StringUtils.hasLength(request.getContentType())) {
			contentType = MediaType.parseMediaType(request.getContentType());
		}
		else {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		return this.mediaType.includes(contentType);
	}

	@Override
	public int getWeight() {
		return 1;
	}
}
