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

package org.springframework.util.comparator;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * A comparator that converts values before they are compared. The specified
 * {@link Converter} will be used to convert each value before it passed to the underlying
 * {@link Comparator}.
 * 
 * @author Phillip Webb
 * @param <S> the source type
 * @param <T> the target type
 * @since 3.2
 */
public class ConvertingComparator<S, T> implements Comparator<S> {

	private Comparator<T> comparator;

	private Converter<S, T> converter;

	/**
	 * Create a new {@link ConvertingComparator} instance.
	 * 
	 * @param comparator the underlying comparator used to compare the converted values
	 * @param converter the converter
	 */
	public ConvertingComparator(Comparator<T> comparator, Converter<S, T> converter) {
		Assert.notNull(comparator, "Comparator must not be null");
		Assert.notNull(converter, "Converter must not be null");
		this.comparator = comparator;
		this.converter = converter;
	}

	/**
	 * Create a new {@link ComparableComparator} instance.
	 * 
	 * @param comparator the underlying comparator
	 * @param conversionService the conversion service
	 * @param targetType the target type
	 */
	public ConvertingComparator(Comparator<T> comparator,
		ConversionService conversionService, Class<? extends T> targetType) {
		this(comparator, new ConversionServiceConverter<S, T>(conversionService,
			targetType));
	}
	
	public int compare(S o1, S o2) {
		T c1 = this.converter.convert(o1);
		T c2 = this.converter.convert(o2);
		return this.comparator.compare(c1, c2);
	}

	/**
	 * Convenience method that can be used to get a {@link ConvertingComparator} for the
	 * given <tt>comparator</tt> and <tt>converter</tt>.
	 * 
	 * @param comparator the underlying comparator used to compare the converted values
	 * @param converter the converter
	 * @return a new {@link ConvertingComparator} instance
	 */
	public static <S, T> ConvertingComparator<S, T> get(Comparator<T> comparator,
		Converter<S, T> converter) {
		return new ConvertingComparator<S, T>(comparator, converter);
	}
	
	/**
	 * Convenience method that can be used to get a {@link ConvertingComparator} for the
	 * given <tt>converter</tt>.  This method will use a {@link ConvertingComparator} as
	 * the underlying comparator.
	 * 
	 * @param comparator the underlying comparator used to compare the converted values
	 * @param converter the converter
	 * @return a new {@link ConvertingComparator} instance
	 */
	public static <S, T extends Comparable<T>> ConvertingComparator<S, T> get(
		Converter<S, T> converter) {
		Comparator<T> comparator = ComparableComparator.get();
		return new ConvertingComparator<S, T>(comparator, converter);
	}

	/**
	 * Create a new {@link ConvertingComparator} that compares {@link Map.Entry map
	 * entries} based on their {@link Map.Entry#getKey() keys}.
	 * 
	 * @param comparator the underlying comparator used to compare keys
	 * @return a new {@link ConvertingComparator} instance
	 */
	public static <K, V> ConvertingComparator<Map.Entry<K, V>, K> mapEntryKeys(
		Comparator<K> comparator) {
		return get(comparator, new Converter<Map.Entry<K, V>, K>() {

			public K convert(Entry<K, V> source) {
				return source.getKey();
			}
		});
	}

	/**
	 * Create a new {@link ConvertingComparator} that compares {@link Map.Entry map
	 * entries} based on their {@link Map.Entry#getValue() values}.
	 * 
	 * @param comparator the underlying comparator used to compare values
	 * @return a new {@link ConvertingComparator} instance
	 */
	public static <K, V> ConvertingComparator<Map.Entry<K, V>, V> mapEntryValues(
		Comparator<V> comparator) {
		return get(comparator, new Converter<Map.Entry<K, V>, V>() {

			public V convert(Entry<K, V> source) {
				return source.getValue();
			}
		});
	}

	/**
	 * Adapts a {@link ConversionService} and <tt>targetType</tt> to a {@link Converter}.
	 */
	private static class ConversionServiceConverter<S, T> implements Converter<S, T> {

		private final ConversionService conversionService;

		private final Class<? extends T> targetType;

		public ConversionServiceConverter(ConversionService conversionService,
			Class<? extends T> targetType) {
			Assert.notNull(conversionService, "ConversionService must not be null");
			Assert.notNull(targetType, "TargetType must not be null");
			this.conversionService = conversionService;
			this.targetType = targetType;
		}

		public T convert(S source) {
			return this.conversionService.convert(source, this.targetType);
		}
	}
}
