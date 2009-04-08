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

import java.math.BigDecimal;

/**
 * Converts a String to a BigDecimal using {@link BigDecimal#BigDecimal(String).
 * 
 * @author Keith Donald
 */
public class StringToBigDecimal implements Converter<String, BigDecimal> {

	public BigDecimal convert(String source) {
		return new BigDecimal(source);
	}

	public String convertBack(BigDecimal target) {
		return target.toString();
	}

}