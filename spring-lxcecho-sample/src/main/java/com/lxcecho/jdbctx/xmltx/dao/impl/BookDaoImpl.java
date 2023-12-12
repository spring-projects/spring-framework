package com.lxcecho.jdbctx.xmltx.dao.impl;

import com.lxcecho.jdbctx.xmltx.dao.BookDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Repository
public class BookDaoImpl implements BookDao {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	//根据图书id查询价格
	@Override
	public Integer getBookPriceByBookId(Integer bookId) {
		String sql = "select price from t_book where book_id=?";
		Integer price = jdbcTemplate.queryForObject(sql, Integer.class, bookId);
		return price;
	}

	//更新库存
	@Override
	public void updateStock(Integer bookId) {
		String sql = "update t_book set stock=stock-1 where book_id=?";
		jdbcTemplate.update(sql, bookId);
	}

	//更新用户表用户余额 -图书价格
	@Override
	public void updateUserBalance(Integer userId, Integer price) {
		String sql = "update t_user set balance=balance-? where user_id=?";
		jdbcTemplate.update(sql, price, userId);
	}
}
