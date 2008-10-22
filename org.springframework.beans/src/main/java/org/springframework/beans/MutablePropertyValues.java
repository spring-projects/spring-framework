/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link PropertyValues} interface.
 * Allows simple manipulation of properties, and provides constructors
 * to support deep copy and construction from a Map.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 13 May 2001
 */
public class MutablePropertyValues implements PropertyValues, Serializable {

	/** List of PropertyValue objects */
	private final List propertyValueList;

	private Set processedProperties;

	private volatile boolean converted = false;


	/**
	 * Creates a new empty MutablePropertyValues object.
	 * Property values can be added with the <code>addPropertyValue</code> methods.
	 * @see #addPropertyValue(PropertyValue)
	 * @see #addPropertyValue(String, Object)
	 */
	public MutablePropertyValues() {
		this.propertyValueList = new ArrayList();
	}

	/**
	 * Deep copy constructor. Guarantees PropertyValue references
	 * are independent, although it can't deep copy objects currently
	 * referenced by individual PropertyValue objects.
	 * @param original the PropertyValues to copy
	 * @see #addPropertyValues(PropertyValues)
	 */
	public MutablePropertyValues(PropertyValues original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		if (original != null) {
			PropertyValue[] pvs = original.getPropertyValues();
			this.propertyValueList = new ArrayList(pvs.length);
			for (int i = 0; i < pvs.length; i++) {
				PropertyValue newPv = new PropertyValue(pvs[i]);
				this.propertyValueList.add(newPv);
			}
		}
		else {
			this.propertyValueList = new ArrayList(0);
		}
	}

