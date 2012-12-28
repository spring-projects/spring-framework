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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.CachingMapDecorator;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link LabeledEnumResolver} implementations,
 * caching all retrieved {@link LabeledEnum} instances.
 *
 * <p>Subclasses need to implement the template method
 * {@link #findLabeledEnums(Class)}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see #findLabeledEnums(Class)
 * @deprecated as of Spring 3.0, in favor of Java 5 enums.
 */
@Deprecated
public abstract class AbstractCachingLabeledEnumResolver implements LabeledEnumResolver {

	protected transient final Log logger = LogFactory.getLog(getClass());

	private final LabeledEnumCache labeledEnumCache = new LabeledEnumCache();


	public Set<LabeledEnum> getLabeledEnumSet(Class type) throws IllegalArgumentException {
		return new TreeSet<LabeledEnum>(getLabeledEnumMap(type).values());
	}

	public Map<Comparable, LabeledEnum> getLabeledEnumMap(Class type) throws IllegalArgumentException {
		Assert.notNull(type, "No type specified");
		return this.labeledEnumCache.get(type);
	}

	public LabeledEnum getLabeledEnumByCode(Class type, Comparable code) throws IllegalArgumentException {
		Assert.notNull(code, "No enum code specified");
		Map<Comparable, LabeledEnum> typeEnums = getLabeledEnumMap(type);
		LabeledEnum codedEnum = typeEnums.get(code);
		if (codedEnum == null) {
			throw new IllegalArgumentException(
					"No enumeration with code '" + code + "'" + " of type [" + type.getName() +
					"] exists: this is likely a configuration error. " +
					"Make sure the code value matches a valid instance's code property!");
		}
		return codedEnum;
	}

	public LabeledEnum getLabeledEnumByLabel(Class type, String label) throws IllegalArgumentException {
		Map<Comparable, LabeledEnum> typeEnums = getLabeledEnumMap(type);
		for (LabeledEnum value : typeEnums.values()) {
			if (value.getLabel().equalsIgnoreCase(label)) {
				return value;
			}
		}
		throw new IllegalArgumentException(
				"No enumeration with label '" + label + "' of type [" + type +
				"] exists: this is likely a configuration error. " +
				"Make sure the label string matches a valid instance's label property!");
	}


	/**
	 * Template method to be implemented by subclasses.
	 * Supposed to find all LabeledEnum instances for the given type.
	 * @param type the enum type
	 * @return the Set of LabeledEnum instances
	 * @see org.springframework.core.enums.LabeledEnum
	 */
	protected abstract Set<LabeledEnum> findLabeledEnums(Class type);


	/**
	 * Inner cache class that implements lazy building of LabeledEnum Maps.
	 */
	@SuppressWarnings("serial")
	private class LabeledEnumCache extends CachingMapDecorator<Class, Map<Comparable, LabeledEnum>> {

		public LabeledEnumCache() {
			super(true);
		}

		@Override
		protected Map<Comparable, LabeledEnum> create(Class key) {
			Set<LabeledEnum> typeEnums = findLabeledEnums(key);
			if (typeEnums == null || typeEnums.isEmpty()) {
				throw new IllegalArgumentException(
						"Unsupported labeled enumeration type '" + key + "': " +
						"make sure you've properly defined this enumeration! " +
						"If it is static, are the class and its fields public/static/final?");
			}
			Map<Comparable, LabeledEnum> typeEnumMap = new HashMap<Comparable, LabeledEnum>(typeEnums.size());
			for (LabeledEnum labeledEnum : typeEnums) {
				typeEnumMap.put(labeledEnum.getCode(), labeledEnum);
			}
			return Collections.unmodifiableMap(typeEnumMap);
		}

		@Override
		protected boolean useWeakValue(Class key, Map<Comparable, LabeledEnum> value) {
			if (!ClassUtils.isCacheSafe(key, AbstractCachingLabeledEnumResolver.this.getClass().getClassLoader())) {
				if (logger != null && logger.isDebugEnabled()) {
					logger.debug("Not strongly caching class [" + key.getName() + "] because it is not cache-safe");
				}
				return true;
			}
			else {
				return false;
			}
		}
	}

}
