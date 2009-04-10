/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Util code shared by collection converters extending from {@link AbstractCollectionConverter}.
 */
class CollectionConversionUtils {

	/**
	 * Get the java.util.Collection implementation class that should be used for the given target collection type.
	 * @param targetCollectionType the target collection type, may be an interface
	 * @return the collection impl to use
	 */
	public static Class<?> getImpl(Class<?> targetCollectionType) {
		if (targetCollectionType.isInterface()) {
			if (List.class.equals(targetCollectionType)) {
				return ArrayList.class;
			} else if (Set.class.equals(targetCollectionType)) {
				return LinkedHashSet.class;
			} else if (SortedSet.class.equals(targetCollectionType)) {
				return TreeSet.class;
			} else if (Collection.class.equals(targetCollectionType)) {
				return ArrayList.class;
			} else {
				throw new IllegalArgumentException("Unsupported collection interface [" + targetCollectionType.getName() + "]");
			}
		} else {
			return targetCollectionType;
		}
	}

}
