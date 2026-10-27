package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.MemberAuthService;
import com.mall.member.service.MemberService;
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

/** 提供 C 端会员注册、登录、资料和积分流水接口，并拒绝后台主体访问会员资源。 */
@RestController
@RequestMapping("/api/member/auth")
@RequiredArgsConstructor
public class MemberAuthController {
    /** 处理会员凭证验证、注册及个人资料查询。 */
    private final MemberAuthService memberAuthService;
    /** 查询会员积分流水。 */
    private final MemberService memberService;
    /** 为验证通过的会员生成访问令牌。 */
    private final JwtTokenProvider jwtTokenProvider;

    /** 注册会员账户；手机号、密码和昵称等字段先经请求校验。 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        memberAuthService.register(req.getPhone(), req.getPassword(), req.getNickName());
        return Result.success(null);
    }

    /** 验证会员手机号和密码，返回会员 JWT 及前端所需的会员标识信息。 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        MemberPrincipal member = memberAuthService.authenticate(req.getPhone(), req.getPassword());
        return Result.success(Map.of("token", jwtTokenProvider.generateMemberToken(member.getMemberId(), member.getPhone()),
                "memberId", member.getMemberId(), "phone", member.getPhone()));
    }

    /** 查询当前登录会员资料；拒绝使用后台登录主体访问会员资料。 */
    @GetMapping("/profile")
    public Result<?> profile(Authentication authentication) {
        MemberPrincipal member = requireMember(authentication);
        return Result.success(memberAuthService.getProfile(member.getMemberId()));
    }

    /** 查询当前会员的积分变动流水。 */
    @GetMapping("/points/log")
    public Result<?> pointsLog(Authentication authentication) {
        return Result.success(memberService.listPointsLogs(requireMember(authentication).getMemberId()));
    }

    /** 确认认证主体是会员类型，避免后台用户主体被当作会员主体使用。 */
    private MemberPrincipal requireMember(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof MemberPrincipal member)) {
            throw new com.mall.common.exception.BusinessException(403, "仅会员账户可访问");
        }
        return member;
    }

    /** 会员登录请求；手机号需符合当前接口采用的 11 位手机号格式。 */
    @Data public static class LoginReq { @NotBlank @Pattern(regexp = "^1\\d{10}$") private String phone; @NotBlank private String password; }
    /** 会员注册请求；手机号、密码和昵称均为必填字段。 */
    @Data public static class RegisterReq { @NotBlank @Pattern(regexp = "^1\\d{10}$") private String phone; @NotBlank private String password; @NotBlank private String nickName; }
}
