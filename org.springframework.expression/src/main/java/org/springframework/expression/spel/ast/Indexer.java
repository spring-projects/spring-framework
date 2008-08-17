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
package org.springframework.expression.spel.ast;

import java.util.Collection;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

// TODO support multidimensional arrays
// TODO support correct syntax for multidimensional [][][] and not [,,,]
/**
 * An Indexer can index into some proceeding structure to access a particular piece of it. Supported structures are:
 * strings/collections (lists/sets)/arrays
 * 
 * @author Andy Clement
 */
public class Indexer extends SpelNode {

	public Indexer(Token payload) {
		super(payload);
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object ctx = state.getActiveContextObject();
		Object index = getChild(0).getValue(state);

		// Indexing into a Map
		if (ctx instanceof Map) {
			return ((Map<?, ?>) ctx).get(index);
		}

		int idx = state.toInteger(index);

		if (ctx.getClass().isArray()) {
			return accessArrayElement(ctx, idx);
		} else if (ctx instanceof Collection) {
			Collection<?> c = (Collection<?>) ctx;
			if (idx >= c.size()) {
				throw new SpelException(SpelMessages.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
			}
			int pos = 0;
			for (Object o : c) {
				if (pos == idx) {
					return o;
				}
				pos++;
			}
			// } else if (ctx instanceof Map) {
			// Map<?,?> c = (Map<?,?>) ctx;
			// // This code would allow a key/value pair to be pulled out by index from a map
			// if (idx >= c.size()) {
			// throw new ELException(ELMessages.COLLECTION_INDEX_OUT_OF_BOUNDS,c.size(),idx);
			// }
			// Set<?> keys = c.keySet();
			// int pos = 0;
			// for (Object k : keys) {
			// if (pos==idx) {
			// return new KeyValuePair(k,c.get(k));
			// }
			// pos++;
			// }
		} else if (ctx instanceof String) {
			String ctxString = (String) ctx;
			if (idx >= ctxString.length()) {
				throw new SpelException(SpelMessages.STRING_INDEX_OUT_OF_BOUNDS, ctxString.length(), idx);
			}
			return String.valueOf(ctxString.charAt(idx));
		}
		throw new SpelException(SpelMessages.INDEXING_NOT_SUPPORTED_FOR_TYPE, ctx.getClass().getName());
	}

	private Object accessArrayElement(Object ctx, int idx) throws SpelException {
		Class<?> arrayComponentType = ctx.getClass().getComponentType();
		if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

	private void checkAccess(int arrayLength, int index) throws SpelException {
		if (index > arrayLength) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.ARRAY_INDEX_OUT_OF_BOUNDS, arrayLength, index);
		}
	}

}
