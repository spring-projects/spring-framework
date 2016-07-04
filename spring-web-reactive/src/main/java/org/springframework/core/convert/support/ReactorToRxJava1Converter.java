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

import org.reactivestreams.Publisher;
import reactor.core.converter.RxJava1CompletableConverter;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.converter.RxJava1SingleConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 */
public final class ReactorToRxJava1Converter implements GenericConverter {

	@Override
	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		Set<GenericConverter.ConvertiblePair> pairs = new LinkedHashSet<>();
		pairs.add(new GenericConverter.ConvertiblePair(Flux.class, Observable.class));
		pairs.add(new GenericConverter.ConvertiblePair(Observable.class, Flux.class));
		pairs.add(new GenericConverter.ConvertiblePair(Mono.class, Single.class));
		pairs.add(new GenericConverter.ConvertiblePair(Single.class, Mono.class));
		pairs.add(new GenericConverter.ConvertiblePair(Mono.class, Completable.class));
		pairs.add(new GenericConverter.ConvertiblePair(Completable.class, Mono.class));
		return pairs;
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		if (Observable.class.isAssignableFrom(sourceType.getType())) {
			return RxJava1ObservableConverter.from((Observable) source);
		}
		else if (Observable.class.isAssignableFrom(targetType.getType())) {
			return RxJava1ObservableConverter.from((Publisher) source);
		}
		else if (Single.class.isAssignableFrom(sourceType.getType())) {
			return RxJava1SingleConverter.from((Single) source);
		}
		else if (Single.class.isAssignableFrom(targetType.getType())) {
			return RxJava1SingleConverter.from((Publisher) source);
		}
		else if (Completable.class.isAssignableFrom(sourceType.getType())) {
			return RxJava1CompletableConverter.from((Completable) source);
		}
		else if (Completable.class.isAssignableFrom(targetType.getType())) {
			return RxJava1CompletableConverter.from((Publisher) source);
		}
		return null;
	}

}
