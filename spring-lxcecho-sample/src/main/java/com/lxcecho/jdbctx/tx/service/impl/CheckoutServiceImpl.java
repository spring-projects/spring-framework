package com.lxcecho.jdbctx.tx.service.impl;

import com.lxcecho.jdbctx.tx.service.BookService;
import com.lxcecho.jdbctx.tx.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Service
public class CheckoutServiceImpl implements CheckoutService {

	// 注 入bookService
	@Autowired
	private BookService bookService;

	/**
	 * 买多本书的方法
	 *
	 * @param bookIds
	 * @param userId
	 */
	@Transactional
	@Override
	public void checkout(Integer[] bookIds, Integer userId) {
		for (Integer bookId : bookIds) {
			//调用service的方法
			bookService.buyBook(bookId, userId);
		}
	}
}
