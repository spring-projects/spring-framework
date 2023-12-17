package com.lxcecho.jdbctx.annotx.service;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public interface CheckoutService {

	/**
	 * 买多本书的方法
	 *
	 * @param bookIds
	 * @param userId
	 */
	void checkout(Integer[] bookIds, Integer userId);

}
