/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.docs.dataaccess.jdbc.jdbccomplextypes

import org.springframework.jdbc.core.SqlOutParameter
import org.springframework.jdbc.`object`.StoredProcedure
import java.sql.CallableStatement
import java.sql.Struct
import java.sql.Types
import java.util.Date
import javax.sql.DataSource

@Suppress("unused")
class TestItemStoredProcedure(dataSource: DataSource) : StoredProcedure(dataSource, "get_item") {
	init {
		declareParameter(SqlOutParameter("item",Types.STRUCT,"ITEM_TYPE") {
				cs: CallableStatement, colIndx: Int, _: Int, _: String? ->
				val struct = cs.getObject(colIndx) as Struct
				val attr = struct.attributes
				val item = TestItem()
				item.id = (attr[0] as Number).toLong()
				item.description = attr[1] as String
				item.expirationDate = attr[2] as Date
				item
			})
		// ...
	}
}