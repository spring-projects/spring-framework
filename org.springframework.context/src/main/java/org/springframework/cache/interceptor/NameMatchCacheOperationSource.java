/*
 * Copyright 2011 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * Simple {@link CacheOperationSource} implementation that
 * allows attributes to be matched by registered name.
 * 
 * @author Costin Leau
 */
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

	/**
	 * Logger available to subclasses.
	 * <p>Static for optimal serialization.
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchCacheOperationSource.class);

	/** Keys are method names; values are TransactionAttributes */
	private Map<String, CacheOperation> nameMap = new LinkedHashMap<String, CacheOperation>();

	/**
	 * Set a name/attribute map, consisting of method names
	 * (e.g. "myMethod") and CacheOperation instances
	 * (or Strings to be converted to CacheOperation instances).
	 * @see CacheOperation
	 * @see CacheOperationEditor
	 */
	public void setNameMap(Map<String, CacheOperation> nameMap) {
		for (Map.Entry<String, CacheOperation> entry : nameMap.entrySet()) {
			addCacheMethod(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Parses the given properties into a name/attribute map.
	 * Expects method names as keys and String attributes definitions as values,
	 * parsable into CacheOperation instances via CacheOperationEditor.
	 * @see #setNameMap
	 * @see CacheOperationEditor
	 */
	public void setProperties(Properties cacheOperations) {
		CacheOperationEditor tae = new CacheOperationEditor();
		Enumeration propNames = cacheOperations.propertyNames();
		while (propNames.hasMoreElements()) {
			String methodName = (String) propNames.nextElement();
			String value = cacheOperations.getProperty(methodName);
			tae.setAsText(value);
			CacheOperation op = (CacheOperation) tae.getValue();
			addCacheMethod(methodName, op);
		}
	}

	/**
	 * Add an attribute for a cacheable method.
	 * <p>Method names can be exact matches, or of the pattern "xxx*",
	 * "*xxx" or "*xxx*" for matching multiple methods.
	 * @param methodName the name of the method
	 * @param operation operation associated with the method
	 */
	public void addCacheMethod(String methodName, CacheOperation operation) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding method [" + methodName + "] with cache operation [" + operation + "]");
		}
		this.nameMap.put(methodName, operation);
	}

	public CacheOperation getCacheOperation(Method method, Class<?> targetClass) {
		// look for direct name match
		String methodName = method.getName();
		CacheOperation attr = this.nameMap.get(methodName);

		if (attr == null) {
			// Look for most specific name match.
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName)
						&& (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					attr = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return attr;
	}

	/**
	 * Return if the given method name matches the mapped name.
	 * <p>The default implementation checks for "xxx*", "*xxx" and "*xxx*" matches,
	 * as well as direct equality. Can be overridden in subclasses.
	 * @param methodName the method name of the class
	 * @param mappedName the name in the descriptor
	 * @return if the names match
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchCacheOperationSource)) {
			return false;
		}
		NameMatchCacheOperationSource otherTas = (NameMatchCacheOperationSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchCacheOperationSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}
}