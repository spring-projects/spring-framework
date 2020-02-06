package com.luban.service;

import com.luban.anno.Luban;
import com.luban.dao.TestDao;
import com.luban.dao.UserDao;

@Luban("bb")
public class UserServiceImpl implements UserService {


    UserDao dao;

    @Override
    public void find() {
        System.out.println("service");
        dao.query();
    }

    //public void setDao(UserDao dao) {
       // this.dao = dao;
   // }
}
