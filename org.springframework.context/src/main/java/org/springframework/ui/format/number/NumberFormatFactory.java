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
package org.springframework.ui.format.number;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A factory for {@link NumberFormat} objects. Conceals the complexity associated with configuring, constructing, and/or
 * caching number format instances.
 * 
 * @author Keith Donald
 * @since 3.0
 */
abstract class NumberFormatFactory {

	/**
	 * Factory method that returns a fully-configured {@link NumberFormat} instance to use to format an object for
	 * display.
	 * @return the number format
	 */
	public abstract NumberFormat getNumberFormat(Locale locale);

}