package com.lxcecho.jdbctx.annotx;

import com.lxcecho.jdbctx.annotx.config.TxConfig;
import com.lxcecho.jdbctx.annotx.controller.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TxByAnnoAllTest {

	@Test
	public void testTxAllAnnotation() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(TxConfig.class);
		BookController bookController = applicationContext.getBean("bookController", BookController.class);
		Integer[] bookIds = {1, 2};
		bookController.checkout(bookIds, 1);
	}

}
