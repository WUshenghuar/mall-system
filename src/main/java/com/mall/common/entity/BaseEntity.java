package com.mall.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础实体类，所有实体类的父类
 * 包含了公共字段：id、创建时间、更新时间和删除标记
 */
@Data
public abstract class BaseEntity {

    /**
     * 主键ID，使用数据库自增策略
     */
    @TableId(type = IdType.AUTO)
    private Long id;
 // MyBatis-Plus的主键注解，设置为自增类型

    /**
     * 创建时间，在插入数据时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
 // MyBatis-Plus的字段注解，插入时自动填充

    /**
     * 更新时间，在插入和更新数据时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE) // MyBatis-Plus的字段注解，插入和更新时自动填充
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记，0表示未删除，1表示已删除
     * 使用@TableLogic注解实现逻辑删除功能
     */
    @TableLogic // MyBatis-Plus的逻辑删除注解
    @TableField("deleted") // 指定数据库字段名为"deleted"
    private Integer deleted;
}