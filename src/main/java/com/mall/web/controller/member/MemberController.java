package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.entity.Member;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 提供后台会员档案查询和积分人工调整接口。 */
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    /** 提供后台会员档案及积分操作所需的业务服务。 */
    private final MemberService memberService;

    /** 分页查询会员档案，可按关键字和会员等级筛选。 */
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                String keyword, Integer level) {
        return Result.success(memberService.selectPage(page, size, keyword, level));
    }

    /** 查询指定会员档案，需具备会员详情权限。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:detail')")
    public Result<Member> detail(@PathVariable Long id) {
        return Result.success(memberService.getById(id));
    }

    /** 按指定分值调整会员积分，并记录调整原因。 */
    @PostMapping("/{id}/points")
    @PreAuthorize("hasAuthority('member:points:adjust')")
    public Result<Void> adjustPoints(@PathVariable Long id, @RequestParam int points,
                                      @RequestParam String reason) {
        memberService.adjustPoints(id, points, reason);
        return Result.success(null);
    }
}
