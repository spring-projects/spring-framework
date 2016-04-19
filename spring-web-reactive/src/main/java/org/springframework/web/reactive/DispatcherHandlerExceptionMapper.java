/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.reactive;

import java.util.function.Function;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Map "standard" framework exceptions and
 * {@link ResponseStatus @ResponseStatus}-annotated exceptions to a
 * {@link ResponseStatusException}.
 *
 * @author Rossen Stoyanchev
 */
public class DispatcherHandlerExceptionMapper implements Function<Throwable, Throwable> {


	@Override
	public Throwable apply(Throwable ex) {
		if (ex instanceof HandlerNotFoundException) {
			ex = new ResponseStatusException(HttpStatus.NOT_FOUND, ex);
		}
		else if (ex instanceof HttpMediaTypeNotAcceptableException) {
			ex = new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, ex);
		}
		else {
			ResponseStatus status = findStatus(ex);
			if (status != null) {
				ex = new ResponseStatusException(status.code(), ex);
			}
		}
		return ex;
	}

	private ResponseStatus findStatus(Throwable ex) {
		Class<? extends Throwable> type = ex.getClass();
		ResponseStatus status = AnnotatedElementUtils.findMergedAnnotation(type, ResponseStatus.class);
		if (status != null) {
			return status;
		}
		else if (ex.getCause() != null) {
			return findStatus(ex.getCause());
		}
		return null;
	}

}
