create table `im_user`
(
    `id`               bigint       not null auto_increment primary key comment 'id',
    `user_name`        varchar(255) not null comment '用户名',
    `nick_name`        varchar(255) not null comment '用户昵称',
    `head_image`       varchar(255)  default '' comment '用户头像',
    `head_image_thumb` varchar(255)  default '' comment '用户头像缩略图',
    `password`         varchar(255) not null comment '密码',
    `sex`              tinyint       default 0 comment '性别 0:男 1:女',
    `is_banned`        tinyint(1) default 0 comment '是否被封禁 0:否 1:是',
    `reason`           varchar(255)  default '' comment '被封禁原因',
    `type`             smallint      default 1 comment '用户类型 1:普通用户 2:审核账户',
    `signature`        varchar(1024) default '' comment '个性签名',
    `last_login_time`  datetime      default null comment '最后登录时间',
    `created_time`     datetime      default current_timestamp comment '创建时间',
    `is_deleted` int default 0 comment '是否删除 0true 1false',
    unique key `idx_user_name` (user_name),
    key `idx_nick_name` (nick_name)
) engine = innodb charset = utf8mb4 comment '用户';

create table `im_friend`
(
    `id`                bigint       not null auto_increment primary key comment 'id',
    `user_id`           bigint       not null comment '用户id',
    `friend_id`         bigint       not null comment '好友id',
    `friend_nick_name`  varchar(255) not null comment '好友昵称',
    `friend_head_image` varchar(255) default '' comment '好友头像',
    `is_dnd`            tinyint      default 0 comment '免打扰标识(do not disturb)  0:关闭   1:开启',
    `is_top`            tinyint(1)   default 0 comment '是否置顶会话',
    `deleted`           tinyint      default 0 comment '删除标识  0：正常   1：已删除',
    `created_time`      datetime     default current_timestamp comment '创建时间',
    `version`           BIGINT       default 0 comment '版本号',
    UNIQUE KEY `idx_user_friend_id` (`user_id`, `friend_id`),
    key                 `idx_friend_id` (`friend_id`)
) engine = innodb charset = utf8mb4 comment '好友';

create table `im_private_message`
(
    `id`        bigint      not null auto_increment primary key comment 'id',
    `local_id`  varchar(64) comment '业务id,由前端生成',
    `seq_no`    int         not null comment '序列号,单个会话消息的序号连续递增',
    `send_id`   bigint      not null comment '发送用户id',
    `recv_id`   bigint      not null comment '接收用户id',
    `conv_key`  varchar(64) not null comment '会话key，格式:userId1_userId2',
    `content`   text character set utf8mb4 comment '发送内容',
    `type`      tinyint     not null comment '消息类型 0:文字 1:图片 2:文件 3:语音 4:视频 21:提示',
    `status`    tinyint     not null comment '状态 0:未读 1:已发送 2:撤回 3:已读',
    `send_time` datetime(3) default current_timestamp(3) comment '发送时间',
    key         `idx_conv_key_seq_no`(`conv_key`,`seq_no`),
    key         `idx_send_recv_id` (`send_id`, `recv_id`,`id`),
    key         `idx_recv_id` (`recv_id`)
) engine = innodb charset = utf8mb4 comment '私聊消息';