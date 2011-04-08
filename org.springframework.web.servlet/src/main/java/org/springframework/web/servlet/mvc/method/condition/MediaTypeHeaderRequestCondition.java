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

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;

/**
 * A RequestCondition that for headers that contain {@link org.springframework.http.MediaType MediaTypes}.
 */
class MediaTypeHeaderRequestCondition extends AbstractNameValueCondition<List<MediaType>> {

	public MediaTypeHeaderRequestCondition(String expression) {
		super(expression);
	}

	@Override
	protected List<MediaType> parseValue(String valueExpression) {
		return Collections.unmodifiableList(MediaType.parseMediaTypes(valueExpression));
	}

	@Override
	protected boolean matchName(HttpServletRequest request) {
		return request.getHeader(name) != null;
	}

	@Override
	protected boolean matchValue(HttpServletRequest request) {
		List<MediaType> requestMediaTypes = MediaType.parseMediaTypes(request.getHeader(name));

		for (MediaType mediaType : this.value) {
			for (MediaType requestMediaType : requestMediaTypes) {
				if (mediaType.includes(requestMediaType)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && obj instanceof MediaTypeHeaderRequestCondition) {
			MediaTypeHeaderRequestCondition other = (MediaTypeHeaderRequestCondition) obj;
			return ((this.name.equalsIgnoreCase(other.name)) &&
					(this.value != null ? this.value.equals(other.value) : other.value == null) &&
					this.isNegated == other.isNegated);
		}
		return false;
	}


}
