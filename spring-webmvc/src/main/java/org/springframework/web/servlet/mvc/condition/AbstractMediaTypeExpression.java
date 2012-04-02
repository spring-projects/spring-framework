/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Supports media type expressions as described in:
 * {@link RequestMapping#consumes()} and {@link RequestMapping#produces()}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
abstract class AbstractMediaTypeExpression implements Comparable<AbstractMediaTypeExpression>, MediaTypeExpression {

	protected final Log logger = LogFactory.getLog(getClass());

	private final MediaType mediaType;

	private final boolean isNegated;

	AbstractMediaTypeExpression(String expression) {
		if (expression.startsWith("!")) {
			isNegated = true;
			expression = expression.substring(1);
		}
		else {
			isNegated = false;
		}
		this.mediaType = MediaType.parseMediaType(expression);
	}

	AbstractMediaTypeExpression(MediaType mediaType, boolean negated) {
		this.mediaType = mediaType;
		isNegated = negated;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public boolean isNegated() {
		return isNegated;
	}

	public final boolean match(HttpServletRequest request) {
		try {
			boolean match = matchMediaType(request);
			return !isNegated ? match : !match;
		}
		catch (IllegalArgumentException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not parse media type header: " + ex.getMessage());
			}
			return false;
		}
	}

	protected abstract boolean matchMediaType(HttpServletRequest request);

	public int compareTo(AbstractMediaTypeExpression other) {
		return MediaType.SPECIFICITY_COMPARATOR.compare(this.getMediaType(), other.getMediaType());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass().equals(obj.getClass())) {
			AbstractMediaTypeExpression other = (AbstractMediaTypeExpression) obj;
			return (this.mediaType.equals(other.mediaType)) && (this.isNegated == other.isNegated);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mediaType.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (isNegated) {
			builder.append('!');
		}
		builder.append(mediaType.toString());
		return builder.toString();
	}

}