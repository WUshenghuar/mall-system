package com.mall.system.vo;

import lombok.Data;

@Data
public class SysRoleVO {
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private Integer status;
    private String remark;
}
