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
package org.springframework.expression.spel.support;

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.SpelNodeImpl;

/**
 * @author Andy Clement
 * @since 3.0
 */
public class BooleanTypedValue extends TypedValue {

	public static final BooleanTypedValue True = new BooleanTypedValue(true);
	public static final BooleanTypedValue False = new BooleanTypedValue(false);
	
	private BooleanTypedValue(boolean b) {
		super(b,SpelNodeImpl.BOOLEAN_TYPE_DESCRIPTOR);
	}
	
	public static BooleanTypedValue forValue(boolean b) {
		if (b) {
			return True;
		} else {
			return False;
		}
	}
}
