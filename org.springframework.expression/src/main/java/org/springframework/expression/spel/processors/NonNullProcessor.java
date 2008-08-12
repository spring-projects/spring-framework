/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.ExpressionState;

/**
 * The NonNull Processor returns a new collection containing all non-null entries from the input collection.
 * 
 * @author Andy Clement
 * 
 */
public class NonNullProcessor implements DataProcessor {

	public Object process(Collection<?> input, Object[] arguments, ExpressionState state) throws SpelException {
		List<Object> l = new ArrayList<Object>();
		for (Object o : input) {
			if (o != null)
				l.add(o);
		}
		return l;
	}

}
