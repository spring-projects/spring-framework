CREATE TABLE `t_book`
(
    `book_id`   int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_name` varchar(20) DEFAULT NULL COMMENT '图书名称',
    `price`     int(11) DEFAULT NULL COMMENT '价格',
    `stock`     int(10) unsigned DEFAULT NULL COMMENT '库存（无符号）',
    PRIMARY KEY (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

insert into `t_book`(`book_id`, `book_name`, `price`, `stock`)
values (1, '斗破苍穹', 80, 100),
       (2, '斗罗大陆', 50, 100);

CREATE TABLE `t_user`
(
    `user_id`  int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` varchar(20) DEFAULT NULL COMMENT '用户名',
    `balance`  int(10) unsigned DEFAULT NULL COMMENT '余额（无符号）',
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

insert into `t_user`(`user_id`, `username`, `balance`)
values (1, 'admin', 50);