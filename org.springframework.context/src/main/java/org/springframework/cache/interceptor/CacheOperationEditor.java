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

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * PropertyEditor for {@link CacheOperation} objects. Accepts a String of form
 * <p><tt>action,cache,key,condition</tt> 
 * <p>where only action and cache are required. Available definitions for action are
 * <tt>cacheable</tt> and <tt>evict</tt>.
 * When specifying multiple caches, use ; as a separator
 * 
 * A typical example would be:
 * <p><code>cacheable, orders;books, #p0</code>
 *
 * <p>The tokens need to be specified in the order above.
 *
 * @author Costin Leau
 * 
 * @see org.springframework.transaction.TransactionAttributeEditor 
 * @see org.springframework.core.Constants
 */
public class CacheOperationEditor extends PropertyEditorSupport {

	/**
	 * Format is action, cache, key, condition.
	 * Null or the empty string means that the method is non cacheable.
	 * @see java.beans.PropertyEditor#setAsText(java.lang.String)
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasLength(text)) {
			// tokenize it with ","
			String[] tokens = StringUtils.commaDelimitedListToStringArray(text);
			if (tokens.length < 2) {
				throw new IllegalArgumentException(
						"too little arguments found, at least the cache action and cache name are required");
			}

			CacheOperation op;

			if ("cacheable".contains(tokens[0])) {
				op = new CacheableOperation();
			}

			else if ("evict".contains(tokens[0])) {
				op = new CacheEvictOperation();
			} else {
				throw new IllegalArgumentException("Invalid cache action specified " + tokens[0]);
			}

			op.setCacheNames(StringUtils.delimitedListToStringArray(tokens[1], ";"));

			if (tokens.length > 2) {
				op.setKey(tokens[2]);
			}

			if (tokens.length > 3) {
				op.setCondition(tokens[3]);
			}

			setValue(op);
		} else {
			setValue(null);
		}
	}
}