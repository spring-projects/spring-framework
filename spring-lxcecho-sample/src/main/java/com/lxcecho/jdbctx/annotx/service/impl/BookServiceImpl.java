package com.lxcecho.jdbctx.annotx.service.impl;

import com.lxcecho.jdbctx.annotx.dao.BookDao;
import com.lxcecho.jdbctx.annotx.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 因为 service 层表示业务逻辑层，一个方法表示一个完成的功能，因此处理事务一般在 service 层处理
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Transactional(propagation = Propagation.REQUIRES_NEW) // 标识的类上，则会影响类中所有的方法
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
//	@Transactional(readOnly = true) // 标识在方法上，则只会影响该方法
//	@Transactional(timeout = 3) // 标识在方法上，则只会影响该方法
//	@Transactional(noRollbackFor = ArithmeticException.class) // 标识在方法上，则只会影响该方法
	//@Transactional(noRollbackForClassName = "java.lang.ArithmeticException")
	@Override
	public void buyBook(Integer bookId, Integer userId) {
		// TODO 模拟超时效果
		/*try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}*/

		// 根据图书 id 查询图书价格
		Integer price = bookDao.getBookPriceByBookId(bookId);

		// 更新图书表库存量 -1
		bookDao.updateStock(bookId);

		// 更新用户表用户余额 -图书价格
		bookDao.updateUserBalance(userId, price);

		// 测试 rollback 属性
//		System.out.println(1 / 0);
	}
}
