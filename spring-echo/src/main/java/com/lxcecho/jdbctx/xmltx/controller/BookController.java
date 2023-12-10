package com.lxcecho.jdbctx.xmltx.controller;

import com.lxcecho.jdbctx.xmltx.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Controller
public class BookController {

	@Autowired
	private BookService bookService;

	//买书的方法：图书id和用户id
	public void buyBook(Integer bookId, Integer userId) {
		//调用service方法
		bookService.buyBook(bookId, userId);
	}

}
