package com.lxcecho.jdbctx.annotx;

import com.lxcecho.jdbctx.annotx.config.TxConfig;
import com.lxcecho.jdbctx.annotx.controller.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = "classpath:bean-tx.xml")
public class TxByAnnoTest {

	@Autowired
	private BookController bookController;

	@Test
	public void testBuyBook() {
//		bookController.buyBook(1,1);

		// Test propagation
		Integer[] bookIds = {1, 2};
		bookController.checkout(bookIds, 1);
	}

	@Test
	public void testTxAllAnnotation() {
		ApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(TxConfig.class);
		BookController accountService = applicationContext.getBean("bookController", BookController.class);
		Integer[] bookIds = {1, 2};
		accountService.checkout(bookIds, 1);
	}

}
