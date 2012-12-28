/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.enums;

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.util.comparator.CompoundComparator;
import org.springframework.util.comparator.NullSafeComparator;

/**
 * An interface for objects that represent a labeled enumeration.
 * Each such enum instance has the following characteristics:
 *
 * <ul>
 * <li>A type that identifies the enum's class.
 * For example: {@code com.mycompany.util.FileFormat}.</li>
 *
 * <li>A code that uniquely identifies the enum within the context of its type.
 * For example: &quot;CSV&quot;. Different classes of codes are possible
 * (e.g., Character, Integer, String).</li>
 *
 * <li>A descriptive label. For example: "the CSV File Format".</li>
 * </ul>
 *
 * @author Keith Donald
 * @since 1.2.2
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
public interface LabeledEnum extends Comparable, Serializable {

	/**
	 * Return this enumeration's type.
	 */
	Class getType();

	/**
	 * Return this enumeration's code.
	 * <p>Each code should be unique within enumerations of the same type.
	 */
	Comparable getCode();

	/**
	 * Return a descriptive, optional label.
	 */
	String getLabel();


	// Constants for standard enum ordering (Comparator implementations)

	/**
	 * Shared Comparator instance that sorts enumerations by {@code CODE_ORDER}.
	 */
	Comparator CODE_ORDER = new Comparator() {
		@Override
		public int compare(Object o1, Object o2) {
			Comparable c1 = ((LabeledEnum) o1).getCode();
			Comparable c2 = ((LabeledEnum) o2).getCode();
			return c1.compareTo(c2);
		}
	};

	/**
	 * Shared Comparator instance that sorts enumerations by {@code LABEL_ORDER}.
	 */
	Comparator LABEL_ORDER = new Comparator() {
		@Override
		public int compare(Object o1, Object o2) {
			LabeledEnum e1 = (LabeledEnum) o1;
			LabeledEnum e2 = (LabeledEnum) o2;
			Comparator comp = new NullSafeComparator(String.CASE_INSENSITIVE_ORDER, true);
			return comp.compare(e1.getLabel(), e2.getLabel());
		}
	};

	/**
	 * Shared Comparator instance that sorts enumerations by {@code LABEL_ORDER},
	 * then {@code CODE_ORDER}.
	 */
	Comparator DEFAULT_ORDER =
			new CompoundComparator(new Comparator[] { LABEL_ORDER, CODE_ORDER });

}
