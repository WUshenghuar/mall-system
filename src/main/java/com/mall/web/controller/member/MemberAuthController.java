package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.MemberAuthService;
import com.mall.security.jwt.JwtTokenProvider;
import com.mall.security.user.MemberPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/member/auth")
@RequiredArgsConstructor
public class MemberAuthController {
    private final MemberAuthService memberAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        memberAuthService.register(req.getPhone(), req.getPassword(), req.getNickName());
        return Result.success(null);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        MemberPrincipal member = memberAuthService.authenticate(req.getPhone(), req.getPassword());
        return Result.success(Map.of("token", jwtTokenProvider.generateMemberToken(member.getMemberId(), member.getPhone()),
                "memberId", member.getMemberId(), "phone", member.getPhone()));
    }

    @GetMapping("/profile")
    public Result<?> profile(Authentication authentication) {
        MemberPrincipal member = requireMember(authentication);
        return Result.success(memberAuthService.getProfile(member.getMemberId()));
    }

    private MemberPrincipal requireMember(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof MemberPrincipal member)) {
            throw new com.mall.common.exception.BusinessException(403, "仅会员账户可访问");
        }
        return member;
    }

    @Data public static class LoginReq { @NotBlank @Pattern(regexp = "^1\\d{10}$") private String phone; @NotBlank private String password; }
    @Data public static class RegisterReq { @NotBlank @Pattern(regexp = "^1\\d{10}$") private String phone; @NotBlank private String password; @NotBlank private String nickName; }
}
