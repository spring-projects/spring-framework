CREATE
DATABASE `spring`;

use
`spring`;

CREATE TABLE `t_emp`
(
    `id`   int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(20) DEFAULT NULL COMMENT '姓名',
    `age`  int(11) DEFAULT NULL COMMENT '年龄',
    `sex`  varchar(2)  DEFAULT NULL COMMENT '性别',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;