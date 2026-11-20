/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbccomplextypes;

import java.sql.CallableStatement;
import java.sql.Struct;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.object.StoredProcedure;

@SuppressWarnings("unused")
public class TestItemStoredProcedure extends StoredProcedure {

	public TestItemStoredProcedure(DataSource dataSource) {
		super(dataSource, "get_item");
		declareParameter(new SqlOutParameter("item", Types.STRUCT, "ITEM_TYPE",
				(CallableStatement cs, int colIndx, int sqlType, String typeName) -> {
					Struct struct = (Struct) cs.getObject(colIndx);
					Object[] attr = struct.getAttributes();
					TestItem item = new TestItem();
					item.setId(((Number) attr[0]).longValue());
					item.setDescription((String) attr[1]);
					item.setExpirationDate((java.util.Date) attr[2]);
					return item;
				}));
		// ...
	}

}