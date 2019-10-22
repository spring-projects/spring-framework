/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@link RowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class with nested entities using standard
 * Spring BeanWrapper. The mapped target class must be a top-level class and
 * it must have a default or no-arg constructor.
 *
 * <p>Column values are mapped based on matching the column name as obtained from result set
 * meta-data to public setters for the corresponding properties. The names are matched by
 * transforming a name separating the parts with underscores to the same name
 * using "camel" case.
 *
 * <p>To distinguish new nested entity from column name property 'entityDelimiter' is used. The default
 * one is '.' which requires escaping in SQL query like
 * <pre>
 * {@code
 *     SELECT ...
 *       column1 as `NESTED_ENTITY.PROPERTY_NAME` -- h2, mssql
 *       column2 as "NESTED_ENTITY.PROPERTY_NAME" -- oracle (12.1 supports 30 chars only, 12.2 - 128)
 * }
 * </pre>
 * It's possible to override it to pattern you prefer better (like '__' to avoid escaping)
 * <pre>
 * {@code
 *     SELECT ...
 *       column1 as NESTED_ENTITY__PROPERTY_NAME
 * }
 * </pre>
 *
 * <p>Mapping is provided for fields in the target class for many common types, e.g.:
 * String, boolean, Boolean, byte, Byte, short, Short, int, Integer, long, Long,
 * float, Float, double, Double, BigDecimal, {@code java.util.Date}, etc.
 *
 * <p>To facilitate mapping between columns and fields that don't have matching names,
 * try using column aliases in the SQL statement like "select fname as first_name from customer".
 *
 * <p>For 'null' values read from the database, we will attempt to call the setter, but in the case of
 * Java primitives, this causes a TypeMismatchException. This class can be configured (using the
 * primitivesDefaultedForNullValue property) to trap this exception and use the primitives default value.
 * Be aware that if you use the values from the generated bean to update the database the primitive value
 * will have been set to the primitive's default value instead of null.
 *
 * <p>Please note that this class is designed to provide convenience rather than high performance.
 * For best performance, consider using a custom {@link RowMapper} implementation.
 *
 * @author Pavel Tsiber
 * @since ?
 * @param <T> the result type
 */
public class NestedBeanPropertyRowMapper<T> implements RowMapper<T> {

    /** Logger available to subclasses. */
    protected final Log logger = LogFactory.getLog(getClass());

    /** The class we are mapping to. */
    @Nullable
    private Class<T> mappedClass;

    /**
     * Whether we're defaulting primitives when mapping a null value.
     */
    private boolean primitivesDefaultedForNullValue = false;

    /** ConversionService for binding JDBC values to bean properties. */
    @Nullable
    private ConversionService conversionService = DefaultConversionService.getSharedInstance();

    /** Sequence of characters to distinguish nested entity */
    private String entityDelimiter = ".";

    private NestedBeanPropertyRowMapper(Class<T> mappedClass) {
        this.mappedClass = mappedClass;
    }

