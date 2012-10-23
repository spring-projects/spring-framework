/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
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


	static class AccessorLValue implements ValueRef {
		private PropertyOrFieldReference ref;
		private TypedValue contextObject;
		private EvaluationContext eContext;
		private boolean isAutoGrowNullReferences;
		
		public AccessorLValue(
				PropertyOrFieldReference propertyOrFieldReference,
				TypedValue activeContextObject,
				EvaluationContext evaluationContext, boolean isAutoGrowNullReferences) {
			this.ref = propertyOrFieldReference;
			this.contextObject = activeContextObject;
			this.eContext =evaluationContext;
			this.isAutoGrowNullReferences = isAutoGrowNullReferences;
		}

		public TypedValue getValue() {
			return ref.getValueInternal(contextObject,eContext,isAutoGrowNullReferences);
		}

		public void setValue(Object newValue) {
			ref.writeProperty(contextObject,eContext, ref.name, newValue);
		}

		public boolean isWritable() {
			return true;
		}
		
	}
	
	@Override
	public ValueRef getValueRef(ExpressionState state) throws EvaluationException {
//		if (isWritable(state)) {
			return new AccessorLValue(this,state.getActiveContextObject(),state.getEvaluationContext(),state.getConfiguration().isAutoGrowNullReferences());
//		}
//		return super.getLValue(state);
	}
	
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return getValueInternal(state.getActiveContextObject(), state.getEvaluationContext(), state.getConfiguration().isAutoGrowNullReferences());
	}
	
	private TypedValue getValueInternal(TypedValue contextObject, EvaluationContext eContext, boolean isAutoGrowNullReferences) throws EvaluationException {

		TypedValue result = readProperty(contextObject, eContext, this.name);
				
		// Dynamically create the objects if the user has requested that optional behavior
		if (result.getValue() == null && isAutoGrowNullReferences &&
				nextChildIs(Indexer.class, PropertyOrFieldReference.class)) {
			TypeDescriptor resultDescriptor = result.getTypeDescriptor();
			// Creating lists and maps
			if ((resultDescriptor.getType().equals(List.class) || resultDescriptor.getType().equals(Map.class))) {
				// Create a new collection or map ready for the indexer
				if (resultDescriptor.getType().equals(List.class)) {
					try { 
						if (isWritableProperty(this.name,contextObject,eContext)) {
							List newList = ArrayList.class.newInstance();
							writeProperty(contextObject, eContext, this.name, newList);
							result = readProperty(contextObject, eContext, this.name);
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
				else {
					try { 
						if (isWritableProperty(this.name,contextObject,eContext)) {
							Map newMap = HashMap.class.newInstance();
							writeProperty(contextObject, eContext, name, newMap);
							result = readProperty(contextObject, eContext, this.name);
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
			}
			else {
				// 'simple' object
				try { 
					if (isWritableProperty(this.name,contextObject,eContext)) {
						Object newObject  = result.getTypeDescriptor().getType().newInstance();
						writeProperty(contextObject, eContext, name, newObject);
						result = readProperty(contextObject, eContext, this.name);
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
	public void setValue(ExpressionState state, Object newValue) throws SpelEvaluationException {
		writeProperty(state.getActiveContextObject(), state.getEvaluationContext(), this.name, newValue);
	}

	@Override
	public boolean isWritable(ExpressionState state) throws SpelEvaluationException {
		return isWritableProperty(this.name, state.getActiveContextObject(), state.getEvaluationContext());
	}

	@Override
	public String toStringAST() {
		return this.name;
	}

	/**
	 * Attempt to read the named property from the current context object.
	 * @param state the evaluation state
	 * @param name the name of the property
	 * @return the value of the property
	 * @throws SpelEvaluationException if any problem accessing the property or it cannot be found
	 */
	private TypedValue readProperty(TypedValue contextObject, EvaluationContext eContext, String name) throws EvaluationException {
//		TypedValue contextObject = state.getActiveContextObject();
		Object targetObject = contextObject.getValue();

		if (targetObject == null && this.nullSafe) {
			return TypedValue.NULL;
		}

		PropertyAccessor accessorToUse = this.cachedReadAccessor;
		if (accessorToUse != null) {
			try {
				return accessorToUse.read(eContext, contextObject.getValue(), name);
			}
			catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
				this.cachedReadAccessor = null;
			}
		}

		Class<?> contextObjectClass = getObjectClass(contextObject.getValue());
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(contextObjectClass, eContext.getPropertyAccessors());

		// Go through the accessors that may be able to resolve it. If they are a cacheable accessor then
		// get the accessor and use it. If they are not cacheable but report they can read the property
		// then ask them to read it
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canRead(eContext, contextObject.getValue(), name)) {
						if (accessor instanceof ReflectivePropertyAccessor) {
							accessor = ((ReflectivePropertyAccessor) accessor).createOptimalAccessor(
									eContext, contextObject.getValue(), name);
						}
						this.cachedReadAccessor = accessor;
						return accessor.read(eContext, contextObject.getValue(), name);
					}
				}
			}
			catch (AccessException ae) {
				throw new SpelEvaluationException(ae, SpelMessage.EXCEPTION_DURING_PROPERTY_READ, name, ae.getMessage());
			}
		}
		if (contextObject.getValue() == null) {
			throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL, name);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, name,
					FormatHelper.formatClassNameForMessage(contextObjectClass));
		}
	}

	// TODO why is name passed in here?
	private void writeProperty(TypedValue contextObject, EvaluationContext eContext, String name, Object newValue) throws SpelEvaluationException {
		
		if (contextObject.getValue() == null && nullSafe) {
			return;
		}

		PropertyAccessor accessorToUse = this.cachedWriteAccessor;
		if (accessorToUse != null) {
			try {				
				accessorToUse.write(eContext, contextObject.getValue(), name, newValue);
				return;
			}
			catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
				this.cachedWriteAccessor = null;
			}
		}

		Class<?> contextObjectClass = getObjectClass(contextObject.getValue());

		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(contextObjectClass, eContext.getPropertyAccessors());
		if (accessorsToTry != null) {
			try {
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canWrite(eContext, contextObject.getValue(), name)) {
						this.cachedWriteAccessor = accessor;
						accessor.write(eContext, contextObject.getValue(), name, newValue);
						return;
					}
				}
			}
			catch (AccessException ae) {
				throw new SpelEvaluationException(getStartPosition(), ae, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
						name, ae.getMessage());
			}
		}
		if (contextObject.getValue()==null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE_ON_NULL, name);
		}
		else {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.PROPERTY_OR_FIELD_NOT_WRITABLE, name,
					FormatHelper.formatClassNameForMessage(contextObjectClass));
		}
	}

	public boolean isWritableProperty(String name, TypedValue contextObject, EvaluationContext eContext) throws SpelEvaluationException {
		Object contextObjectValue = contextObject.getValue();
		// TypeDescriptor td = state.getActiveContextObject().getTypeDescriptor();
		List<PropertyAccessor> resolversToTry = getPropertyAccessorsToTry(getObjectClass(contextObjectValue), eContext.getPropertyAccessors());
		if (resolversToTry != null) {
			for (PropertyAccessor pfResolver : resolversToTry) {
				try {
					if (pfResolver.canWrite(eContext, contextObjectValue, name)) {
						return true;
					}
				}
				catch (AccessException ae) {
					// let others try
				}
			}
		}
		return false;
	}

	// TODO when there is more time, remove this and use the version in AstUtils
	/**
	 * Determines the set of property resolvers that should be used to try and access a property on the specified target
	 * type. The resolvers are considered to be in an ordered list, however in the returned list any that are exact
	 * matches for the input target type (as opposed to 'general' resolvers that could work for any type) are placed at
	 * the start of the list. In addition, there are specific resolvers that exactly name the class in question and
	 * resolvers that name a specific class but it is a supertype of the class we have. These are put at the end of the
	 * specific resolvers set and will be tried after exactly matching accessors but before generic accessors.
	 * @param targetType the type upon which property access is being attempted
	 * @return a list of resolvers that should be tried in order to access the property
	 */
	private List<PropertyAccessor> getPropertyAccessorsToTry(Class<?> targetType, List<PropertyAccessor> propertyAccessors) {
		List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
		List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
		for (PropertyAccessor resolver : propertyAccessors) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) { // generic resolver that says it can be used for any type
				generalAccessors.add(resolver);
			}
			else {
				if (targetType != null) {
					for (Class<?> clazz : targets) {
						if (clazz == targetType) {
							specificAccessors.add( resolver);
							break;
						}
						else if (clazz.isAssignableFrom(targetType)) { 
							generalAccessors.add(resolver);
						}
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

}
