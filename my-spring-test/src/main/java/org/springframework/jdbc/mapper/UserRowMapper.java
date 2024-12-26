package org.springframework.jdbc.mapper;


import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author sushuaiqiang
 * @date 2024/12/25 - 15:31
 */
public class UserRowMapper implements RowMapper {

	@Override
	public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new User().setId(rs.getInt("id"))
				.setName(rs.getString("name"))
				.setSex(rs.getString("sex"))
				.setAge(rs.getInt("age"));
	}
}
