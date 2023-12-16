package com.lxcecho.jdbctx.annotx.service;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public interface BookService {

	/**
	 * 买书的方法：图书 id 和用户 id
	 *
	 * @param bookId
	 * @param userId
	 */
	void buyBook(Integer bookId, Integer userId);
}
