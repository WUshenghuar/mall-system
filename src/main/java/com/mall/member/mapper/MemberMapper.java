package com.mall.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.member.entity.Member;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MemberMapper extends BaseMapper<Member> {
    @Select("SELECT * FROM mm_member WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Member selectByIdForUpdate(@Param("id") Long id);
}
