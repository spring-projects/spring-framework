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
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;

public class CutProcessor implements DataProcessor {

	/** Cut a piece out of a collection - the arguments are from (inclusive) to (exclusive) */
	public Object process(Collection<?> input, Object[] arguments, ExpressionState state) throws SpelException {
		if (!(arguments[0] instanceof Integer && arguments[1] instanceof Integer)) {
			throw new SpelException(SpelMessages.CUT_ARGUMENTS_MUST_BE_INTS, arguments[0].getClass().getName(),
					arguments[1].getClass().getName());
		}
		int first = ((Integer) arguments[0]).intValue();
		int last = ((Integer) arguments[1]).intValue();
		List<Object> result = new ArrayList<Object>();
		int pos = 0;
		if (first < last) {
			for (Object o : input) {
				if (pos >= first && pos <= last)
					result.add(o);
				pos++;
			}
		} else {
			for (Object o : input) {
				if (pos >= last && pos <= first)
					result.add(0, o);
				pos++;
			}
		}
		return result;
	}

}
