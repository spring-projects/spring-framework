package com.lxcecho.jdbctx.tx.service;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public interface BookService {

	//买书的方法：图书id和用户id
	void buyBook(Integer bookId, Integer userId);
}
