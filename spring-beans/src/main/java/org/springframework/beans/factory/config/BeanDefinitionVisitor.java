/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringValueResolver;

/**
 * Visitor class for traversing {@link BeanDefinition} objects, in particular
 * the property values and constructor argument values contained in them,
 * resolving bean metadata values.
 *
 * <p>Used by {@link PlaceholderConfigurerSupport} to parse all String values
 * contained in a BeanDefinition, resolving any placeholders found.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2
 * @see BeanDefinition
 * @see BeanDefinition#getPropertyValues
 * @see BeanDefinition#getConstructorArgumentValues
 * @see PlaceholderConfigurerSupport
 */
public class BeanDefinitionVisitor {

	private @Nullable StringValueResolver valueResolver;


	/**
	 * Create a new BeanDefinitionVisitor, applying the specified
	 * value resolver to all bean metadata values.
	 * @param valueResolver the StringValueResolver to apply
	 */
	public BeanDefinitionVisitor(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		this.valueResolver = valueResolver;
	}

	/**
	 * Create a new BeanDefinitionVisitor for subclassing.
	 * Subclasses need to override the {@link #resolveStringValue} method.
	 */
	protected BeanDefinitionVisitor() {
	}


	/**
	 * Traverse the given BeanDefinition object and the MutablePropertyValues
	 * and ConstructorArgumentValues contained in them.
	 * @param beanDefinition the BeanDefinition object to traverse
	 * @see #resolveStringValue(String)
	 */
	public void visitBeanDefinition(BeanDefinition beanDefinition) {
		visitParentName(beanDefinition);
		visitBeanClassName(beanDefinition);
		visitFactoryBeanName(beanDefinition);
		visitFactoryMethodName(beanDefinition);
		visitScope(beanDefinition);
		if (beanDefinition.hasPropertyValues()) {
			visitPropertyValues(beanDefinition.getPropertyValues());
		}
		if (beanDefinition.hasConstructorArgumentValues()) {
			ConstructorArgumentValues cas = beanDefinition.getConstructorArgumentValues();
			visitIndexedArgumentValues(cas.getIndexedArgumentValues());
			visitGenericArgumentValues(cas.getGenericArgumentValues());
		}
	}

	protected void visitParentName(BeanDefinition beanDefinition) {
		String parentName = beanDefinition.getParentName();
		if (parentName != null) {
			String resolvedName = resolveStringValue(parentName);
			if (!parentName.equals(resolvedName)) {
				beanDefinition.setParentName(resolvedName);
			}
		}
	}

	protected void visitBeanClassName(BeanDefinition beanDefinition) {
		String beanClassName = beanDefinition.getBeanClassName();
		if (beanClassName != null) {
			String resolvedName = resolveStringValue(beanClassName);
			if (!beanClassName.equals(resolvedName)) {
				beanDefinition.setBeanClassName(resolvedName);
			}
		}
	}

	protected void visitFactoryBeanName(BeanDefinition beanDefinition) {
		String factoryBeanName = beanDefinition.getFactoryBeanName();
		if (factoryBeanName != null) {
			String resolvedName = resolveStringValue(factoryBeanName);
			if (!factoryBeanName.equals(resolvedName)) {
				beanDefinition.setFactoryBeanName(resolvedName);
			}
		}
	}

	protected void visitFactoryMethodName(BeanDefinition beanDefinition) {
		String factoryMethodName = beanDefinition.getFactoryMethodName();
		if (factoryMethodName != null) {
			String resolvedName = resolveStringValue(factoryMethodName);
			if (!factoryMethodName.equals(resolvedName)) {
				beanDefinition.setFactoryMethodName(resolvedName);
			}
		}
	}

	protected void visitScope(BeanDefinition beanDefinition) {
		String scope = beanDefinition.getScope();
		if (scope != null) {
			String resolvedScope = resolveStringValue(scope);
			if (!scope.equals(resolvedScope)) {
				beanDefinition.setScope(resolvedScope);
			}
		}
	}

	protected void visitPropertyValues(MutablePropertyValues pvs) {
		PropertyValue[] pvArray = pvs.getPropertyValues();
		for (PropertyValue pv : pvArray) {
			Object newVal = resolveValue(pv.getValue());
			if (!ObjectUtils.nullSafeEquals(newVal, pv.getValue())) {
				pvs.add(pv.getName(), newVal);
			}
		}
	}

