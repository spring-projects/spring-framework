/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.validation.support;

import java.util.Map;

import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BindingResult;

/**
 * Subclass of {@link ConcurrentModel} that automatically removes
 * the {@link BindingResult} object when its corresponding
 * target attribute is replaced through regular {@link Map} operations.
 *
 * <p>This is the class exposed to handler methods by Spring WebFlux,
 * typically consumed through a declaration of the
 * {@link org.springframework.ui.Model} interface as a parameter type.
 * There is typically no need to create it within user code.
 * If necessary a handler method can return a regular {@code java.util.Map},
 * likely a {@code java.util.ConcurrentMap}, for a pre-determined model.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see BindingResult
 */
@SuppressWarnings("serial")
public class BindingAwareConcurrentModel extends ConcurrentModel {

	@Override
	public Object put(String key, Object value) {
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		map.entrySet().forEach(e -> removeBindingResultIfNecessary(e.getKey(), e.getValue()));
		super.putAll(map);
	}

	private void removeBindingResultIfNecessary(String key, Object value) {
		if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			String resultKey = BindingResult.MODEL_KEY_PREFIX + key;
			BindingResult result = (BindingResult) get(resultKey);
			if (result != null && result.getTarget() != value) {
				remove(resultKey);
			}
		}
	}

}
