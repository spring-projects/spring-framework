/*
 * Copyright 2012-2013 the original author or authors.
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

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * {@link TypeInformation} implementation for {@link WildcardType}s. 
 * 
 * @author Oliver Gierke
 */
public class WildcardTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

	private final TypeInformation<?> type;
	private final boolean isLowerBound;

	/**
	 * Creates a new {@link WildcardTypeInformation}.
	 * 
	 * @param type must not be {@literal null}.
	 * @param parent must not be {@literal null}.
	 */
	protected WildcardTypeInformation(WildcardType type, TypeDiscoverer<?> parent) {

		super(type, parent, null);

		Type[] bounds = type.getLowerBounds();

		if (bounds.length > 0) {
			this.type = createInfo(bounds[0]);
			this.isLowerBound = true;
			return;
		}

		bounds = type.getUpperBounds();

		if (bounds.length > 0) {
			this.type = createInfo(bounds[0]);
			this.isLowerBound = false;
			return;
		}

		throw new IllegalArgumentException(String.format("Cannot detect bounds for type %s!", type));
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.type.TypeDiscoverer#getType()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) this.type.getType();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.type.TypeDiscoverer#getComponentType()
	 */
	@Override
	public TypeInformation<?> getComponentType() {
		return type.getComponentType();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.type.TypeDiscoverer#getMapValueType()
	 */
	@Override
	public TypeInformation<?> getMapValueType() {
		return this.type.getMapValueType();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.type.TypeDiscoverer#resolvesTo(org.springframework.beans.type.TypeInformation)
	 */
	@Override
	public boolean resolvesTo(TypeInformation<?> target) {
		return isAssignableFrom(target);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.type.TypeDiscoverer#isAssignableFrom(org.springframework.beans.type.TypeInformation)
	 */
	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {
		return isLowerBound ? target.isAssignableFrom(type) : type.isAssignableFrom(target);
	}
}
