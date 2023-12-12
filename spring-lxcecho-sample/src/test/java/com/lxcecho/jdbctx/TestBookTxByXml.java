package com.lxcecho.jdbctx;

import com.lxcecho.jdbctx.xmltx.controller.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = "classpath:bean-tx-xml.xml")
public class TestBookTxByXml {

	@Autowired
	private BookController bookController;

	@Test
	public void testBuyBook() {
		bookController.buyBook(1, 1);
	}
}
