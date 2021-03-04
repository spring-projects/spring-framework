/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class AopNamespaceHandlerAdviceTypeTests {

	@Test
	public void testParsingOfAdviceTypes() {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-ok.xml", getClass());
	}

	@Test
	public void testParsingOfAdviceTypesWithError() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-error.xml", getClass()))
			.matches(ex -> ex.contains(SAXParseException.class));
	}

}
