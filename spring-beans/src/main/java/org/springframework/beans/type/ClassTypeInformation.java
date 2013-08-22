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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link TypeInformation} for a plain {@link Class}.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ClassTypeInformation<S> extends TypeDiscoverer<S> {

	public static final TypeInformation<Collection> COLLECTION = new ClassTypeInformation(Collection.class);
	public static final TypeInformation<List> LIST = new ClassTypeInformation(List.class);
	public static final TypeInformation<Set> SET = new ClassTypeInformation(Set.class);
	public static final TypeInformation<Map> MAP = new ClassTypeInformation(Map.class);
	public static final TypeInformation<Object> OBJECT = new ClassTypeInformation(Object.class);

	private static final Map<Class<?>, Reference<TypeInformation<?>>> CACHE = new ConcurrentReferenceHashMap<Class<?>, Reference<TypeInformation<?>>>();

	static {
		for (TypeInformation<?> info : Arrays.asList(COLLECTION, LIST, SET, MAP, OBJECT)) {
			CACHE.put(info.getType(), new WeakReference<TypeInformation<?>>(info));
		}
	}

	private final Class<S> type;

	/**
	 * Simple factory method to easily create new instances of {@link ClassTypeInformation}.
	 * 
	 * @param <S>
	 * @param type
	 * @return
	 */
	public static <S> TypeInformation<S> from(Class<S> type) {

		type = type == null ? (Class<S>) Object.class : type;

		Reference<TypeInformation<?>> cachedReference = CACHE.get(type);
		TypeInformation<?> cachedTypeInfo = cachedReference == null ? null : cachedReference.get();

		if (cachedTypeInfo != null) {
			return (TypeInformation<S>) cachedTypeInfo;
		}

		TypeInformation<S> result = new ClassTypeInformation<S>(type);
		CACHE.put(type, new WeakReference<TypeInformation<?>>(result));
		return result;
	}

	/**
	 * Creates a {@link TypeInformation} from the given method's return type.
	 * 
	 * @param method
	 * @return
	 */
	public static <S> TypeInformation<S> fromReturnTypeOf(Method method) {
		return new ClassTypeInformation(method.getDeclaringClass()).createInfo(method.getGenericReturnType());
	}

	/**
	 * Creates {@link ClassTypeInformation} for the given type.
	 * 
	 * @param type
	 */
	ClassTypeInformation(Class<S> type) {
		this(type, GenericTypeResolver.getTypeVariableMap(type));
	}

	ClassTypeInformation(Class<S> type, Map<TypeVariable, Type> typeVariableMap) {
		super(ClassUtils.getUserClass(type), typeVariableMap);
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#getType()
	 */
	@Override
	public Class<S> getType() {
		return type;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#isAssignableFrom(org.springframework.data.util.TypeInformation)
	 */
	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {
		return getType().isAssignableFrom(target.getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.type.TypeDiscoverer#isRawType()
	 */
	@Override
	public boolean isRawType() {

		List<Type> genericTypes = new ArrayList<Type>();
		genericTypes.addAll(Arrays.asList(type.getGenericInterfaces()));
		genericTypes.add(type.getGenericSuperclass());

		for (Type genericType : genericTypes) {
			if (genericType instanceof ParameterizedType) {
				for (Type arguments : ((ParameterizedType) genericType).getActualTypeArguments()) {
					if(!(arguments instanceof Class)) {
						return true;
					}
				}

				return false;
			}
		}

		return true;
	}
}
