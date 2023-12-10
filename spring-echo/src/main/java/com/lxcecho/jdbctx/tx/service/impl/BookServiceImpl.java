package com.lxcecho.jdbctx.tx.service.impl;

import com.lxcecho.jdbctx.tx.dao.BookDao;
import com.lxcecho.jdbctx.tx.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Service
public class BookServiceImpl implements BookService {

	@Autowired
	private BookDao bookDao;

	//买书的方法：图书id和用户id
	@Override
	public void buyBook(Integer bookId, Integer userId) {
		//TODO 模拟超时效果
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

		//根据图书id查询图书价格
		Integer price = bookDao.getBookPriceByBookId(bookId);

		//更新图书表库存量 -1
		bookDao.updateStock(bookId);

		//更新用户表用户余额 -图书价格
		bookDao.updateUserBalance(userId, price);

		// System.out.println(1/0);
	}
}
