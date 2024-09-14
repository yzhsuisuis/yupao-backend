create schema user_center;
use user_center;
create table user
(
    id           bigint auto_increment
        primary key,
    username     varchar(256)                       null comment '用户昵称',
    avatarUrl    varchar(1024)                      null comment '用户头像',
    userAccount  varchar(256)                       null comment '账号',
    gender       tinyint                            null comment '性别',
    userPassword varchar(128)                       null comment '密码',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 null comment '状态 0 -正常',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '逻辑删除',
    userRole     int      default 0                 null comment '用户权限 默认为0 管理员为1',
    planetCode   varchar(512)                       null comment '星球编号'
);


create table tag
(
    id         bigint auto_increment comment 'id'
        primary key,
    tagName    varchar(256)                       null comment '标签名称',
    userId     bigint                             null comment '用户id',
    isParent   tinyint                            null comment '0 - 不是，1 - 父标签',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete   tinyint  default 0                 null comment '是否删除',
    constraint uniIdx_tagName
        unique (tagName)
)
    comment '标签';
-- 给tag加一个索引
create index idx_userId_
    on tag (userId);

