package com.lxcecho.jdbctx.tx.service;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public interface CheckoutService {
	//买多本书的方法
	void checkout(Integer[] bookIds, Integer userId);
}
