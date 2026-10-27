package com.mall.system.vo;

import lombok.Data;

/**
 * 系统角色视图对象(SysRoleVO)
 * 用于封装和传输系统角色相关的数据
 * 使用@Data注解自动生成getter、setter、toString等方法
 */
@Data
public class SysRoleVO {
    private Long id;            // 角色ID，唯一标识
    private String roleName;    // 角色名称
    private String roleKey;     // 角色标识，如'admin'等
    private Integer roleSort;   // 角色排序值
    private Integer status;     // 角色状态，如0-禁用，1-正常
    private String remark;      // 备注信息
}
