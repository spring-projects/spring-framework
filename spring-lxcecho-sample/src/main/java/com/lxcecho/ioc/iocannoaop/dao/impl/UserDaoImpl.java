package com.lxcecho.ioc.iocannoaop.dao.impl;

import com.lxcecho.ioc.iocannoaop.dao.UserDao;
import org.springframework.stereotype.Repository;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
@Repository
public class UserDaoImpl  implements UserDao {
    @Override
    public void add() {
        System.out.println("dao........");
    }

}
