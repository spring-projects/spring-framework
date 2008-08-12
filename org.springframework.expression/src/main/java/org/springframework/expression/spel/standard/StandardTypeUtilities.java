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
package org.springframework.expression.spel.standard;

import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypeUtils;

/**
 * The StandardTypeUtilities implementation pulls together the standard implementations of the TypeComparator,
 * TypeLocator and TypeConverter interfaces. Each of these can be replaced so if only wishing to replace one of those
 * type facilities.
 * 
 * @author Andy Clement
 * 
 */
public class StandardTypeUtilities implements TypeUtils {

	private TypeComparator typeComparator;
	private TypeLocator typeLocator;
	private TypeConverter typeConverter;
	private OperatorOverloader operatorOverloader;

	public StandardTypeUtilities() {
		typeComparator = new StandardComparator();
		typeLocator = new StandardTypeLocator();
		typeConverter = new StandardTypeConverter();
		operatorOverloader = null; // this means operations between basic types are supported (eg. numbers)
	}

	public TypeLocator getTypeLocator() {
		return typeLocator;
	}

	/**
	 * Set the type locator for the StandardTypeUtilities object, allows a user to replace parts of the standard
	 * TypeUtilities implementation if they wish.
	 * 
	 * @param typeLocator the TypeLocator to use from now on
	 */
	public void setTypeLocator(TypeLocator typeLocator) {
		this.typeLocator = typeLocator;
	}

	public TypeConverter getTypeConverter() {
		return typeConverter;
	}

	/**
	 * Set the type converter for the StandardTypeUtilities object, allows a user to replace parts of the standard
	 * TypeUtilities implementation if they wish.
	 * 
	 * @param typeConverter the TypeConverter to use from now on
	 */
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	public TypeComparator getTypeComparator() {
		return typeComparator;
	}

	/**
	 * Set the type comparator for the StandardTypeUtilities object, allows a user to replace parts of the standard
	 * TypeUtilities implementation if they wish.
	 * 
	 * @param typeComparator the TypeComparator to use from now on
	 */
	public void setTypeComparator(TypeComparator typeComparator) {
		this.typeComparator = typeComparator;
	}

	public OperatorOverloader getOperatorOverloader() {
		return operatorOverloader;
	}

	/**
	 * Set the operator overloader for the StandardTypeUtilities object, allows a user to overload the mathematical
	 * operators to support them between non-standard types.
	 * 
	 * @param operatorOverloader the OperatorOverloader to use from now on
	 */
	public void setOperatorOverloader(OperatorOverloader operatorOverloader) {
		this.operatorOverloader = operatorOverloader;
	}

}
