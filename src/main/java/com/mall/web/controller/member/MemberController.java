package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.entity.Member;
import com.mall.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('member:list')")
    public Result<Object> page(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                String keyword, Integer level) {
        return Result.success(memberService.selectPage(page, size, keyword, level));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('member:detail')")
    public Result<Member> detail(@PathVariable Long id) {
        return Result.success(memberService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('member:detail')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Member member) {
        member.setId(id);
        memberService.update(member);
        return Result.success(null);
    }

    @PostMapping("/{id}/points")
    @PreAuthorize("hasAuthority('member:points:adjust')")
    public Result<Void> adjustPoints(@PathVariable Long id, @RequestParam int points,
                                      @RequestParam String reason) {
        memberService.adjustPoints(id, points, reason);
        return Result.success(null);
    }
}
