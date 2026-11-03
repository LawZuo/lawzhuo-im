-- 好友表
-- 线上环境该表此前是手工建的，这里用 IF NOT EXISTS 保证新库能自动创建、已有库不报错
create table if not exists `im_friend`
(
    `id`                bigint(20)   not null auto_increment comment 'id',
    `user_id`           bigint(20)   not null comment '用户id',
    `friend_id`         bigint(20)   not null comment '好友id',
    `friend_nick_name`  varchar(255) not null comment '好友昵称',
    `friend_head_image` varchar(255) default '' comment '好友头像',
    `is_dnd`            tinyint(4)   default 0 comment '免打扰标识(do not disturb) 0:关闭 1:开启',
    `is_top`            tinyint(1)   default 0 comment '是否置顶会话',
    `deleted`           tinyint(4)   default 0 comment '删除标识 0:正常 1:已删除',
    `created_time`      datetime     default current_timestamp comment '创建时间',
    `version`           bigint(20)   default 0 comment '版本号',
    primary key (`id`),
    unique key `idx_user_friend_id` (`user_id`, `friend_id`),
    key `idx_friend_id` (`friend_id`)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment ='好友';
