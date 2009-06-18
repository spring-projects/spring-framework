/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.message;

import java.util.Locale;

import org.springframework.context.MessageSource;

/**
 * A factory for a localized message.
 * TODO - consider putting this abstraction together with MessageSource; does it need to be in its own package?
 * @author Keith Donald
 * @since 3.0
 * @see MessageSource
 */
public interface MessageResolver {

	/**
	 * Resolve the message from the message source for the locale.
	 * @param messageSource the message source, an abstraction for a resource bundle
	 * @param locale the locale of this request
	 * @return the resolved message
	 * @throws MessageResolutionException if a resolution failure occurs
	 */
	public String resolveMessage(MessageSource messageSource, Locale locale);
}
