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

package org.springframework.oxm.xstream;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;

/**
 * Generic utility methods for working with XStream. Mainly for internal use within the framework.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
abstract class XStreamUtils {

	/**
	 * Convert the given XStream exception to an appropriate exception from the
	 * {@code org.springframework.oxm} hierarchy.
	 * <p>A boolean flag is used to indicate whether this exception occurs during marshalling or
	 * unmarshalling, since XStream itself does not make this distinction in its exception hierarchy.
	 * @param ex XStream exception that occured
	 * @param marshalling indicates whether the exception occurs during marshalling ({@code true}),
	 * or unmarshalling ({@code false})
	 * @return the corresponding {@code XmlMappingException}
	 */
	public static XmlMappingException convertXStreamException(Exception ex, boolean marshalling) {
		if (ex instanceof StreamException || ex instanceof CannotResolveClassException ||
				ex instanceof ConversionException) {
			if (marshalling) {
				return new MarshallingFailureException("XStream marshalling exception",  ex);
			}
			else {
				return new UnmarshallingFailureException("XStream unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown XStream exception", ex);
		}
	}

}
