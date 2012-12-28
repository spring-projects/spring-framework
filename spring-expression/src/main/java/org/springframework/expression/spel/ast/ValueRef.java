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

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * Represents a reference to a value.  With a reference it is possible to get or set the
 * value. Passing around value references rather than the values themselves can avoid
 * incorrect duplication of operand evaluation. For example in 'list[index++]++' without a
 * value reference for 'list[index++]' it would be necessary to evaluate list[index++]
 * twice (once to get the value, once to determine where the value goes) and that would
 * double increment index.
 *
 * @author Andy Clement
 * @since 3.2
 */
public interface ValueRef {

	/**
	 * A ValueRef for the null value.
	 */
	static class NullValueRef implements ValueRef {

		static NullValueRef instance = new NullValueRef();

		@Override
		public TypedValue getValue() {
			return TypedValue.NULL;
		}

		@Override
		public void setValue(Object newValue) {
			// The exception position '0' isn't right but the overhead of creating
			// instances of this per node (where the node is solely for error reporting)
			// would be unfortunate.
			throw new SpelEvaluationException(0,SpelMessage.NOT_ASSIGNABLE,"null");
		}

		@Override
		public boolean isWritable() {
			return false;
		}

	}

	/**
	 * A ValueRef holder for a single value, which cannot be set.
	 */
	static class TypedValueHolderValueRef implements ValueRef {

		private TypedValue typedValue;
		private SpelNodeImpl node; // used only for error reporting

		public TypedValueHolderValueRef(TypedValue typedValue,SpelNodeImpl node) {
			this.typedValue = typedValue;
			this.node = node;
		}

		@Override
		public TypedValue getValue() {
			return typedValue;
		}

		@Override
		public void setValue(Object newValue) {
			throw new SpelEvaluationException(
					node.pos, SpelMessage.NOT_ASSIGNABLE, node.toStringAST());
		}

		@Override
		public boolean isWritable() {
			return false;
		}

	}

	/**
	 * Returns the value this ValueRef points to, it should not require expression
	 * component re-evaluation.
	 * @return the value
	 */
	TypedValue getValue();

	/**
	 * Sets the value this ValueRef points to, it should not require expression component
	 * re-evaluation.
	 * @param newValue the new value
	 */
	void setValue(Object newValue);

	/**
	 * Indicates whether calling setValue(Object) is supported.
	 * @return true if setValue() is supported for this value reference.
	 */
	boolean isWritable();
}
