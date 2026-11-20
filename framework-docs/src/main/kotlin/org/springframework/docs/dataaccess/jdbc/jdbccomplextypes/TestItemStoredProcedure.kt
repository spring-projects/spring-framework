/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
				TestItem(
					(attr[0] as Number).toLong(),
					attr[1] as String,
					attr[2] as Date
				)
			})
		// ...
	}
}