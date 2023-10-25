/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.r2dbc.core;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import io.r2dbc.spi.OutParameters;
import io.r2dbc.spi.OutParametersMetadata;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.ReadableMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.TypeConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Mapping {@code Function} implementation that converts an R2DBC {@link Readable}
 * (a {@link Row} or {@link OutParameters}) into a new instance of the specified mapped
 * target class. The mapped target class must be a top-level class or {@code static}
 * nested class, and it must have a default or no-arg constructor.
 *
 * <p>{@code Readable} component values are mapped based on matching the column
 * name (as obtained from R2DBC meta-data) to public setters in the target class
 * for the corresponding properties. The names are matched either directly or by
 * transforming a name separating the parts with underscores to the same name using
 * "camel" case.
 *
 * <p>Mapping is provided for properties in the target class for many common types &mdash;
 * for example: String, boolean, Boolean, byte, Byte, short, Short, int, Integer,
 * long, Long, float, Float, double, Double, BigDecimal, {@code java.util.Date}, etc.
 *
 * <p>To facilitate mapping between columns and properties that don't have matching
 * names, try using column aliases in the SQL statement like
 * {@code "select fname as first_name from customer"}, where {@code first_name}
 * can be mapped to a {@code setFirstName(String)} method in the target class.
 *
 * <p>If you need to map to a target class which has a <em>data class</em> constructor
 * &mdash; for example, a Java {@code record} or a Kotlin {@code data} class &mdash;
 * use {@link DataClassRowMapper} instead.
 *
 * <p>Please note that this class is designed to provide convenience rather than
 * high performance. For best performance, consider using a custom mapping function
 * implementation.
 *
 * @author Simon Baslé
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 * @param <T> the result type
 * @see DataClassRowMapper
 */
public class BeanPropertyRowMapper<T> implements Function<Readable, T> {

	/** The class we are mapping to. */
	private final Class<T> mappedClass;

	/** ConversionService for binding result values to bean properties. */
	private final ConversionService conversionService;

	/** Map of the properties we provide mapping for. */
	private final Map<String, PropertyDescriptor> mappedProperties;


	/**
	 * Create a new {@code BeanPropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass) {
		this(mappedClass, DefaultConversionService.getSharedInstance());
	}

	/**
	 * Create a new {@code BeanPropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @param conversionService a {@link ConversionService} for binding
	 * result values to bean properties
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass, ConversionService conversionService) {
		Assert.notNull(mappedClass, "Mapped Class must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.mappedClass = mappedClass;
		this.conversionService = conversionService;
		this.mappedProperties = new HashMap<>();

		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(mappedClass)) {
			if (pd.getWriteMethod() != null) {
				String lowerCaseName = lowerCaseName(pd.getName());
				this.mappedProperties.put(lowerCaseName, pd);
				String underscoreName = underscoreName(pd.getName());
				if (!lowerCaseName.equals(underscoreName)) {
					this.mappedProperties.put(underscoreName, pd);
				}
			}
		}
	}


	/**
	 * Remove the specified property from the mapped properties.
	 * @param propertyName the property name (as used by property descriptors)
	 */
	protected void suppressProperty(String propertyName) {
		this.mappedProperties.remove(lowerCaseName(propertyName));
		this.mappedProperties.remove(underscoreName(propertyName));
	}

	/**
	 * Convert the given name to lower case.
	 * <p>By default, conversions will happen within the US locale.
	 * @param name the original name
	 * @return the converted name
	 */
	protected String lowerCaseName(String name) {
		return name.toLowerCase(Locale.US);
	}

	/**
	 * Convert a name in camelCase to an underscored name in lower case.
	 * <p>Any upper case letters are converted to lower case with a preceding underscore.
	 * @param name the original name
	 * @return the converted name
	 * @see #lowerCaseName
	 */
	protected String underscoreName(String name) {
		if (!StringUtils.hasLength(name)) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		result.append(Character.toLowerCase(name.charAt(0)));
		for (int i = 1; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				result.append('_').append(Character.toLowerCase(c));
			}
			else {
				result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * Extract the values for the current {@link Readable}: all columns in case
	 * of a {@link Row} or all parameters in case of an {@link OutParameters}.
	 * <p>Utilizes public setters and derives meta-data from the concrete type.
	 * @throws IllegalArgumentException in case the concrete type is neither
	 * {@code Row} nor {@code OutParameters}
	 * @see RowMetadata
	 * @see OutParametersMetadata
	 */
	@Override
	public T apply(Readable readable) {
		if (readable instanceof Row row) {
			return mapForReadable(row, row.getMetadata().getColumnMetadatas());
		}
		if (readable instanceof OutParameters out) {
			return mapForReadable(out, out.getMetadata().getParameterMetadatas());
		}
		throw new IllegalArgumentException("Can only map Readable Row or OutParameters, got " + readable.getClass().getName());
	}

	private <R extends Readable> T mapForReadable(R readable, List<? extends ReadableMetadata> readableMetadatas) {
		BeanWrapperImpl bw = new BeanWrapperImpl();
		bw.setConversionService(this.conversionService);
		T mappedObject = constructMappedInstance(readable, readableMetadatas, bw);
		bw.setBeanInstance(mappedObject);

		int readableItemCount = readableMetadatas.size();
		for (int itemIndex = 0; itemIndex < readableItemCount; itemIndex++) {
			ReadableMetadata itemMetadata = readableMetadatas.get(itemIndex);
			String itemName = itemMetadata.getName();
			String property = lowerCaseName(StringUtils.delete(itemName, " "));
			PropertyDescriptor pd = this.mappedProperties.get(property);
			if (pd != null) {
				Object value = getItemValue(readable, itemIndex, pd.getPropertyType());
				bw.setPropertyValue(pd.getName(), value);
			}
		}

		return mappedObject;
	}

	/**
	 * Construct an instance of the mapped class for the current {@code Readable}.
	 * <p>The default implementation simply instantiates the mapped class. Can be
	 * overridden in subclasses.
	 * @param readable the {@code Readable} being mapped (a {@code Row} or {@code OutParameters})
	 * @param itemMetadatas the list of item {@code ReadableMetadata} (either
	 * {@code ColumnMetadata} or {@code OutParameterMetadata})
	 * @param tc a TypeConverter with this row mapper's conversion service
	 * @return a corresponding instance of the mapped class
	 */
	protected T constructMappedInstance(Readable readable, List<? extends ReadableMetadata> itemMetadatas, TypeConverter tc) {
		return BeanUtils.instantiateClass(this.mappedClass);
	}

	/**
	 * Retrieve an R2DBC object value for the specified item index (a column or
	 * an out-parameter).
	 * <p>The default implementation calls {@link Readable#get(int, Class)} then
	 * falls back to {@link Readable#get(int)} in case of an exception.
	 * Subclasses may override this to check specific value types upfront,
	 * or to post-process values returned from {@code get}.
	 * @param readable is the {@code Row} or {@code OutParameters} holding the data
	 * @param itemIndex is the column index or out-parameter index
	 * @param paramType the target parameter type
	 * @return the Object value
	 * @see Readable#get(int, Class)
	 * @see Readable#get(int)
	 */
	@Nullable
	protected Object getItemValue(Readable readable, int itemIndex, Class<?> paramType) {
		try {
			return readable.get(itemIndex, paramType);
		}
		catch (Throwable ex) {
			return readable.get(itemIndex);
		}
	}

}
