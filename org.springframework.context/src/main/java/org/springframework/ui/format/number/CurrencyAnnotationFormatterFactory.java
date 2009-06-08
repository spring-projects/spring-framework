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

import java.math.BigDecimal;

import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;

/**
 * Returns a CurrencyFormatter for properties annotated with the {@link CurrencyFormat} annotation.
 * @author Keith Donald
 */
public class CurrencyAnnotationFormatterFactory implements AnnotationFormatterFactory<CurrencyFormat, BigDecimal> {
	public Formatter<BigDecimal> getFormatter(CurrencyFormat annotation) {
		return new CurrencyFormatter();
	}
}
