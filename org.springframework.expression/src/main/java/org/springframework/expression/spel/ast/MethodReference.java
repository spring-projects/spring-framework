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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.internal.Utils;
import org.springframework.expression.spel.processors.AverageProcessor;
import org.springframework.expression.spel.processors.CountProcessor;
import org.springframework.expression.spel.processors.CutProcessor;
import org.springframework.expression.spel.processors.DataProcessor;
import org.springframework.expression.spel.processors.DistinctProcessor;
import org.springframework.expression.spel.processors.MaxProcessor;
import org.springframework.expression.spel.processors.MinProcessor;
import org.springframework.expression.spel.processors.NonNullProcessor;
import org.springframework.expression.spel.processors.SortProcessor;

public class MethodReference extends SpelNode {

	private static Map<String, DataProcessor> registeredProcessers = new HashMap<String, DataProcessor>();

	private final String name;
	private MethodExecutor fastInvocationAccessor; // TODO should this be nulled if executing in a different context or is it OK to keep?

	static {
		registeredProcessers.put("count", new CountProcessor());
		registeredProcessers.put("max", new MaxProcessor());
		registeredProcessers.put("min", new MinProcessor());
		registeredProcessers.put("average", new AverageProcessor());
		registeredProcessers.put("sort", new SortProcessor());
		registeredProcessers.put("nonnull", new NonNullProcessor());
		registeredProcessers.put("distinct", new DistinctProcessor());
		registeredProcessers.put("cut", new CutProcessor());
	}

	public MethodReference(Token payload) {
		super(payload);
		name = payload.getText();
	}

