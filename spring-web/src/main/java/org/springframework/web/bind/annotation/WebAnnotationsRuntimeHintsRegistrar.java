/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.util.stream.Stream;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.support.RuntimeHintsUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

/**
 * {@link RuntimeHintsRegistrar} implementation that make web binding
 * annotations at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class WebAnnotationsRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		Stream.of(Controller.class, ControllerAdvice.class, CookieValue.class,
				CrossOrigin.class, DeleteMapping.class, ExceptionHandler.class,
				GetMapping.class, InitBinder.class, Mapping.class, MatrixVariable.class,
				ModelAttribute.class, PatchMapping.class, PathVariable.class,
				PostMapping.class, PutMapping.class, RequestAttribute.class,
				RequestBody.class, RequestHeader.class, RequestMapping.class,
				RequestParam.class, RequestPart.class, ResponseBody.class,
				ResponseStatus.class, RestController.class, RestControllerAdvice.class,
				SessionAttribute.class, SessionAttributes.class).forEach(
				annotationType -> RuntimeHintsUtils.registerAnnotation(hints, annotationType));
	}

}