	/**
	 * Construct a new MutablePropertyValues object from a Map.
	 * @param original Map with property values keyed by property name Strings
	 * @see #addPropertyValues(Map)
	 */
	public MutablePropertyValues(Map original) {
		// We can optimize this because it's all new:
		// There is no replacement of existing property values.
		if (original != null) {
			this.propertyValueList = new ArrayList(original.size());
			Iterator it = original.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				PropertyValue newPv = new PropertyValue((String) entry.getKey(), entry.getValue());
				this.propertyValueList.add(newPv);
			}
		}
		else {
			this.propertyValueList = new ArrayList(0);
		}
	}

	/**
	 * Construct a new MutablePropertyValues object using the given List of
	 * PropertyValue objects as-is.
	 * <p>This is a constructor for advanced usage scenarios.
	 * It is not intended for typical programmatic use.
	 * @param propertyValueList List of PropertyValue objects
	 */
	public MutablePropertyValues(List propertyValueList) {
		this.propertyValueList = (propertyValueList != null ? propertyValueList : new ArrayList());
	}


	/**
	 * Return the underlying List of PropertyValue objects in its raw form.
	 * The returned List can be modified directly, although this is not recommended.
	 * <p>This is an accessor for optimized access to all PropertyValue objects.
	 * It is not intended for typical programmatic use.
	 */
	public List getPropertyValueList() {
		return this.propertyValueList;
	}

	/**
	 * Copy all given PropertyValues into this object. Guarantees PropertyValue
	 * references are independent, although it can't deep copy objects currently
	 * referenced by individual PropertyValue objects.
	 * @param other the PropertyValues to copy
	 * @return this object to allow creating objects, adding multiple PropertyValues
	 * in a single statement
	 */
	public MutablePropertyValues addPropertyValues(PropertyValues other) {
		if (other != null) {
			PropertyValue[] pvs = other.getPropertyValues();
			for (int i = 0; i < pvs.length; i++) {
				PropertyValue newPv = new PropertyValue(pvs[i]);
				addPropertyValue(newPv);
			}
		}
		return this;
	}

	/**
	 * Add all property values from the given Map.
	 * @param other Map with property values keyed by property name,
	 * which must be a String
	 * @return this object to allow creating objects, adding multiple
	 * PropertyValues in a single statement
	 */
	public MutablePropertyValues addPropertyValues(Map other) {
		if (other != null) {
			Iterator it = other.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				PropertyValue newPv = new PropertyValue((String) entry.getKey(), entry.getValue());
				addPropertyValue(newPv);
			}
		}
		return this;
	}

	/**
	 * Add a PropertyValue object, replacing any existing one
	 * for the corresponding property.
	 * @param pv PropertyValue object to add
	 * @return this object to allow creating objects, adding multiple
	 * PropertyValues in a single statement
	 */
	public MutablePropertyValues addPropertyValue(PropertyValue pv) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue currentPv = (PropertyValue) this.propertyValueList.get(i);
			if (currentPv.getName().equals(pv.getName())) {
				pv = mergeIfRequired(pv, currentPv);
				setPropertyValueAt(pv, i);
				return this;
			}
		}
		this.propertyValueList.add(pv);
		return this;
	}

	/**
	 * Overloaded version of <code>addPropertyValue</code> that takes
	 * a property name and a property value.
	 * @param propertyName name of the property
	 * @param propertyValue value of the property
	 * @see #addPropertyValue(PropertyValue)
	 */
	public void addPropertyValue(String propertyName, Object propertyValue) {
		addPropertyValue(new PropertyValue(propertyName, propertyValue));
	}

	/**
	 * Modify a PropertyValue object held in this object.
	 * Indexed from 0.
	 */
	public void setPropertyValueAt(PropertyValue pv, int i) {
		this.propertyValueList.set(i, pv);
	}

	/**
	 * Merges the value of the supplied 'new' {@link PropertyValue} with that of
	 * the current {@link PropertyValue} if merging is supported and enabled.
	 * @see Mergeable
	 */
	private PropertyValue mergeIfRequired(PropertyValue newPv, PropertyValue currentPv) {
		Object value = newPv.getValue();
		if (value instanceof Mergeable) {
			Mergeable mergeable = (Mergeable) value;
			if (mergeable.isMergeEnabled()) {
				Object merged = mergeable.merge(currentPv.getValue());
				return new PropertyValue(newPv.getName(), merged);
			}
		}
		return newPv;
	}

	/**
	 * Overloaded version of <code>removePropertyValue</code> that takes a property name.
	 * @param propertyName name of the property
	 * @see #removePropertyValue(PropertyValue)
	 */
	public void removePropertyValue(String propertyName) {
		removePropertyValue(getPropertyValue(propertyName));
	}

	/**
	 * Remove the given PropertyValue, if contained.
	 * @param pv the PropertyValue to remove
	 */
	public void removePropertyValue(PropertyValue pv) {
		this.propertyValueList.remove(pv);
	}

	/**
	 * Clear this holder, removing all PropertyValues.
	 */
	public void clear() {
		this.propertyValueList.clear();
	}


	public PropertyValue[] getPropertyValues() {
		return (PropertyValue[])
				this.propertyValueList.toArray(new PropertyValue[this.propertyValueList.size()]);
	}

	public PropertyValue getPropertyValue(String propertyName) {
		for (int i = 0; i < this.propertyValueList.size(); i++) {
			PropertyValue pv = (PropertyValue) this.propertyValueList.get(i);
			if (pv.getName().equals(propertyName)) {
				return pv;
			}
		}
		return null;
	}

	/**
	 * Register the specified property as "processed" in the sense
	 * of some processor calling the corresponding setter method
	 * outside of the PropertyValue(s) mechanism.
	 * <p>This will lead to <code>true</code> being returned from
	 * a {@link #contains} call for the specified property.
	 * @param propertyName the name of the property.
	 */
	public void registerProcessedProperty(String propertyName) {
		if (this.processedProperties == null) {
			this.processedProperties = new HashSet();
		}
		this.processedProperties.add(propertyName);
	}

	public boolean contains(String propertyName) {
		return (getPropertyValue(propertyName) != null ||
				(this.processedProperties != null && this.processedProperties.contains(propertyName)));
	}

	public boolean isEmpty() {
		return this.propertyValueList.isEmpty();
	}

	public int size() {
		return this.propertyValueList.size();
	}

	public PropertyValues changesSince(PropertyValues old) {
		MutablePropertyValues changes = new MutablePropertyValues();
		if (old == this) {
			return changes;
		}

		// for each property value in the new set
		for (Iterator it = this.propertyValueList.iterator(); it.hasNext();) {
			PropertyValue newPv = (PropertyValue) it.next();
			// if there wasn't an old one, add it
			PropertyValue pvOld = old.getPropertyValue(newPv.getName());
			if (pvOld == null) {
				changes.addPropertyValue(newPv);
			}
			else if (!pvOld.equals(newPv)) {
				// it's changed
				changes.addPropertyValue(newPv);
			}
		}
		return changes;
	}


	/**
	 * Mark this holder as containing converted values only
	 * (i.e. no runtime resolution needed anymore).
	 */
	public void setConverted() {
		this.converted = true;
	}

	/**
	 * Return whether this holder contains converted values only (<code>true</code>),
	 * or whether the values still need to be converted (<code>false</code>).
	 */
	public boolean isConverted() {
		return this.converted;
	}


	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MutablePropertyValues)) {
			return false;
		}
		MutablePropertyValues that = (MutablePropertyValues) other;
		return this.propertyValueList.equals(that.propertyValueList);
	}

	public int hashCode() {
		return this.propertyValueList.hashCode();
	}

	public String toString() {
		PropertyValue[] pvs = getPropertyValues();
		StringBuffer sb = new StringBuffer("PropertyValues: length=" + pvs.length + "; ");
		sb.append(StringUtils.arrayToDelimitedString(pvs, "; "));
		return sb.toString();
	}

}
