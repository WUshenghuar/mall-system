package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.BrowseHistoryService;
import com.mall.security.user.CurrentMember;
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
        browseHistoryService.add(CurrentMember.id(auth), req.getSpuId());
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication auth) {
        return Result.success(browseHistoryService.selectPage(page, size, CurrentMember.id(auth)));
    }

    @Data
    public static class AddReq {
        @NotNull private Long spuId;
    }
}
