package com.mall.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.system.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(@Param("username") String username);

    @Select("SELECT r.role_key FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    @Select("<script>SELECT ur.user_id, r.role_key FROM sys_role r INNER JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id IN <foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND r.deleted = 0</script>")
    List<Map<String, Object>> selectRoleKeysByUserIds(@Param("userIds") List<Long> userIds);
}