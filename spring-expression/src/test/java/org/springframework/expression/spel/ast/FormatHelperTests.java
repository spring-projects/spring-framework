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

package org.springframework.expression.spel.ast;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.core.convert.TypeDescriptor;

import static org.junit.Assert.*;

/**
 * @author Andy Wilkinson
 */
public class FormatHelperTests {

	@Test
	public void formatMethodWithSingleArgumentForMessage() {
		String message = FormatHelper.formatMethodForMessage("foo", Arrays.asList(TypeDescriptor.forObject("a string")));
		assertEquals("foo(java.lang.String)", message);
	}

	@Test
	public void formatMethodWithMultipleArgumentsForMessage() {
		String message = FormatHelper.formatMethodForMessage("foo", Arrays.asList(TypeDescriptor.forObject("a string"), TypeDescriptor.forObject(Integer.valueOf(5))));
		assertEquals("foo(java.lang.String,java.lang.Integer)", message);
	}

}
