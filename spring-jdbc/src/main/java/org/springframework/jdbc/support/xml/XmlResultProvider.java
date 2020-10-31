/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc.support.xml;

import javax.xml.transform.Result;

/**
 * Interface defining handling involved with providing {@code Result}
 * data for XML input.
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see javax.xml.transform.Result
 */
public interface XmlResultProvider {

	/**
	 * Implementations must implement this method to provide the XML content
	 * for the {@code Result}. Implementations will vary depending on
	 * the {@code Result} implementation used.
	 * @param result the {@code Result} object being used to provide the XML input
	 */
	void provideXml(Result result);

}
