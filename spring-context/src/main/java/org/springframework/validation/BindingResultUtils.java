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

package org.springframework.validation;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenience methods for looking up BindingResults in a model Map.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see BindingResult#MODEL_KEY_PREFIX
 */
public abstract class BindingResultUtils {

	/**
	 * Find the BindingResult for the given name in the given model.
	 * @param model the model to search
	 * @param name the name of the target object to find a BindingResult for
	 * @return the BindingResult, or {@code null} if none found
	 * @throws IllegalStateException if the attribute found is not of type BindingResult
	 */
	@Nullable
	public static BindingResult getBindingResult(Map<?, ?> model, String name) {
		Assert.notNull(model, "Model map must not be null");
		Assert.notNull(name, "Name must not be null");
		Object attr = model.get(BindingResult.MODEL_KEY_PREFIX + name);
		if (attr != null && !(attr instanceof BindingResult)) {
			throw new IllegalStateException("BindingResult attribute is not of type BindingResult: " + attr);
		}
		return (BindingResult) attr;
	}

	/**
	 * Find a required BindingResult for the given name in the given model.
	 * @param model the model to search
	 * @param name the name of the target object to find a BindingResult for
	 * @return the BindingResult (never {@code null})
	 * @throws IllegalStateException if no BindingResult found
	 */
	public static BindingResult getRequiredBindingResult(Map<?, ?> model, String name) {
		BindingResult bindingResult = getBindingResult(model, name);
		if (bindingResult == null) {
			throw new IllegalStateException("No BindingResult attribute found for name '" + name +
					"'- have you exposed the correct model?");
		}
		return bindingResult;
	}

}
