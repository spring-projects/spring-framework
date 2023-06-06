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

import java.lang.reflect.Constructor;
import java.util.List;

import io.r2dbc.spi.Readable;
import io.r2dbc.spi.ReadableMetadata;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Mapping {@code Function} implementation that converts an R2DBC {@link Readable}
 * (a {@link io.r2dbc.spi.Row Row} or {@link io.r2dbc.spi.OutParameters OutParameters})
 * into a new instance of the specified mapped target class. The mapped target class
 * must be a top-level class or {@code static} nested class, and it may expose either
 * a <em>data class</em> constructor with named parameters corresponding to column
 * names or classic bean property setter methods with property names corresponding
 * to column names (or even a combination of both).
 *
 * <p>The term "data class" applies to Java <em>records</em>, Kotlin <em>data
 * classes</em>, and any class which has a constructor with named parameters
 * that are intended to be mapped to corresponding column names.
 *
 * <p>When combining a data class constructor with setter methods, any property
 * mapped successfully via a constructor argument will not be mapped additionally
 * via a corresponding setter method. This means that constructor arguments take
 * precedence over property setter methods.
 *
 * <p>Note that this class extends {@link BeanPropertyRowMapper} and can
 * therefore serve as a common choice for any mapped target class, flexibly
 * adapting to constructor style versus setter methods in the mapped class.
 *
 * <p>Please note that this class is designed to provide convenience rather than
 * high performance. For best performance, consider using a custom readable mapping
 * {@code Function} implementation.
 *
 * @author Simon Baslé
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 * @param <T> the result type
 */
// Note: this class is adapted from the DataClassRowMapper in spring-jdbc
public class DataClassRowMapper<T> extends BeanPropertyRowMapper<T> {

	@Nullable
	private Constructor<T> mappedConstructor;

	@Nullable
	private String[] constructorParameterNames;

	@Nullable
	private TypeDescriptor[] constructorParameterTypes;


	/**
	 * Create a new {@code DataClassRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 */
	public DataClassRowMapper(Class<T> mappedClass) {
		super(mappedClass);
	}


	@Override
	protected void initialize(Class<T> mappedClass) {
		super.initialize(mappedClass);

		this.mappedConstructor = BeanUtils.getResolvableConstructor(mappedClass);
		int paramCount = this.mappedConstructor.getParameterCount();
		if (paramCount > 0) {
			this.constructorParameterNames = BeanUtils.getParameterNames(this.mappedConstructor);
			for (String name : this.constructorParameterNames) {
				suppressProperty(name);
			}
			this.constructorParameterTypes = new TypeDescriptor[paramCount];
			for (int i = 0; i < paramCount; i++) {
				this.constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(this.mappedConstructor, i));
			}
		}
	}

	@Override
	protected T constructMappedInstance(Readable readable, List<? extends ReadableMetadata> itemMetadatas, TypeConverter tc) {
		Assert.state(this.mappedConstructor != null, "Mapped constructor was not initialized");

		Object[] args;
		if (this.constructorParameterNames != null && this.constructorParameterTypes != null) {
			args = new Object[this.constructorParameterNames.length];
			for (int i = 0; i < args.length; i++) {
				String name = this.constructorParameterNames[i];
				int index = findIndex(readable, itemMetadatas, lowerCaseName(name));
				if (index == -1) {
					index = findIndex(readable, itemMetadatas, underscoreName(name));
				}
				if (index == -1) {
					throw new DataRetrievalFailureException("Unable to map constructor parameter '" + name + "' to a column or out-parameter");
				}
				TypeDescriptor td = this.constructorParameterTypes[i];
				Object value = getItemValue(readable, index, td.getType());
				args[i] = tc.convertIfNecessary(value, td.getType(), td);
			}
		}
		else {
			args = new Object[0];
		}

		return BeanUtils.instantiateClass(this.mappedConstructor, args);
	}

	private int findIndex(Readable readable, List<? extends ReadableMetadata> itemMetadatas, String name) {
		int index = 0;
		for (ReadableMetadata itemMetadata : itemMetadatas) {
			// we use equalsIgnoreCase, similar to RowMetadata#contains(String)
			if (itemMetadata.getName().equalsIgnoreCase(name)) {
				return index;
			}
			index++;
		}
		return -1;
	}


	/**
	 * Static factory method to create a new {@code DataClassRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @see #newInstance(Class, ConversionService)
	 */
	public static <T> DataClassRowMapper<T> newInstance(Class<T> mappedClass) {
		return new DataClassRowMapper<>(mappedClass);
	}

	/**
	 * Static factory method to create a new {@code DataClassRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @param conversionService the {@link ConversionService} for binding
	 * R2DBC values to bean properties, or {@code null} for none
	 * @see #newInstance(Class)
	 * @see #setConversionService
	 */
	public static <T> DataClassRowMapper<T> newInstance(
			Class<T> mappedClass, @Nullable ConversionService conversionService) {

		DataClassRowMapper<T> rowMapper = newInstance(mappedClass);
		rowMapper.setConversionService(conversionService);
		return rowMapper;
	}

}
