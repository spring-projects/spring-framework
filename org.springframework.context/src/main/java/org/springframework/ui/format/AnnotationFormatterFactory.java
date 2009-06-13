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
package org.springframework.ui.format;

import java.lang.annotation.Annotation;

/**
 * A factory that creates {@link Formatter formatters} to format property values on properties annotated with a particular format {@link Annotation}.
 * For example, a <code>CurrencyAnnotationFormatterFactory</code> might create a <code>Formatter</code> that formats a <code>BigDecimal</code> value set on a property annotated with <code>@CurrencyFormat</code>.
 * @author Keith Donald
 * @since 3.0 
 * @param <A> The type of Annotation this factory uses to create Formatter instances
 * @param <T> The type of Object Formatters created by this factory format
 */
public interface AnnotationFormatterFactory<A extends Annotation, T> {
	
	/**
	 * Get the Formatter that will format the value of the property annotated with the provided annotation.
	 * The annotation instance can contain properties that may be used to configure the Formatter that is returned.
	 * @param annotation the annotation instance
	 * @return the Formatter to use to format values of properties annotated with the annotation.
	 */
	Formatter<T> getFormatter(A annotation);	
}