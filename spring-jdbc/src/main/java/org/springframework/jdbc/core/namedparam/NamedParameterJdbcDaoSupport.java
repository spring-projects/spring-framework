/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jdbc.core.namedparam;

import org.springframework.jdbc.core.support.JdbcDaoSupport;

/**
 * Extension of JdbcDaoSupport that exposes a NamedParameterJdbcTemplate as well.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 */
public class NamedParameterJdbcDaoSupport extends JdbcDaoSupport {

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


	/**
	 * Create a NamedParameterJdbcTemplate based on the configured JdbcTemplate.
	 */
	@Override
	protected void initTemplateConfig() {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(getJdbcTemplate());
	}

	/**
	 * Return a NamedParameterJdbcTemplate wrapping the configured JdbcTemplate.
	 */
	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
	  return namedParameterJdbcTemplate;
	}

}
