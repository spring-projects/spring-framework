/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.support;

import java.util.List;
import java.util.Map;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;

/**
 * Interface for retrieving keys, typically used for auto-generated keys
 * as potentially returned by JDBC insert statements.
 *
 * <p>Implementations of this interface can hold any number of keys.
 * In the general case, the keys are returned as a List containing one Map
 * for each row of keys.
 *
 * <p>Most applications only use on key per row and process only one row at a
 * time in an insert statement. In these cases, just call {@code getKey}
 * to retrieve the key. The returned value is a Number here, which is the
 * usual type for auto-generated keys.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.object.SqlUpdate
 */
public interface KeyHolder {

	/**
	 * Retrieve the first item from the first map, assuming that there is just
	 * one item and just one map, and that the item is a number.
	 * This is the typical case: a single, numeric generated key.
	 * <p>Keys are held in a List of Maps, where each item in the list represents
	 * the keys for each row. If there are multiple columns, then the Map will have
	 * multiple entries as well. If this method encounters multiple entries in
	 * either the map or the list meaning that multiple keys were returned,
	 * then an InvalidDataAccessApiUsageException is thrown.
	 * @return the generated key as a number
	 * @throws InvalidDataAccessApiUsageException if multiple keys are encountered
	 */
	@Nullable
	Number getKey() throws InvalidDataAccessApiUsageException;

	/**
	 * Retrieve the first map of keys.
	 * <p>If there are multiple entries in the list (meaning that multiple rows
	 * had keys returned), then an InvalidDataAccessApiUsageException is thrown.
	 * @return the Map of generated keys for a single row
	 * @throws InvalidDataAccessApiUsageException if keys for multiple rows are encountered
	 */
	@Nullable
	Map<String, Object> getKeys() throws InvalidDataAccessApiUsageException;

	/**
	 * Return a reference to the List that contains the keys.
	 * <p>Can be used for extracting keys for multiple rows (an unusual case),
	 * and also for adding new maps of keys.
	 * @return the List for the generated keys, with each entry representing
	 * an individual row through a Map of column names and key values
	 */
	List<Map<String, Object>> getKeyList();

}
