/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;

/**
 * Represents a simple property or field reference.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Clark Duplichien
 * @since 3.0
 */
public class PropertyOrFieldReference extends SpelNodeImpl {

	private final boolean nullSafe;

	private final String name;

	private volatile PropertyAccessor cachedReadAccessor;

	private volatile PropertyAccessor cachedWriteAccessor;


	public PropertyOrFieldReference(boolean nullSafe, String propertyOrFieldName, int pos) {
		super(pos);
		this.nullSafe = nullSafe;
		this.name = propertyOrFieldName;
	}


	public boolean isNullSafe() {
		return this.nullSafe;
	}

	public String getName() {
		return this.name;
	}


	@Override
	public ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		return new AccessorLValue(this, state.getActiveContextObject(), state.getEvaluationContext(),
				state.getConfiguration().isAutoGrowNullReferences());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue tv = getValueInternal(state.getActiveContextObject(), state.getEvaluationContext(),
				state.getConfiguration().isAutoGrowNullReferences());
		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (accessorToUse instanceof CompilablePropertyAccessor) {
			CompilablePropertyAccessor accessor = (CompilablePropertyAccessor) accessorToUse;
			this.exitTypeDescriptor = CodeFlow.toDescriptor(accessor.getPropertyType());
		}
		return tv;
	}

	private TypedValue getValueInternal(TypedValue contextObject, EvaluationContext evalContext,
			boolean isAutoGrowNullReferences) throws EvaluationException {

		TypedValue result = readProperty(contextObject, evalContext, this.name);

		// Dynamically create the objects if the user has requested that optional behavior
		if (result.getValue() == null && isAutoGrowNullReferences &&
				nextChildIs(Indexer.class, PropertyOrFieldReference.class)) {
			TypeDescriptor resultDescriptor = result.getTypeDescriptor();
			// Create a new collection or map ready for the indexer
			if (List.class == resultDescriptor.getType()) {
				try {
					if (isWritableProperty(this.name, contextObject, evalContext)) {
						List<?> newList = ArrayList.class.newInstance();
						writeProperty(contextObject, evalContext, this.name, newList);
						result = readProperty(contextObject, evalContext, this.name);
					}
				}
				catch (InstantiationException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_LIST_FOR_INDEXING);
				}
				catch (IllegalAccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_LIST_FOR_INDEXING);
				}
			}
			else if (Map.class == resultDescriptor.getType()) {
				try {
					if (isWritableProperty(this.name,contextObject, evalContext)) {
						Map<?,?> newMap = HashMap.class.newInstance();
						writeProperty(contextObject, evalContext, this.name, newMap);
						result = readProperty(contextObject, evalContext, this.name);
					}
				}
				catch (InstantiationException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_MAP_FOR_INDEXING);
				}
				catch (IllegalAccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_CREATE_MAP_FOR_INDEXING);
				}
			}
			else {
				// 'simple' object
				try {
					if (isWritableProperty(this.name,contextObject, evalContext)) {
						Object newObject  = result.getTypeDescriptor().getType().newInstance();
						writeProperty(contextObject, evalContext, this.name, newObject);
						result = readProperty(contextObject, evalContext, this.name);
					}
				}
				catch (InstantiationException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, result.getTypeDescriptor().getType());
				}
				catch (IllegalAccessException ex) {
					throw new SpelEvaluationException(getStartPosition(), ex,
							SpelMessage.UNABLE_TO_DYNAMICALLY_CREATE_OBJECT, result.getTypeDescriptor().getType());
				}
			}
		}
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object newValue) throws EvaluationException {
		writeProperty(state.getActiveContextObject(), state.getEvaluationContext(), this.name, newValue);
	}

	@Override
	public boolean isWritable(ExpressionState state) throws EvaluationException {
		return isWritableProperty(this.name, state.getActiveContextObject(), state.getEvaluationContext());
	}

	@Override
	public String toStringAST() {
		return this.name;
	}

	/**
	 * Attempt to read the named property from the current context object.
	 * @return the value of the property
	 * @throws EvaluationException if any problem accessing the property or it cannot be found
	 */
	private TypedValue readProperty(TypedValue contextObject, EvaluationContext evalContext, String name)
			throws EvaluationException {

		Object targetObject = contextObject.getValue();
		if (targetObject == null && this.nullSafe) {
			return TypedValue.NULL;
		}

		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (accessorToUse != null) {
			try {
				return accessorToUse.read(evalContext, contextObject.getValue(), name);
			}
			catch (Exception ex) {
				// This is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up...
				this.cachedReadAccessor = null;
			}
		}

		List<PropertyAccessor> accessorsToTry =
				getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
		// Go through the accessors that may be able to resolve it. If they are a cacheable accessor then
		// get the accessor and use it. If they are not cacheable but report they can read the property
		// then ask them to read it
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canRead(evalContext, contextObject.getValue(), name)) {
						if (accessor instanceof ReflectivePropertyAccessor) {
							accessor = ((ReflectivePropertyAccessor) accessor).createOptimalAccessor(
									evalContext, contextObject.getValue(), name);
						}
						this.cachedReadAccessor = accessor;
						return accessor.read(evalContext, contextObject.getValue(), name);
					}
				}
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(ex, SpelMessage.EXCEPTION_DURING_PROPERTY_READ, name, ex.getMessage());
			}
		}
		if (contextObject.getValue() == null) {
			throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL, name);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, name,
					FormatHelper.formatClassNameForMessage(getObjectClass(contextObject.getValue())));
		}
	}

	private void writeProperty(TypedValue contextObject, EvaluationContext evalContext, String name, Object newValue)
			throws EvaluationException {

		if (contextObject.getValue() == null && this.nullSafe) {
			return;
		}

		PropertyAccessor accessorToUse = this.cachedWriteAccessor;
		if (accessorToUse != null) {
			try {
				accessorToUse.write(evalContext, contextObject.getValue(), name, newValue);
				return;
			}
			catch (Exception ex) {
				// This is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up...
				this.cachedWriteAccessor = null;
			}
		}

		List<PropertyAccessor> accessorsToTry =
				getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canWrite(evalContext, contextObject.getValue(), name)) {
						this.cachedWriteAccessor = accessor;
						accessor.write(evalContext, contextObject.getValue(), name, newValue);
						return;
					}
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
						name, ex.getMessage());
			}
		}
		if (contextObject.getValue() == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL, name);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE, name,
					FormatHelper.formatClassNameForMessage(getObjectClass(contextObject.getValue())));
		}
	}

	public boolean isWritableProperty(String name, TypedValue contextObject, EvaluationContext evalContext)
			throws EvaluationException {

		List<PropertyAccessor> accessorsToTry =
				getPropertyAccessorsToTry(contextObject.getValue(), evalContext.getPropertyAccessors());
		if (accessorsToTry != null) {
			for (PropertyAccessor accessor : accessorsToTry) {
				try {
					if (accessor.canWrite(evalContext, contextObject.getValue(), name)) {
						return true;
					}
				}
				catch (AccessException ex) {
					// let others try
				}
			}
		}
		return false;
	}

	/**
	 * Determines the set of property resolvers that should be used to try and access a property
	 * on the specified target type. The resolvers are considered to be in an ordered list,
	 * however in the returned list any that are exact matches for the input target type (as
	 * opposed to 'general' resolvers that could work for any type) are placed at the start of the
	 * list. In addition, there are specific resolvers that exactly name the class in question
	 * and resolvers that name a specific class but it is a supertype of the class we have.
	 * These are put at the end of the specific resolvers set and will be tried after exactly
	 * matching accessors but before generic accessors.
	 * @param contextObject the object upon which property access is being attempted
	 * @return a list of resolvers that should be tried in order to access the property
	 */
	private List<PropertyAccessor> getPropertyAccessorsToTry(Object contextObject, List<PropertyAccessor> propertyAccessors) {
		Class<?> targetType = (contextObject != null ? contextObject.getClass() : null);

		List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
		List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
		for (PropertyAccessor resolver : propertyAccessors) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) {
				// generic resolver that says it can be used for any type
				generalAccessors.add(resolver);
			}
			else if (targetType != null) {
				for (Class<?> clazz : targets) {
					if (clazz == targetType) {
						specificAccessors.add(resolver);
						break;
					}
					else if (clazz.isAssignableFrom(targetType)) {
						generalAccessors.add(resolver);
					}
				}
			}
		}
		List<PropertyAccessor> resolvers = new ArrayList<PropertyAccessor>();
		resolvers.addAll(specificAccessors);
		generalAccessors.removeAll(specificAccessors);
		resolvers.addAll(generalAccessors);
		return resolvers;
	}
	
	@Override
	public boolean isCompilable() {
		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		return (accessorToUse instanceof CompilablePropertyAccessor &&
				((CompilablePropertyAccessor) accessorToUse).isCompilable());
	}
	
	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (!(accessorToUse instanceof CompilablePropertyAccessor)) {
			throw new IllegalStateException("Property accessor is not compilable: " + accessorToUse);
		}
		((CompilablePropertyAccessor) accessorToUse).generateCode(this.name, mv, cf);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}


	private static class AccessorLValue implements ValueRef {

		private final PropertyOrFieldReference ref;

		private final TypedValue contextObject;

		private final EvaluationContext evalContext;

		private final boolean autoGrowNullReferences;

		public AccessorLValue(PropertyOrFieldReference propertyOrFieldReference, TypedValue activeContextObject,
				EvaluationContext evalContext, boolean autoGrowNullReferences) {
			this.ref = propertyOrFieldReference;
			this.contextObject = activeContextObject;
			this.evalContext = evalContext;
			this.autoGrowNullReferences = autoGrowNullReferences;
		}

		@Override
		public TypedValue getValue() {
			TypedValue value =
					this.ref.getValueInternal(this.contextObject, this.evalContext, this.autoGrowNullReferences);
			PropertyAccessor accessorToUse = this.ref.cachedReadAccessor;
			if (accessorToUse instanceof CompilablePropertyAccessor) {
				this.ref.exitTypeDescriptor =
						CodeFlow.toDescriptor(((CompilablePropertyAccessor) accessorToUse).getPropertyType());
			}
			return value;
		}

		@Override
		public void setValue(Object newValue) {
			this.ref.writeProperty(this.contextObject, this.evalContext, this.ref.name, newValue);
		}

		@Override
		public boolean isWritable() {
			return this.ref.isWritableProperty(this.ref.name, this.contextObject, this.evalContext);
		}
	}

}