	protected void visitIndexedArgumentValues(Map<Integer, ConstructorArgumentValues.ValueHolder> ias) {
		for (ConstructorArgumentValues.ValueHolder valueHolder : ias.values()) {
			Object newVal = resolveValue(valueHolder.getValue());
			if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
				valueHolder.setValue(newVal);
			}
		}
	}

	protected void visitGenericArgumentValues(List<ConstructorArgumentValues.ValueHolder> gas) {
		for (ConstructorArgumentValues.ValueHolder valueHolder : gas) {
			Object newVal = resolveValue(valueHolder.getValue());
			if (!ObjectUtils.nullSafeEquals(newVal, valueHolder.getValue())) {
				valueHolder.setValue(newVal);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	protected @Nullable Object resolveValue(@Nullable Object value) {
		if (value instanceof BeanDefinition beanDef) {
			visitBeanDefinition(beanDef);
		}
		else if (value instanceof BeanDefinitionHolder beanDefHolder) {
			visitBeanDefinition(beanDefHolder.getBeanDefinition());
		}
		else if (value instanceof RuntimeBeanReference ref) {
			String newBeanName = resolveStringValue(ref.getBeanName());
			if (newBeanName == null) {
				return null;
			}
			if (!newBeanName.equals(ref.getBeanName())) {
				return new RuntimeBeanReference(newBeanName);
			}
		}
		else if (value instanceof RuntimeBeanNameReference ref) {
			String newBeanName = resolveStringValue(ref.getBeanName());
			if (newBeanName == null) {
				return null;
			}
			if (!newBeanName.equals(ref.getBeanName())) {
				return new RuntimeBeanNameReference(newBeanName);
			}
		}
		else if (value instanceof Object[] array) {
			visitArray(array);
		}
		else if (value instanceof List list) {
			visitList(list);
		}
		else if (value instanceof Set set) {
			visitSet(set);
		}
		else if (value instanceof Map map) {
			visitMap(map);
		}
		else if (value instanceof TypedStringValue typedStringValue) {
			String stringValue = typedStringValue.getValue();
			if (stringValue != null) {
				String visitedString = resolveStringValue(stringValue);
				typedStringValue.setValue(visitedString);
			}
		}
		else if (value instanceof String strValue) {
			return resolveStringValue(strValue);
		}
		return value;
	}

	protected void visitArray(@Nullable Object[] arrayVal) {
		for (int i = 0; i < arrayVal.length; i++) {
			Object elem = arrayVal[i];
			Object newVal = resolveValue(elem);
			if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
				arrayVal[i] = newVal;
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void visitList(List listVal) {
		for (int i = 0; i < listVal.size(); i++) {
			Object elem = listVal.get(i);
			Object newVal = resolveValue(elem);
			if (!ObjectUtils.nullSafeEquals(newVal, elem)) {
				listVal.set(i, newVal);
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void visitSet(Set setVal) {
		Set newContent = new LinkedHashSet();
		boolean entriesModified = false;
		for (Object elem : setVal) {
			int elemHash = (elem != null ? elem.hashCode() : 0);
			Object newVal = resolveValue(elem);
			int newValHash = (newVal != null ? newVal.hashCode() : 0);
			newContent.add(newVal);
			entriesModified = entriesModified || (newVal != elem || newValHash != elemHash);
		}
		if (entriesModified) {
			setVal.clear();
			setVal.addAll(newContent);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void visitMap(Map<?, ?> mapVal) {
		Map newContent = new LinkedHashMap();
		boolean entriesModified = false;
		for (Map.Entry entry : mapVal.entrySet()) {
			Object key = entry.getKey();
			int keyHash = (key != null ? key.hashCode() : 0);
			Object newKey = resolveValue(key);
			int newKeyHash = (newKey != null ? newKey.hashCode() : 0);
			Object val = entry.getValue();
			Object newVal = resolveValue(val);
			newContent.put(newKey, newVal);
			entriesModified = entriesModified || (newVal != val || newKey != key || newKeyHash != keyHash);
		}
		if (entriesModified) {
			mapVal.clear();
			mapVal.putAll(newContent);
		}
	}

	/**
	 * Resolve the given String value, for example parsing placeholders.
	 * @param strVal the original String value
	 * @return the resolved String value
	 */
	protected @Nullable String resolveStringValue(String strVal) {
		if (this.valueResolver == null) {
			throw new IllegalStateException("No StringValueResolver specified - pass a resolver " +
					"object into the constructor or override the 'resolveStringValue' method");
		}
		String resolvedValue = this.valueResolver.resolveStringValue(strVal);
		// Return original String if not modified.
		return (strVal.equals(resolvedValue) ? strVal : resolvedValue);
	}

}
