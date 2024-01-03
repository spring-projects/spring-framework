package com.lxcecho.jdbctx.annotx.dao;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public interface BookDao {

	/**
	 * 根据图书 id 查询图书价格
	 *
	 * @param bookId
	 * @return
	 */
	Integer getBookPriceByBookId(Integer bookId);

	/**
	 * 更新图书表库存量 -1
	 *
	 * @param bookId
	 */
	void updateStock(Integer bookId);

	/**
	 * 更新用户表用户余额 -图书价格
	 *
	 * @param userId
	 * @param price
	 */
	void updateUserBalance(Integer userId, Integer price);

	Integer getUserBalance(Integer userId);
}
