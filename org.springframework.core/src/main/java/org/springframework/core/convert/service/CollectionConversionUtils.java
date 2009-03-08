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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class CollectionConversionUtils {

	static Class<?> getImpl(Class<?> targetClass) {
		if (targetClass.isInterface()) {
			if (List.class.equals(targetClass)) {
				return ArrayList.class;
			} else if (Set.class.equals(targetClass)) {
				return LinkedHashSet.class;
			} else if (SortedSet.class.equals(targetClass)) {
				return TreeSet.class;
			} else {
				throw new IllegalArgumentException("Unsupported collection interface [" + targetClass.getName() + "]");
			}
		} else {
			return targetClass;
		}
	}

}
