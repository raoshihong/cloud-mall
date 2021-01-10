drop table if exists pms_attr;

drop table if exists pms_attr_attrgroup_relation;

drop table if exists pms_attr_group;

drop table if exists pms_brand;

drop table if exists pms_category;

drop table if exists pms_category_brand_relation;

drop table if exists pms_comment_replay;

drop table if exists pms_product_attr_value;

drop table if exists pms_sku_images;

drop table if exists pms_sku_info;

drop table if exists pms_sku_sale_attr_value;

drop table if exists pms_spu_comment;

drop table if exists pms_spu_images;

drop table if exists pms_spu_info;

drop table if exists pms_spu_info_desc;

/*==============================================================*/
/* Table: pms_attr                                              */
/*==============================================================*/
create table pms_attr
(
   attr_id              bigint not null auto_increment comment '??id',
   attr_name            char(30) comment '???',
   search_type          tinyint comment '??????[0-????1-??]',
   icon                 varchar(255) comment '????',
   value_select         char(255) comment '?????[?????]',
   attr_type            tinyint comment '????[0-?????1-?????2-????????????]',
   enable               bigint comment '????[0 - ???1 - ??]',
   catelog_id           bigint comment '????',
   show_desc            tinyint comment '??????????????0-? 1-????sku???????',
   primary key (attr_id)
);

alter table pms_attr comment '????';

/*==============================================================*/
/* Table: pms_attr_attrgroup_relation                           */
/*==============================================================*/
create table pms_attr_attrgroup_relation
(
   id                   bigint not null auto_increment comment 'id',
   attr_id              bigint comment '??id',
   attr_group_id        bigint comment '????id',
   attr_sort            int comment '??????',
   primary key (id)
);

alter table pms_attr_attrgroup_relation comment '??&??????';

/*==============================================================*/
/* Table: pms_attr_group                                        */
/*==============================================================*/
create table pms_attr_group
(
   attr_group_id        bigint not null auto_increment comment '??id',
   attr_group_name      char(20) comment '??',
   sort                 int comment '??',
   descript             varchar(255) comment '??',
   icon                 varchar(255) comment '???',
   catelog_id           bigint comment '????id',
   primary key (attr_group_id)
);

alter table pms_attr_group comment '????';

/*==============================================================*/
/* Table: pms_brand                                             */
/*==============================================================*/
create table pms_brand
(
   brand_id             bigint not null auto_increment comment '??id',
   name                 char(50) comment '???',
   logo                 varchar(2000) comment '??logo??',
   descript             longtext comment '??',
   show_status          tinyint comment '????[0-????1-??]',
   first_letter         char(1) comment '?????',
   sort                 int comment '??',
   primary key (brand_id)
);

alter table pms_brand comment '??';

/*==============================================================*/
/* Table: pms_category                                          */
/*==============================================================*/
create table pms_category
(
   cat_id               bigint not null auto_increment comment '??id',
   name                 char(50) comment '????',
   parent_cid           bigint comment '???id',
   cat_level            int comment '??',
   show_status          tinyint comment '????[0-????1??]',
   sort                 int comment '??',
   icon                 char(255) comment '????',
   product_unit         char(50) comment '????',
   product_count        int comment '????',
   primary key (cat_id)
);

alter table pms_category comment '??????';

/*==============================================================*/
/* Table: pms_category_brand_relation                           */
/*==============================================================*/
create table pms_category_brand_relation
(
   id                   bigint not null auto_increment,
   brand_id             bigint comment '??id',
   catelog_id           bigint comment '??id',
   brand_name           varchar(255),
   catelog_name         varchar(255),
   primary key (id)
);

alter table pms_category_brand_relation comment '??????';

/*==============================================================*/
/* Table: pms_comment_replay                                    */
/*==============================================================*/
create table pms_comment_replay
(
   id                   bigint not null auto_increment comment 'id',
   comment_id           bigint comment '??id',
   reply_id             bigint comment '??id',
   primary key (id)
);

alter table pms_comment_replay comment '????????';

/*==============================================================*/
/* Table: pms_product_attr_value                                */
/*==============================================================*/
create table pms_product_attr_value
(
   id                   bigint not null auto_increment comment 'id',
   spu_id               bigint comment '??id',
   attr_id              bigint comment '??id',
   attr_name            varchar(200) comment '???',
   attr_value           varchar(200) comment '???',
   attr_sort            int comment '??',
   quick_show           tinyint comment '??????????????0-? 1-??',
   primary key (id)
);

