create table if not exists `im_friend_request`
(
    `id`           bigint        not null auto_increment comment 'id',
    `from_user_id` bigint        not null comment '申请人id',
    `to_user_id`   bigint        not null comment '被申请人id',
    `message`      varchar(1024) default null comment '申请留言',
    `status`       tinyint       not null default 0 comment '申请状态 0:待处理 1:通过 2:拒绝 3:撤回',
    `apply_time`   datetime(3)  not null default current_timestamp(3) comment '申请时间',
    `handle_time`  datetime(3)  default null comment '处理时间',
    `version`      bigint       not null default 0 comment '版本号',
    primary key (`id`),
    unique key `idx_from_to_user_id` (`from_user_id`, `to_user_id`),
    key `idx_to_user_status_apply_time` (`to_user_id`, `status`, `apply_time`)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_general_ci comment = '好友申请';
