/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.validation.support;

import java.util.Map;

import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

/**
 * Subclass of {@link org.springframework.ui.ExtendedModelMap} that automatically removes
 * a {@link org.springframework.validation.BindingResult} object if the corresponding
 * target attribute gets replaced through regular {@link Map} operations.
 *
 * <p>This is the class exposed to handler methods by Spring MVC, typically consumed through
 * a declaration of the {@link org.springframework.ui.Model} interface. There is no need to
 * build it within user code; a plain {@link org.springframework.ui.ModelMap} or even a just
 * a regular {@link Map} with String keys will be good enough to return a user model.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see org.springframework.validation.BindingResult
 */
@SuppressWarnings("serial")
public class BindingAwareModelMap extends ExtendedModelMap {

	@Override
	public Object put(String key, Object value) {
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		for (Map.Entry<? extends String, ?> entry : map.entrySet()) {
			removeBindingResultIfNecessary(entry.getKey(), entry.getValue());
		}
		super.putAll(map);
	}

	private void removeBindingResultIfNecessary(Object key, Object value) {
		if (key instanceof String) {
			String attributeName = (String) key;
			if (!attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + attributeName;
				BindingResult bindingResult = (BindingResult) get(bindingResultKey);
				if (bindingResult != null && bindingResult.getTarget() != value) {
					remove(bindingResultKey);
				}
			}
		}
	}

}
