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
package org.springframework.core.convert.converter;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;

/**
 * A super converter that can also convert a target object of type T to a source of class hierarchy S.
 * <p>
 * Implementations of this interface are thread-safe and can be shared. Converters are typically registered with and
 * accessed through a {@link ConversionService}.
 * </p>
 * @author Keith Donald
 */
public interface SuperTwoWayConverter<S, T> extends SuperConverter<S, T> {

	/**
	 * Convert the target of type T to an instance of RS.
	 * @param target the target object to convert, whose class must be equal to or a subclass of T
	 * @param sourceClass the requested source class to convert to, which must be equal to S or extend from S
	 * @return the converted object, which must be an instance of RS
	 * @throws Exception an exception occurred performing the conversion; may be any checked exception, the conversion
	 * system will handle wrapping the failure in a {@link ConversionException} that provides a consistent type
	 * conversion error context
	 */
	public <RS extends S> RS convertBack(T target, Class<RS> sourceClass) throws Exception;

}