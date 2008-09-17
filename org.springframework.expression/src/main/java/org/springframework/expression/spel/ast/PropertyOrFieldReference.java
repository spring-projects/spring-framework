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

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.expression.AccessException;
import org.springframework.expression.CacheablePropertyAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.PropertyReaderExecutor;
import org.springframework.expression.PropertyWriterExecutor;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.expression.spel.internal.Utils;

/**
 * Represents a simple property or field reference.
 * 
 * @author Andy Clement
 */
public class PropertyOrFieldReference extends SpelNode {

	public static boolean useCaching = true;

	private final Object name;
	private PropertyReaderExecutor cachedReaderExecutor;
	private PropertyWriterExecutor cachedWriterExecutor;

	public PropertyOrFieldReference(Token payload) {
		super(payload);
		name = payload.getText();
	}

	@Override
	public Object getValue(ExpressionState state) throws SpelException {
		return readProperty(state, name);
	}

	@Override
	public void setValue(ExpressionState state, Object newValue) throws SpelException {
		writeProperty(state, name, newValue);
	}

	@Override
	public boolean isWritable(ExpressionState state) throws SpelException {
		return isWritableProperty(name, state);
	}

	@Override
	public String toStringAST() {
		return name.toString();
	}

	/**
	 * Attempt to read the named property from the current context object.
	 * 
	 * @param state the evaluation state
	 * @param name the name of the property
	 * @return the value of the property
	 * @throws SpelException if any problem accessing the property or it cannot be found
	 */
	private Object readProperty(ExpressionState state, Object name) throws SpelException {
		Object contextObject = state.getActiveContextObject();
		EvaluationContext eContext = state.getEvaluationContext();

		if (cachedReaderExecutor != null) {
			try {
				return cachedReaderExecutor.execute(state.getEvaluationContext(), contextObject);
			} catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
			}
		}

		Class<?> contextObjectClass = getObjectClass(contextObject);

		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(contextObjectClass, state);

		// Go through the accessors that may be able to resolve it. If they are a cacheable accessor then
		// get the accessor and use it. If they are not cacheable but report they can read the property
		// then ask them to read it
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor instanceof CacheablePropertyAccessor) {
						cachedReaderExecutor = ((CacheablePropertyAccessor) accessor).getReaderAccessor(eContext,
								contextObject, name);
						if (cachedReaderExecutor != null) {
							try {
								return cachedReaderExecutor.execute(state.getEvaluationContext(), contextObject);
							} catch (AccessException ae) {
								cachedReaderExecutor = null;
								throw ae;
							} finally {
								if (!useCaching) {
									cachedReaderExecutor = null;
								}
							}
						}
					} else {
						if (accessor.canRead(eContext, contextObject, name)) {
							Object value = accessor.read(eContext, contextObject, name);
							return value;
						}
					}
				}
			} catch (AccessException ae) {
				throw new SpelException(ae, SpelMessages.EXCEPTION_DURING_PROPERTY_READ, name, ae.getMessage());
			}
		}
		throw new SpelException(SpelMessages.PROPERTY_OR_FIELD_NOT_FOUND, name, Utils
				.formatClassnameForMessage(contextObjectClass));
	}

	private void writeProperty(ExpressionState state, Object name, Object newValue) throws SpelException {
		Object contextObject = state.getActiveContextObject();
		EvaluationContext eContext = state.getEvaluationContext();

		if (cachedWriterExecutor != null) {
			try {
				cachedWriterExecutor.execute(state.getEvaluationContext(), contextObject, newValue);
				return;
			} catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
			}
		}

		Class<?> contextObjectClass = getObjectClass(contextObject);

		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(contextObjectClass, state);
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor instanceof CacheablePropertyAccessor) {
						cachedWriterExecutor = ((CacheablePropertyAccessor) accessor).getWriterAccessor(eContext,
								contextObject, name);
						if (cachedWriterExecutor != null) {
							try {
								cachedWriterExecutor.execute(state.getEvaluationContext(), contextObject, newValue);
								return;
							} catch (AccessException ae) {
								cachedWriterExecutor = null;
								throw ae;
							} finally {
								if (!useCaching) {
									cachedWriterExecutor = null;
								}
							}
						}
					} else {
						if (accessor.canWrite(eContext, contextObject, name)) {
							accessor.write(eContext, contextObject, name, newValue);
							return;
						}
					}
				}
			} catch (AccessException ae) {
				throw new SpelException(getCharPositionInLine(), ae, SpelMessages.EXCEPTION_DURING_PROPERTY_WRITE,
						name, ae.getMessage());
			}
		}
		throw new SpelException(SpelMessages.PROPERTY_OR_FIELD_SETTER_NOT_FOUND, name, Utils
				.formatClassnameForMessage(contextObjectClass));
	}

	public boolean isWritableProperty(Object name, ExpressionState state) throws SpelException {
		Object contextObject = state.getActiveContextObject();
		EvaluationContext eContext = state.getEvaluationContext();
		if (contextObject == null) {
			throw new SpelException(SpelMessages.ATTEMPTED_PROPERTY_FIELD_REF_ON_NULL_CONTEXT_OBJECT, name);
		}
		List<PropertyAccessor> resolversToTry = getPropertyAccessorsToTry(
				(contextObject instanceof Class) ? ((Class<?>) contextObject) : contextObject.getClass(), state);
		if (resolversToTry != null) {
			for (PropertyAccessor pfResolver : resolversToTry) {
				try {
					if (pfResolver.canWrite(eContext, contextObject, name)) {
						return true;
					}
				} catch (AccessException ae) {
					// let others try
				}
			}
		}
		return false;
	}

	/**
	 * Determines the set of property resolvers that should be used to try and access a property on the specified target
	 * type. The resolvers are considered to be in an ordered list, however in the returned list any that are exact
	 * matches for the input target type (as opposed to 'general' resolvers that could work for any type) are placed at
	 * the start of the list. In addition, there are specific resolvers that exactly name the class in question and
	 * resolvers that name a specific class but it is a supertype of the class we have. These are put at the end of the
	 * specific resolvers set and will be tried after exactly matching accessors but before generic accessors.
	 * 
	 * @param targetType the type upon which property access is being attempted
	 * @return a list of resolvers that should be tried in order to access the property
	 */
	private List<PropertyAccessor> getPropertyAccessorsToTry(Class<?> targetType, ExpressionState state) {
		List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
		List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
		for (PropertyAccessor resolver : state.getPropertyAccessors()) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) { // generic resolver that says it can be used for any type
				generalAccessors.add(resolver);
			} else {
				if (targetType != null) {
					int pos = 0;
					for (int i = 0; i < targets.length; i++) {
						Class<?> clazz = targets[i];
						if (clazz == targetType) { // put exact matches on the front to be tried first?
							specificAccessors.add(pos++, resolver);
						} else if (clazz.isAssignableFrom(targetType)) { // put supertype matches at the end of the
																			// specificAccessor list
							generalAccessors.add(resolver);
						}
					}
				}
			}
		}
		List<PropertyAccessor> resolvers = new ArrayList<PropertyAccessor>();
		resolvers.addAll(specificAccessors);
		resolvers.addAll(generalAccessors);
		return resolvers;
	}

}
