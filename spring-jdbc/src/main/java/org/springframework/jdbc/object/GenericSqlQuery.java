/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.object;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * A concrete variant of {@link SqlQuery} which can be configured
 * with a {@link RowMapper}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> the result type
 * @see #setRowMapper
 * @see #setRowMapperClass
 */
public class GenericSqlQuery<T extends @Nullable Object> extends SqlQuery<T> {

	private @Nullable RowMapper<T> rowMapper;

	@SuppressWarnings("rawtypes")
	private @Nullable Class<? extends RowMapper> rowMapperClass;


	/**
	 * Set a specific {@link RowMapper} instance to use for this query.
	 * @since 4.3.2
	 */
	public void setRowMapper(RowMapper<T> rowMapper) {
		this.rowMapper = rowMapper;
	}

	/**
	 * Set a {@link RowMapper} class for this query, creating a fresh
	 * {@link RowMapper} instance per execution.
	 */
	@SuppressWarnings("rawtypes")
	public void setRowMapperClass(Class<? extends RowMapper> rowMapperClass) {
		this.rowMapperClass = rowMapperClass;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.isTrue(this.rowMapper != null || this.rowMapperClass != null,
				"'rowMapper' or 'rowMapperClass' is required");
	}


	@Override
	@SuppressWarnings("unchecked")
	protected RowMapper<T> newRowMapper(@Nullable Object @Nullable [] parameters, @Nullable Map<?, ?> context) {
		if (this.rowMapper != null) {
			return this.rowMapper;
		}
		else {
			Assert.state(this.rowMapperClass != null, "No RowMapper set");
			return BeanUtils.instantiateClass(this.rowMapperClass);
		}
	}

}
