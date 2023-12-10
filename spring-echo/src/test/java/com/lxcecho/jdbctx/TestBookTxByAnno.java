package com.lxcecho.jdbctx;

import com.lxcecho.jdbctx.tx.controller.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = "classpath:bean-tx.xml")
public class TestBookTxByAnno {

	@Autowired
	private BookController bookController;

	@Test
	public void testBuyBook() {
		//bookController.buyBook(1,1);
		Integer[] bookIds = {1, 2};
		bookController.checkout(bookIds, 1);
	}
}