    /**
     * Static factory method to create a new {@code NestedBeanPropertyRowMapper}
     * (with the mapped class specified only once).
     * @param mappedClass the class that each row should be mapped to
     */
    public static <T> NestedBeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
        return new NestedBeanPropertyRowMapper<>(mappedClass);
    }

    /**
     * Return whether we're defaulting Java primitives in the case of mapping a null value
     * from corresponding database fields.
     */
    public boolean isPrimitivesDefaultedForNullValue() {
        return this.primitivesDefaultedForNullValue;
    }

    /**
     * Set whether we're defaulting Java primitives in the case of mapping a null value
     * from corresponding database fields.
     * <p>Default is {@code false}, throwing an exception when nulls are mapped to Java primitives.
     */
    public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
        this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
    }

    /**
     * Return sequence of characters used to distinguish nested entity
     */
    public String getEntityDelimiter() {
        return entityDelimiter;
    }

    /**
     * Set sequence of characters used to distinguish nested entity
     */
    public void setEntityDelimiter(String entityDelimiter) {
        this.entityDelimiter = entityDelimiter;
    }

    /**
     * Set a {@link ConversionService} for binding JDBC values to bean properties,
     * or {@code null} for none.
     * <p>Default is a {@link DefaultConversionService}, as of Spring 4.3. This
     * provides support for {@code java.time} conversion and other special types.
     * @since 4.3
     * @see #initBeanWrapper(BeanWrapper)
     */
    public void setConversionService(@Nullable ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Return a {@link ConversionService} for binding JDBC values to bean properties,
     * or {@code null} if none.
     * @since 4.3
     */
    @Nullable
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    /**
     * Convert the given name to upper case.
     * By default, conversions will happen within the US locale.
     * @param name the original name
     * @return the converted name
     * @since 4.2
     */
    protected String upperCaseName(String name) {
        return name.toUpperCase(Locale.US);
    }

    /**
     * Convert the given name to lower case.
     * By default, conversions will happen within the US locale.
     * @param name the original name
     * @return the converted name
     * @since 4.2
     */
    protected String lowerCaseName(String name) {
        return name.toLowerCase(Locale.US);
    }

    /**
     * Convert the given name to camel case.
     * By default, conversions will happen within the US locale.
     * @param name the original name
     * @return the converted name
     * @since 4.2
     */
    protected String underscoreToCamelCase(String name) {
        if (!StringUtils.hasLength(name)) {
            return "";
        }

        boolean capitalizeNext = false;
        StringBuilder result = new StringBuilder();
        result.append(lowerCaseName(name.substring(0, 1)));
        for (int i = 1; i < name.length(); i++) {
            String s = name.substring(i, i + 1);
            if (s.equals("_")) {
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext) {
                capitalizeNext = false;
                result.append(upperCaseName(s));
            } else {
                result.append(lowerCaseName(s));
            }
        }

        return result.toString();
    }

    /**
     * Extract the values for all columns in the current row.
     * <p>Utilizes public setters and result set meta-data.
     * @see java.sql.ResultSetMetaData
     */
    @Override
    public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
        Assert.state(this.mappedClass != null, "Mapped class was not specified");
        T mappedObject = BeanUtils.instantiateClass(this.mappedClass);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
        initBeanWrapper(bw);

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        for (int index = 1; index <= columnCount; index++) {
            String column = JdbcUtils.lookupColumnName(rsmd, index);
            String field = underscoreToCamelCase(column.replaceAll(Pattern.quote(entityDelimiter), "."));

            try {
                PropertyDescriptor pd = bw.getPropertyDescriptor(field);
                Object value = getColumnValue(rs, index, pd);

                try {
                    bw.setPropertyValue(field, value);
                } catch (TypeMismatchException ex) {
                    if (value == null && this.primitivesDefaultedForNullValue) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Intercepted TypeMismatchException for row " + rowNumber +
                                    " and column '" + column + "' with null value when setting property '" +
                                    pd.getName() + "' of type '" +
                                    ClassUtils.getQualifiedName(pd.getPropertyType()) +
                                    "' on object: " + mappedObject, ex);
                        }
                    } else {
                        throw ex;
                    }
                }
            } catch (InvalidPropertyException e) {
                logger.debug("No property found for column '" + column + "' mapped to field '" + field + "'");
            }
        }

        return mappedObject;
    }

    /**
     * Initialize the given BeanWrapper to be used for row mapping.
     * To be called for each row.
     * <p>The default implementation applies the configured {@link ConversionService},
     * if any. Can be overridden in subclasses.
     * @param bw the BeanWrapper to initialize
     * @see #getConversionService()
     * @see BeanWrapper#setConversionService
     */
    protected void initBeanWrapper(BeanWrapper bw) {
        bw.setAutoGrowNestedPaths(true);
        ConversionService cs = getConversionService();
        if (cs != null) {
            bw.setConversionService(cs);
        }
    }

    /**
     * Retrieve a JDBC object value for the specified column.
     * <p>The default implementation calls
     * {@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}.
     * Subclasses may override this to check specific value types upfront,
     * or to post-process values return from {@code getResultSetValue}.
     * @param rs is the ResultSet holding the data
     * @param index is the column index
     * @param pd the bean property that each result object is expected to match
     * @return the Object value
     * @throws SQLException in case of extraction failure
     * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)
     */
    @Nullable
    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
    }

}
