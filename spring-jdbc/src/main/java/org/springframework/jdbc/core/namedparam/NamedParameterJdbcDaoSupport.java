/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * Extension of JdbcDaoSupport that exposes a NamedParameterJdbcTemplate as well.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 * @deprecated as of 7.0, in favor of direct injection of {@link NamedParameterJdbcTemplate}
 * or {@link org.springframework.jdbc.core.simple.JdbcClient}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class NamedParameterJdbcDaoSupport extends JdbcDaoSupport {

	private @Nullable NamedParameterJdbcTemplate namedParameterJdbcTemplate;


	/**
	 * Create a NamedParameterJdbcTemplate based on the configured JdbcTemplate.
	 */
	@Override
	protected void initTemplateConfig() {
		JdbcTemplate jdbcTemplate = getJdbcTemplate();
		if (jdbcTemplate != null) {
			this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		}
	}

	/**
	 * Return a NamedParameterJdbcTemplate wrapping the configured JdbcTemplate.
	 */
	public @Nullable NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return this.namedParameterJdbcTemplate;
	}

}
