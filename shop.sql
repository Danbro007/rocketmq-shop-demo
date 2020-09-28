/*
 Navicat Premium Data Transfer

 Source Server         : test服务器
 Source Server Type    : MySQL
 Source Server Version : 50646
 Source Host           : localhost:3306
 Source Schema         : trade

 Target Server Type    : MySQL
 Target Server Version : 50646
 File Encoding         : 65001

 Date: 27/09/2020 22:36:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for trade_coupon
-- ----------------------------
DROP TABLE IF EXISTS `trade_coupon`;
CREATE TABLE `trade_coupon`  (
  `coupon_id` bigint(50) NOT NULL COMMENT '优惠券ID',
  `coupon_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '优惠券金额',
  `user_id` bigint(50) NULL DEFAULT NULL COMMENT '用户ID',
  `order_id` bigint(32) NULL DEFAULT NULL COMMENT '订单ID',
  `is_used` int(1) NULL DEFAULT NULL COMMENT '是否使用 0未使用 1已使用',
  `used_time` timestamp(0) NULL DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`coupon_id`) USING BTREE,
  INDEX `FK_trade_coupon`(`user_id`) USING BTREE,
  INDEX `FK_trade_coupon2`(`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of trade_coupon
-- ----------------------------
INSERT INTO `trade_coupon` VALUES (345988230098857984, 20.00, 345963634385633280, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for trade_goods
-- ----------------------------
DROP TABLE IF EXISTS `trade_goods`;
CREATE TABLE `trade_goods`  (
  `goods_id` bigint(50) NOT NULL AUTO_INCREMENT,
  `goods_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `goods_number` int(11) NULL DEFAULT NULL COMMENT '商品库存',
  `goods_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '商品价格',
  `goods_desc` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品描述',
  `add_time` timestamp(0) NULL DEFAULT NULL COMMENT '添加时间',
  PRIMARY KEY (`goods_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 345959443973935105 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of trade_goods
-- ----------------------------
INSERT INTO `trade_goods` VALUES (345959443973935104, '华为P30', 999, 1000.00, '夜间拍照更美', '2019-07-09 20:38:00');

-- ----------------------------
-- Table structure for trade_goods_number_log
-- ----------------------------
DROP TABLE IF EXISTS `trade_goods_number_log`;
CREATE TABLE `trade_goods_number_log`  (
  `goods_id` bigint(50) NOT NULL COMMENT '商品ID',
  `order_id` bigint(50) NOT NULL COMMENT '订单ID',
  `goods_number` int(11) NULL DEFAULT NULL COMMENT '库存数量',
  `log_time` timestamp(0) NULL DEFAULT NULL,
  PRIMARY KEY (`goods_id`, `order_id`) USING BTREE,
  INDEX `FK_trade_goods_number_log2`(`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for trade_mq_consumer_log
-- ----------------------------
DROP TABLE IF EXISTS `trade_mq_consumer_log`;
CREATE TABLE `trade_mq_consumer_log`  (
  `msg_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `group_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `msg_tag` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `msg_key` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `msg_body` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `consumer_status` int(1) NULL DEFAULT NULL COMMENT '0:正在处理;1:处理成功;2:处理失败',
  `consumer_times` int(1) NULL DEFAULT NULL,
  `consumer_timestamp` timestamp(0) NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`group_name`, `msg_tag`, `msg_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for trade_mq_producer_temp
-- ----------------------------
DROP TABLE IF EXISTS `trade_mq_producer_temp`;
CREATE TABLE `trade_mq_producer_temp`  (
  `id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `group_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `msg_topic` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `msg_tag` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `msg_key` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `msg_body` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `msg_status` int(1) NULL DEFAULT NULL COMMENT '0:未处理;1:已经处理',
  `create_time` timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for trade_order
-- ----------------------------
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order`  (
  `order_id` bigint(50) NOT NULL COMMENT '订单ID',
  `user_id` bigint(50) NULL DEFAULT NULL COMMENT '用户ID',
  `order_status` int(1) NULL DEFAULT NULL COMMENT '订单状态 0未确认 1已确认 2已取消 3无效 4退款',
  `pay_status` int(1) NULL DEFAULT NULL COMMENT '支付状态 0未支付 1支付中 2已支付',
  `shipping_status` int(1) NULL DEFAULT NULL COMMENT '发货状态 0未发货 1已发货 2已收货',
  `address` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '收货地址',
  `consignee` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '收货人',
  `goods_id` bigint(50) NULL DEFAULT NULL COMMENT '商品ID',
  `goods_number` int(11) NULL DEFAULT NULL COMMENT '商品数量',
  `goods_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '商品价格',
  `goods_amount` decimal(10, 0) NULL DEFAULT NULL COMMENT '商品总价',
  `shipping_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '运费',
  `order_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '订单价格',
  `coupon_id` bigint(50) NULL DEFAULT NULL COMMENT '优惠券ID',
  `coupon_paid` decimal(10, 2) NULL DEFAULT NULL COMMENT '优惠券',
  `money_paid` decimal(10, 2) NULL DEFAULT NULL COMMENT '已付金额',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额',
  `add_time` timestamp(0) NULL DEFAULT NULL COMMENT '创建时间',
  `confirm_time` timestamp(0) NULL DEFAULT NULL COMMENT '订单确认时间',
  `pay_time` timestamp(0) NULL DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`order_id`) USING BTREE,
  INDEX `FK_trade_order`(`user_id`) USING BTREE,
  INDEX `FK_trade_order2`(`goods_id`) USING BTREE,
  INDEX `FK_trade_order3`(`coupon_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for trade_pay
-- ----------------------------
DROP TABLE IF EXISTS `trade_pay`;
CREATE TABLE `trade_pay`  (
  `pay_id` bigint(50) NOT NULL COMMENT '支付编号',
  `order_id` bigint(50) NULL DEFAULT NULL COMMENT '订单编号',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额',
  `is_paid` int(1) NULL DEFAULT NULL COMMENT '是否已支付 1否 2是',
  PRIMARY KEY (`pay_id`) USING BTREE,
  INDEX `FK_trade_pay`(`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for trade_user
-- ----------------------------
DROP TABLE IF EXISTS `trade_user`;
CREATE TABLE `trade_user`  (
  `user_id` bigint(50) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户姓名',
  `user_password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户密码',
  `user_mobile` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `user_score` int(11) NULL DEFAULT NULL COMMENT '积分',
  `user_reg_time` timestamp(0) NULL DEFAULT NULL COMMENT '注册时间',
  `user_money` decimal(10, 0) NULL DEFAULT NULL COMMENT '用户余额',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 345963634385633281 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of trade_user
-- ----------------------------
INSERT INTO `trade_user` VALUES (345963634385633280, '刘备', '123L', '18888888888L', 100, '2019-07-09 13:37:03', 900);

-- ----------------------------
-- Table structure for trade_user_money_log
-- ----------------------------
DROP TABLE IF EXISTS `trade_user_money_log`;
CREATE TABLE `trade_user_money_log`  (
  `user_id` bigint(50) NOT NULL COMMENT '用户ID',
  `order_id` bigint(50) NOT NULL COMMENT '订单ID',
  `money_log_type` int(1) NOT NULL COMMENT '日志类型 1订单付款 2 订单退款',
  `use_money` decimal(10, 2) NULL DEFAULT NULL,
  `create_time` timestamp(0) NULL DEFAULT NULL COMMENT '日志时间',
  PRIMARY KEY (`user_id`, `order_id`, `money_log_type`) USING BTREE,
  INDEX `FK_trade_user_money_log2`(`order_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