alter table pms_product_attr_value comment 'spu???';

/*==============================================================*/
/* Table: pms_sku_images                                        */
/*==============================================================*/
create table pms_sku_images
(
   id                   bigint not null auto_increment comment 'id',
   sku_id               bigint comment 'sku_id',
   img_url              varchar(255) comment '????',
   img_sort             int comment '??',
   default_img          int comment '???[0 - ??????1 - ????]',
   primary key (id)
);

alter table pms_sku_images comment 'sku??';

/*==============================================================*/
/* Table: pms_sku_info                                          */
/*==============================================================*/
create table pms_sku_info
(
   sku_id               bigint not null auto_increment comment 'skuId',
   spu_id               bigint comment 'spuId',
   sku_name             varchar(255) comment 'sku??',
   sku_desc             varchar(2000) comment 'sku????',
   catalog_id           bigint comment '????id',
   brand_id             bigint comment '??id',
   sku_default_img      varchar(255) comment '????',
   sku_title            varchar(255) comment '??',
   sku_subtitle         varchar(2000) comment '???',
   price                decimal(18,4) comment '??',
   sale_count           bigint comment '??',
   primary key (sku_id)
);

alter table pms_sku_info comment 'sku??';

/*==============================================================*/
/* Table: pms_sku_sale_attr_value                               */
/*==============================================================*/
create table pms_sku_sale_attr_value
(
   id                   bigint not null auto_increment comment 'id',
   sku_id               bigint comment 'sku_id',
   attr_id              bigint comment 'attr_id',
   attr_name            varchar(200) comment '?????',
   attr_value           varchar(200) comment '?????',
   attr_sort            int comment '??',
   primary key (id)
);

alter table pms_sku_sale_attr_value comment 'sku????&?';

/*==============================================================*/
/* Table: pms_spu_comment                                       */
/*==============================================================*/
create table pms_spu_comment
(
   id                   bigint not null auto_increment comment 'id',
   sku_id               bigint comment 'sku_id',
   spu_id               bigint comment 'spu_id',
   spu_name             varchar(255) comment '????',
   member_nick_name     varchar(255) comment '????',
   star                 tinyint(1) comment '??',
   member_ip            varchar(64) comment '??ip',
   create_time          datetime comment '????',
   show_status          tinyint(1) comment '????[0-????1-??]',
   spu_attributes       varchar(255) comment '???????',
   likes_count          int comment '???',
   reply_count          int comment '???',
   resources            varchar(1000) comment '????/??[json???[{type:????,url:????}]]',
   content              text comment '??',
   member_icon          varchar(255) comment '????',
   comment_type         tinyint comment '????[0 - ?????????1 - ??????]',
   primary key (id)
);

alter table pms_spu_comment comment '????';

/*==============================================================*/
/* Table: pms_spu_images                                        */
/*==============================================================*/
create table pms_spu_images
(
   id                   bigint not null auto_increment comment 'id',
   spu_id               bigint comment 'spu_id',
   img_name             varchar(200) comment '???',
   img_url              varchar(255) comment '????',
   img_sort             int comment '??',
   default_img          tinyint comment '?????',
   primary key (id)
);

alter table pms_spu_images comment 'spu??';

/*==============================================================*/
/* Table: pms_spu_info                                          */
/*==============================================================*/
create table pms_spu_info
(
   id                   bigint not null auto_increment comment '??id',
   spu_name             varchar(200) comment '????',
   spu_description      varchar(1000) comment '????',
   catalog_id           bigint comment '????id',
   brand_id             bigint comment '??id',
   weight               decimal(18,4),
   publish_status       tinyint comment '????[0 - ???1 - ??]',
   create_time          datetime,
   update_time          datetime,
   primary key (id)
);

alter table pms_spu_info comment 'spu??';

/*==============================================================*/
/* Table: pms_spu_info_desc                                     */
/*==============================================================*/
create table pms_spu_info_desc
(
   spu_id               bigint not null comment '??id',
   decript              longtext comment '????',
   primary key (spu_id)
);

alter table pms_spu_info_desc comment 'spu????';
