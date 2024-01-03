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
 * <p>
 * 1.@Transactional(propagation = Propagation.REQUIRED)，默认情况，表示如果当前线程上有已经开启的事务可用，那么就在这个事务中运行。经过观察，购买图书的方法 buyBook() 在 checkout() 中被调用，
 * checkout() 上有事务注解，因此在此事务中执行。所购买的两本图书的价格为 80 和 50，而用户的余额为 100，因此在购买第二本图书时余额不足失败，导致整个 checkout() 回滚，即只要有一本书买不了，就都买不了。
 * <p>
 * 2.@Transactional(propagation = Propagation.REQUIRES_NEW)，表示不管当前线程上是否有已经开启的事务，都要开启新事务。同样的场景，每次购买图书都是在 buyBook() 的事务中执行，因此第一本图书购买成功，
 * 事务结束，第二本图书购买失败，只在第二次的 buyBook() 中回滚，购买第一本图书不受影响，即能买几本就买几本。
 * <p>
 * 【注意】：@Transactional 注解也可以加在接口上，但只有在设置了基于接口的代理时才会生效，因为注解不能继承。所以该注解最好是加在类的实现上。
 *
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
//@Transactional(propagation = Propagation.REQUIRED) // 标识的类上，则会影响类中所有的 public 方法
@Transactional(propagation = Propagation.REQUIRES_NEW) // 标识的类上，则会影响类中所有的 public 方法
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
//	@Transactional(readOnly = true) // 标识在方法上，则只会影响 public 方法
//	@Transactional(timeout = 3) // 标识在方法上，则只会影响 public 方法
//	@Transactional(noRollbackFor = ArithmeticException.class) // 标识在方法上，则只会影响 public 方法
//	@Transactional(noRollbackForClassName = "java.lang.ArithmeticException")
//	@Transactional(rollbackFor = Exception.class) // Spring 事务只回滚运行时异常和 Error
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

		System.out.println(bookId + "'s price=" + price);

		// 更新图书表库存量 -1
		bookDao.updateStock(bookId);

		// 更新用户表用户余额 -图书价格
		bookDao.updateUserBalance(userId, price);

		System.out.println("balance=" + bookDao.getUserBalance(userId));

		// 测试 rollback 属性
//		System.out.println(1 / 0);
	}
}
