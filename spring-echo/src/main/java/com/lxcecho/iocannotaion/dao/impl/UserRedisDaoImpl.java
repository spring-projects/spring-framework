package com.lxcecho.iocannotaion.dao.impl;

import com.lxcecho.iocannotaion.dao.UserDao;
import org.springframework.stereotype.Repository;

@Repository
public class UserRedisDaoImpl implements UserDao {
    @Override
    public void add() {
        System.out.println("dao redis.........");
    }

}
