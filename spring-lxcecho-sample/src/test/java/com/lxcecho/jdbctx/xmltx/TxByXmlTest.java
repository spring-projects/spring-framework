package com.lxcecho.jdbctx.xmltx;

import com.lxcecho.jdbctx.xmltx.controller.BookController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = "classpath:bean-tx-xml.xml")
public class TxByXmlTest {

	@Autowired
	private BookController bookController;

	@Test
	public void testBuyBook() {
		bookController.buyBook(1, 1);
		// 测试事务传播行为：<tx:method name="buy*" read-only="false" propagation="REQUIRES_NEW"/>
//		bookController.buyBook(2, 1);
	}

}