	@SuppressWarnings("unchecked")
	private Object invokeDataProcessor(Object[] arguments, ExpressionState state) throws EvaluationException {
		DataProcessor processor = registeredProcessers.get(name);

		Object target = state.getActiveContextObject();

		// Prepare the input, translating arrays to lists
		boolean wasArray = false;
		Class<?> arrayElementType = null;
		Collection<Object> dataToProcess = null;
		if (target instanceof Collection) {
			dataToProcess = (Collection<Object>) target;
		} else {
			wasArray = true;
			arrayElementType = target.getClass().getComponentType();
			if (arrayElementType.equals(Integer.TYPE)) {
				int[] data = (int[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Byte.TYPE)) {
				byte[] data = (byte[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Character.TYPE)) {
				char[] data = (char[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Double.TYPE)) {
				double[] data = (double[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Long.TYPE)) {
				long[] data = (long[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Float.TYPE)) {
				float[] data = (float[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Short.TYPE)) {
				short[] data = (short[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else if (arrayElementType.equals(Boolean.TYPE)) {
				boolean[] data = (boolean[]) target;
				dataToProcess = new ArrayList<Object>();
				for (int i = 0; i < data.length; i++) {
					dataToProcess.add(data[i]);
				}
			} else {
				dataToProcess = Arrays.asList((Object[]) target);
			}
		}

		Object result = processor.process(dataToProcess, arguments, state);

		// Convert the result back if necessary
		if (wasArray && (result instanceof Collection)) {
			Collection c = (Collection) result;

			if (arrayElementType.equals(Integer.TYPE)) {
				int[] newArray = (int[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Integer) i).intValue();
				return newArray;
			} else if (arrayElementType.equals(Byte.TYPE)) {
				byte[] newArray = (byte[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Byte) i).byteValue();
				return newArray;
			} else if (arrayElementType.equals(Character.TYPE)) {
				char[] newArray = (char[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Character) i).charValue();
				return newArray;
			} else if (arrayElementType.equals(Double.TYPE)) {
				double[] newArray = (double[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Double) i).doubleValue();
				return newArray;
			} else if (arrayElementType.equals(Long.TYPE)) {
				long[] newArray = (long[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Long) i).longValue();
				return newArray;
			} else if (arrayElementType.equals(Float.TYPE)) {
				float[] newArray = (float[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Float) i).floatValue();
				return newArray;
			} else if (arrayElementType.equals(Short.TYPE)) {
				short[] newArray = (short[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Short) i).shortValue();
				return newArray;
			} else if (arrayElementType.equals(Boolean.TYPE)) {
				boolean[] newArray = (boolean[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c)
					newArray[idx++] = ((Boolean) i).booleanValue();
				return newArray;
			} else {
				Object[] newArray = (Object[]) Array.newInstance(arrayElementType, c.size());
				int idx = 0;
				for (Object i : c) {
					newArray[idx++] = i;
				}
				return newArray;
			}
		}
		return result;
	}

	@Override
	public Object getValue(ExpressionState state) throws EvaluationException {
		Object currentContext = state.getActiveContextObject();
		Object[] arguments = new Object[getChildCount()];
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = getChild(i).getValue(state);
		}
		if (currentContext == null) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.ATTEMPTED_METHOD_CALL_ON_NULL_CONTEXT_OBJECT,
					formatMethodForMessage(name, getTypes(arguments)));
		}

		boolean usingProcessor = registeredProcessers.containsKey(name);
		if ((currentContext.getClass().isArray() && usingProcessor)
				|| (currentContext instanceof Collection && registeredProcessers.containsKey(name))) {
			return invokeDataProcessor(arguments, state);
		}

		if (fastInvocationAccessor != null) {
			try {
				return fastInvocationAccessor.execute(state.getEvaluationContext(), state.getActiveContextObject(),
						arguments);
			} catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change, let's get a new one and retry before
				// giving up
			}
		}
		// either there was no accessor or it no longer existed
		fastInvocationAccessor = findAccessorForMethod(name, getTypes(arguments), state);
		try {
			return fastInvocationAccessor.execute(state.getEvaluationContext(), state.getActiveContextObject(),
					arguments);
		} catch (AccessException ae) {
			ae.printStackTrace();
			throw new SpelException(getCharPositionInLine(), ae, SpelMessages.EXCEPTION_DURING_METHOD_INVOCATION, name,
					state.getActiveContextObject().getClass().getName(), ae.getMessage());
		}
	}

	private Class<?>[] getTypes(Object... arguments) {
		if (arguments == null)
			return null;
		Class<?>[] argumentTypes = new Class[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			argumentTypes[i] = arguments[i].getClass();
		}
		return argumentTypes;
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Produce a nice string for a given method name with specified arguments.
	 * @param name the name of the method
	 * @param argumentTypes the types of the arguments to the method
	 * @return nicely formatted string, eg. foo(String,int)
	 */
	private String formatMethodForMessage(String name, Class<?>... argumentTypes) {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		if (argumentTypes != null) {
			for (int i = 0; i < argumentTypes.length; i++) {
				if (i > 0)
					sb.append(",");
				sb.append(argumentTypes[i].getClass());
			}
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}

	public final MethodExecutor findAccessorForMethod(String name, Class<?>[] argumentTypes, ExpressionState state)
			throws SpelException {
		Object contextObject = state.getActiveContextObject();
		EvaluationContext eContext = state.getEvaluationContext();
		if (contextObject == null) {
			throw new SpelException(SpelMessages.ATTEMPTED_METHOD_CALL_ON_NULL_CONTEXT_OBJECT, Utils
					.formatMethodForMessage(name, argumentTypes));
		}
		List<MethodResolver> mResolvers = eContext.getMethodResolvers();
		if (mResolvers != null) {
			for (MethodResolver methodResolver : mResolvers) {
				try {
					MethodExecutor cEx = methodResolver.resolve(state.getEvaluationContext(), contextObject, name,
							argumentTypes);
					if (cEx != null)
						return cEx;
				} catch (AccessException e) {
					Throwable cause = e.getCause();
					if (cause instanceof SpelException) {
						throw (SpelException) cause;
					} else {
						throw new SpelException(cause, SpelMessages.PROBLEM_LOCATING_METHOD, name, contextObject.getClass());
					}
				}
			}
		}
		throw new SpelException(SpelMessages.METHOD_NOT_FOUND, Utils.formatMethodForMessage(name, argumentTypes), Utils
				.formatClassnameForMessage(contextObject instanceof Class ? ((Class<?>) contextObject) : contextObject
						.getClass()));
		// (contextObject instanceof Class ? ((Class<?>) contextObject).getName() : contextObject.getClass()
		// .getName()));
	}
}
