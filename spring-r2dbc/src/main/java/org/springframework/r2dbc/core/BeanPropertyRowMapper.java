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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.r2dbc.spi.OutParameters;
import io.r2dbc.spi.OutParametersMetadata;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.ReadableMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * <p>For a {@code NULL} value read from the database, an attempt will be made to
 * call the corresponding setter method with {@code null}, but in the case of
 * Java primitives this will result in a {@link TypeMismatchException} by default.
 * To ignore {@code NULL} database values for all primitive properties in the
 * target class, set the {@code primitivesDefaultedForNullValue} flag to
 * {@code true}. See {@link #setPrimitivesDefaultedForNullValue(boolean)} for
 * details.
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
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 6.1
 * @param <T> the result type
 * @see DataClassRowMapper
 */
// Note: this class is adapted from the BeanPropertyRowMapper in spring-jdbc
public class BeanPropertyRowMapper<T> implements Function<Readable, T> {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** The class we are mapping to. */
	@Nullable
	private Class<T> mappedClass;

	/** Whether we're strictly validating. */
	private boolean checkFullyPopulated = false;

	/**
	 * Whether {@code NULL} database values should be ignored for primitive
	 * properties in the target class.
	 * @see #setPrimitivesDefaultedForNullValue(boolean)
	 */
	private boolean primitivesDefaultedForNullValue = false;

	/** ConversionService for binding R2DBC values to bean properties. */
	@Nullable
	private ConversionService conversionService = DefaultConversionService.getSharedInstance();

	/** Map of the properties we provide mapping for. */
	@Nullable
	private Map<String, PropertyDescriptor> mappedProperties;

	/** Set of bean property names we provide mapping for. */
	@Nullable
	private Set<String> mappedPropertyNames;

	/**
	 * Create a new {@code BeanPropertyRowMapper}, accepting unpopulated
	 * properties in the target bean.
	 * @param mappedClass the class that each row/outParameters should be mapped to
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass) {
		initialize(mappedClass);
	}

	/**
	 * Create a new {@code BeanPropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @param checkFullyPopulated whether we're strictly validating that
	 * all bean properties have been mapped from corresponding database columns or
	 * out-parameters
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass, boolean checkFullyPopulated) {
		initialize(mappedClass);
		this.checkFullyPopulated = checkFullyPopulated;
	}


	/**
	 * Get the class that we are mapping to.
	 */
	@Nullable
	public final Class<T> getMappedClass() {
		return this.mappedClass;
	}

	/**
	 * Set whether we're strictly validating that all bean properties have been mapped
	 * from corresponding database columns or out-parameters.
	 * <p>Default is {@code false}, accepting unpopulated properties in the target bean.
	 */
	public void setCheckFullyPopulated(boolean checkFullyPopulated) {
		this.checkFullyPopulated = checkFullyPopulated;
	}

	/**
	 * Return whether we're strictly validating that all bean properties have been
	 * mapped from corresponding database columns or out-parameters.
	 */
	public boolean isCheckFullyPopulated() {
		return this.checkFullyPopulated;
	}

	/**
	 * Set whether a {@code NULL} database column or out-parameter value should
	 * be ignored when mapping to a corresponding primitive property in the target class.
	 * <p>Default is {@code false}, throwing an exception when nulls are mapped
	 * to Java primitives.
	 * <p>If this flag is set to {@code true} and you use an <em>ignored</em>
	 * primitive property value from the mapped bean to update the database, the
	 * value in the database will be changed from {@code NULL} to the current value
	 * of that primitive property. That value may be the property's initial value
	 * (potentially Java's default value for the respective primitive type), or
	 * it may be some other value set for the property in the default constructor
	 * (or initialization block) or as a side effect of setting some other property
	 * in the mapped bean.
	 */
	public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
		this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
	}

	/**
	 * Get the value of the {@code primitivesDefaultedForNullValue} flag.
	 * @see #setPrimitivesDefaultedForNullValue(boolean)
	 */
	public boolean isPrimitivesDefaultedForNullValue() {
		return this.primitivesDefaultedForNullValue;
	}

	/**
	 * Set a {@link ConversionService} for binding R2DBC values to bean properties,
	 * or {@code null} for none.
	 * <p>Default is a {@link DefaultConversionService}. This provides support for
	 * {@code java.time} conversion and other special types.
	 * @see #initBeanWrapper(BeanWrapper)
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Return a {@link ConversionService} for binding R2DBC values to bean properties,
	 * or {@code null} if none.
	 */
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	/**
	 * Initialize the mapping meta-data for the given class.
	 * @param mappedClass the mapped class
	 */
	protected void initialize(Class<T> mappedClass) {
		this.mappedClass = mappedClass;
		this.mappedProperties = new HashMap<>();
		this.mappedPropertyNames = new HashSet<>();

		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(mappedClass)) {
			if (pd.getWriteMethod() != null) {
				String lowerCaseName = lowerCaseName(pd.getName());
				this.mappedProperties.put(lowerCaseName, pd);
				String underscoreName = underscoreName(pd.getName());
				if (!lowerCaseName.equals(underscoreName)) {
					this.mappedProperties.put(underscoreName, pd);
				}
				this.mappedPropertyNames.add(pd.getName());
			}
		}
	}

	/**
	 * Remove the specified property from the mapped properties.
	 * @param propertyName the property name (as used by property descriptors)
	 */
	protected void suppressProperty(String propertyName) {
		if (this.mappedProperties != null) {
			this.mappedProperties.remove(lowerCaseName(propertyName));
			this.mappedProperties.remove(underscoreName(propertyName));
		}
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
		initBeanWrapper(bw);

		T mappedObject = constructMappedInstance(readable, readableMetadatas, bw);
		bw.setBeanInstance(mappedObject);

		Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<>() : null);
		int readableItemCount = readableMetadatas.size();
		for(int itemIndex = 0; itemIndex < readableItemCount; itemIndex++) {
			ReadableMetadata itemMetadata = readableMetadatas.get(itemIndex);
			String itemName = itemMetadata.getName();
			String property = lowerCaseName(StringUtils.delete(itemName, " "));
			PropertyDescriptor pd = (this.mappedProperties != null ? this.mappedProperties.get(property) : null);
			if (pd != null) {
				Object value = getItemValue(readable, itemIndex, pd);
				// Implementation note: the JDBC mapper can log the column mapping details each time row 0 is encountered
				// but unfortunately this is not possible in R2DBC as row number is not provided. The BiFunction#apply
				// cannot be stateful as it could be applied to a different row set, e.g. when resubscribing.
				try {
					bw.setPropertyValue(pd.getName(), value);
				}
				catch (TypeMismatchException ex) {
					if (value == null && isPrimitivesDefaultedForNullValue()) {
						if (logger.isDebugEnabled()) {
							String propertyType = ClassUtils.getQualifiedName(pd.getPropertyType());
							//here too, we miss the rowNumber information
							logger.debug("""
										Ignoring intercepted TypeMismatchException for item '%s' \
										with null value when setting property '%s' of type '%s' on object: %s"
										""".formatted(itemName, pd.getName(), propertyType, mappedObject), ex);
						}
					}
					else {
						throw ex;
					}
				}
				if (populatedProperties != null) {
					populatedProperties.add(pd.getName());
				}
			}
		}

		if (populatedProperties != null && !populatedProperties.equals(this.mappedPropertyNames)) {
			throw new InvalidDataAccessApiUsageException("Given readable does not contain all items " +
					"necessary to populate object of " + this.mappedClass + ": " + this.mappedPropertyNames);
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
		Assert.state(this.mappedClass != null, "Mapped class was not specified");
		return BeanUtils.instantiateClass(this.mappedClass);
	}

	/**
	 * Initialize the given BeanWrapper to be used for row mapping or outParameters
	 * mapping.
	 * <p>To be called for each Readable.
	 * <p>The default implementation applies the configured {@link ConversionService},
	 * if any. Can be overridden in subclasses.
	 * @param bw the BeanWrapper to initialize
	 * @see #getConversionService()
	 * @see BeanWrapper#setConversionService
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		ConversionService cs = getConversionService();
		if (cs != null) {
			bw.setConversionService(cs);
		}
	}

	/**
	 * Retrieve an R2DBC object value for the specified item index (a column or an
	 * out-parameter).
	 * <p>The default implementation delegates to
	 * {@link #getItemValue(Readable, int, Class)}.
	 * @param readable is the {@code Row} or {@code OutParameters} holding the data
	 * @param itemIndex is the column index or out-parameter index
	 * @param pd the bean property that each result object is expected to match
	 * @return the Object value
	 * @see #getItemValue(Readable, int, Class)
	 */
	@Nullable
	protected Object getItemValue(Readable readable, int itemIndex, PropertyDescriptor pd) {
		return getItemValue(readable, itemIndex, pd.getPropertyType());
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


	/**
	 * Static factory method to create a new {@code BeanPropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @see #newInstance(Class, ConversionService)
	 */
	public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
		return new BeanPropertyRowMapper<>(mappedClass);
	}

	/**
	 * Static factory method to create a new {@code BeanPropertyRowMapper}.
	 * @param mappedClass the class that each row should be mapped to
	 * @param conversionService the {@link ConversionService} for binding
	 * R2DBC values to bean properties, or {@code null} for none
	 * @see #newInstance(Class)
	 * @see #setConversionService
	 */
	public static <T> BeanPropertyRowMapper<T> newInstance(
			Class<T> mappedClass, @Nullable ConversionService conversionService) {

		BeanPropertyRowMapper<T> rowMapper = newInstance(mappedClass);
		rowMapper.setConversionService(conversionService);
		return rowMapper;
	}

}
