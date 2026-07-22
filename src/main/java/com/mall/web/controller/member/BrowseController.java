package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.BrowseHistoryService;
import com.mall.security.user.LoginUser;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/browse")
@RequiredArgsConstructor
public class BrowseController {

    private final BrowseHistoryService browseHistoryService;

    @PostMapping
    public Result<Void> add(@RequestBody AddReq req, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        browseHistoryService.add(user.getUserId(), req.getSpuId());
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(browseHistoryService.selectPage(page, size, user.getUserId()));
    }

    @Data
    public static class AddReq {
        @NotNull private Long spuId;
    }
}
