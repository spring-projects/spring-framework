/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jdbc.object;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.Customer;
import org.springframework.jdbc.core.RowMapper;

public class CustomerMapper implements RowMapper<Customer> {

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};

	@Override
	public Customer mapRow(ResultSet rs, int rownum) throws SQLException {
		Customer cust = new Customer();
		cust.setId(rs.getInt(COLUMN_NAMES[0]));
		cust.setForename(rs.getString(COLUMN_NAMES[1]));
		return cust;
	}

}
