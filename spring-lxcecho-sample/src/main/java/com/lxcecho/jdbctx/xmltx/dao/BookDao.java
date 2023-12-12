package com.lxcecho.jdbctx.xmltx.dao;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public interface BookDao {

    //根据图书id查询图书价格
    Integer getBookPriceByBookId(Integer bookId);

    //更新图书表库存量 -1
    void updateStock(Integer bookId);

    //更新用户表用户余额 -图书价格
    void updateUserBalance(Integer userId, Integer price);
}
