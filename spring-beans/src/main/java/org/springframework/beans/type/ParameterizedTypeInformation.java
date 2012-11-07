/*
 * Copyright 2011-2013 the original author or authors.
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
package org.springframework.beans.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;

/**
 * Base class for all types that include parameterization of some kind. Crucial as we have to take note of the parent
 * class we will have to resolve generic parameters against.
 * 
 * @author Oliver Gierke
 */
class ParameterizedTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {
	
	private final ParameterizedType type;
	private TypeInformation<?> componentType;

	/**
	 * Creates a new {@link ParameterizedTypeInformation} for the given {@link Type} and parent {@link TypeDiscoverer}.
	 * 
	 * @param type must not be {@literal null}
	 * @param parent must not be {@literal null}
	 */
	public ParameterizedTypeInformation(ParameterizedType type, TypeDiscoverer<?> parent) {
		
		super(type, parent, null);
		this.type = type;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getMapValueType()
	 */
	@Override
	public TypeInformation<?> getMapValueType() {

		if (Map.class.equals(getType())) {
			Type[] arguments = type.getActualTypeArguments();
			return createInfo(arguments[1]);
		}

		Class<?> rawType = getType();

		Set<Type> supertypes = new HashSet<Type>();
		supertypes.add(rawType.getGenericSuperclass());
		supertypes.addAll(Arrays.asList(rawType.getGenericInterfaces()));

		for (Type supertype : supertypes) {
			Class<?> rawSuperType = GenericTypeResolver.resolveType(supertype, getTypeVariableMap());
			if (Map.class.isAssignableFrom(rawSuperType)) {
				ParameterizedType parameterizedSupertype = (ParameterizedType) supertype;
				Type[] arguments = parameterizedSupertype.getActualTypeArguments();
				return createInfo(arguments[1]);
			}
		}

		return super.getMapValueType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getTypeParameters()
	 */
	@Override
	public List<TypeInformation<?>> getTypeArguments() {

		List<TypeInformation<?>> result = new ArrayList<TypeInformation<?>>();

		for (Type argument : type.getActualTypeArguments()) {
			result.add(createInfo(argument));
		}

		return result;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#isAssignableFrom(org.springframework.data.util.TypeInformation)
	 */
	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {

		if (this.equals(target)) {
			return true;
		}

		Class<T> rawType = getType();
		Class<?> rawTargetType = target.getType();

		if (!rawType.isAssignableFrom(rawTargetType)) {
			return false;
		}

		TypeInformation<?> otherTypeInformation = rawType.equals(rawTargetType) ? target : target
				.getSuperTypeInformation(rawType);

		List<TypeInformation<?>> myParameters = getTypeArguments();
		List<TypeInformation<?>> typeParameters = otherTypeInformation.getTypeArguments();

		if (myParameters.size() != typeParameters.size()) {
			return false;
		}

		for (int i = 0; i < myParameters.size(); i++) {
			if (!myParameters.get(i).resolvesTo(typeParameters.get(i))) {
				return false;
			}
		}

		return true;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getComponentType()
	 */
	@Override
	public TypeInformation<?> getComponentType() {

		if (componentType == null) {
			this.componentType = createInfo(type.getActualTypeArguments()[0]);
		}

		return this.componentType;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof ParameterizedTypeInformation)) {
			return false;
		}

		ParameterizedTypeInformation<?> that = (ParameterizedTypeInformation<?>) obj;

		if (this.isResolvedCompletely() && that.isResolvedCompletely()) {
			return this.type.equals(that.type);
		}

		return super.equals(obj);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + (isResolvedCompletely() ? this.type.hashCode() : 0);
	}

	private boolean isResolvedCompletely() {

		Type[] types = type.getActualTypeArguments();

		if (types.length == 0) {
			return false;
		}

		for (Type type : types) {

			TypeInformation<?> info = createInfo(type);

			if (info instanceof ParameterizedTypeInformation) {
				if (!((ParameterizedTypeInformation<?>) info).isResolvedCompletely()) {
					return false;
				}
			}

			if (!(info instanceof ClassTypeInformation)) {
				return false;
			}
		}

		return true;
	}
}
