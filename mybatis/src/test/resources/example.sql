-- set global validate_password.policy=0;
-- set global validate_password.length=4;



CREATE DATABASE  example;
CREATE USER 'example'@'%' IDENTIFIED BY 'example';

GRANT ALL ON example.* to 'example'@'%';

FLUSH privileges;


CREATE TABLE `users` (
	`id`  int  UNSIGNED auto_increment   PRIMARY KEY,
	`name` varchar(64)  NOT NULL COMMENT '用户名'
) DEFAULT CHARSET=utf8mb4 COMMENT '用户表';

INSERT INTO `users`( `name`) VALUES ('test1');
INSERT INTO `users`( `name`) VALUES ('test2');
INSERT INTO `users`( `name`) VALUES ('test3');
INSERT INTO `users`( `name`) VALUES ('test4');
INSERT INTO `users`( `name`) VALUES ('test5');
INSERT INTO `users`( `name`) VALUES ('test6');
INSERT INTO `users`( `name`) VALUES ('test7');
INSERT INTO `users`( `name`) VALUES ('test8');
INSERT INTO `users`( `name`) VALUES ('test9');
INSERT INTO `users`( `name`) VALUES ('test10');
INSERT INTO `users`( `name`) VALUES ('test11');
INSERT INTO `users`( `name`) VALUES ('test12');
INSERT INTO `users`( `name`) VALUES ('test13');
INSERT INTO `users`( `name`) VALUES ('test14');
INSERT INTO `users`( `name`) VALUES ('test15');