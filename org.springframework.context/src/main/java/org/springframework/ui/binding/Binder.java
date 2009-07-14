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
package org.springframework.ui.binding;

import java.util.Map;

/**
 * Binds user-entered values to properties of a model object.
 * @author Keith Donald
 * @since 3.0
 * @see #addBinding(String)
 * @see #getBinding(String)
 * @see #bind(Map)
 */
public interface Binder {

	/**
	 * The model object for which property bindings may be accessed.
	 */
	Object getModel();

	/**
	 * Add a binding to a model property.
	 * The property may be a path to a member property like "name", or a nested property like "address.city" or "addresses.city".
	 * If the property path is nested and traverses a collection or Map, do not use indexes.
	 * The property path should express the model-class property structure like addresses.city, not an object structure like addresses[0].city.
	 * Examples:
	 * <pre>
	 * name - bind to property 'name'
	 * addresses - bind to property 'addresses', presumably a List&lt;Address&gt; e.g. allowing property expressions like addresses={ 12345 Macy Lane, 1977 Bel Aire Estates } and addresses[0]=12345 Macy Lane
	 * addresses.city - bind to property 'addresses.city', for all indexed addresses in the collection e.g. allowing property expressions like addresses[0].city=Melbourne
	 * address.city - bind to property 'address.city'
	 * favoriteFoodByFoodGroup - bind to property 'favoriteFoodByFoodGroup', presumably a Map<FoodGroup, Food>; e.g. allowing favoriteFoodByFoodGroup={ DAIRY=Milk, MEAT=Steak } and favoriteFoodByFoodGroup['DAIRY']=Milk
	 * favoriteFoodByFoodGroup.name - bind to property 'favoriteFoodByFoodGroup.name', for all keyed Foods in the map; e.g. allowing favoriteFoodByFoodGroup['DAIRY'].name=Milk
	 * </pre>
	 * @param propertyPath the model property path
	 * @return a BindingConfiguration object, allowing additional configuration of the newly added binding
	 * @throws IllegalArgumentException if no such property path exists on the model
	 */
	public BindingConfiguration addBinding(String propertyPath);
	
	/**
	 * Get a binding to a model property..
	 * @param property the property path
	 * @throws NoSuchBindingException if no binding to the property exists
	 */
	Binding getBinding(String property);
	
	/**
	 * Bind the source values to the properties of the model.
	 * A result is returned for each registered {@link Binding}.
	 * @param sourceValues the source values to bind
	 * @return the results of the binding operation
	 * @throws MissingSourceValuesException when the sourceValues Map is missing entries for required bindings
	 */
	BindingResults bind(Map<String, ? extends Object> sourceValues);

}