package com.lxcecho.jdbctx.xmltx.service.impl;

import com.lxcecho.jdbctx.xmltx.dao.BookDao;
import com.lxcecho.jdbctx.xmltx.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Service
public class BookServiceImpl implements BookService {

	@Autowired
	private BookDao bookDao;

	/**
	 * 买书的方法：图书 id 和用户 id
	 *
	 * @param bookId
	 * @param userId
	 */
	@Override
	public void buyBook(Integer bookId, Integer userId) {

		// 根据图书 id 查询图书价格
		Integer price = bookDao.getBookPriceByBookId(bookId);

		// 更新图书表库存量 -1
		bookDao.updateStock(bookId);

		// 更新用户表用户余额 -图书价格
		bookDao.updateUserBalance(userId, price);
	}
}
