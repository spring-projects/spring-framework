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

package org.springframework.core.convert.support;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * @author Sebastien Deleuze
 */
public class MonoToCompletableFutureConverter implements GenericConverter {

	@Override
	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		Set<GenericConverter.ConvertiblePair> pairs = new LinkedHashSet<>(2);
		pairs.add(new GenericConverter.ConvertiblePair(Mono.class, CompletableFuture.class));
		pairs.add(new GenericConverter.ConvertiblePair(CompletableFuture.class, Mono.class));
		return pairs;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		else if (CompletableFuture.class.isAssignableFrom(sourceType.getType())) {
			return Mono.fromFuture((CompletableFuture<?>) source);
		}
		else if (CompletableFuture.class.isAssignableFrom(targetType.getType())) {
			return Mono.from((Publisher<?>) source).toFuture();
		}
		return null;
	}

}
