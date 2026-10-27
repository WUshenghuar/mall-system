package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.BrowseHistoryService;
import com.mall.security.user.CurrentMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供当前会员商品浏览历史的写入和分页查询接口。 */
@RestController
@RequestMapping("/api/member/browse")
@RequiredArgsConstructor
public class BrowseController {

    /** 记录商品浏览历史并提供会员历史分页查询。 */
    private final BrowseHistoryService browseHistoryService;

    /** 记录当前会员浏览指定 SPU 的行为。 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddReq req, Authentication auth) {
        browseHistoryService.add(CurrentMember.id(auth), req.getSpuId());
        return Result.success(null);
    }

    /** 分页返回当前会员的浏览历史；默认第 1 页、每页 10 条。 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication auth) {
        return Result.success(browseHistoryService.selectPage(page, size, CurrentMember.id(auth)));
    }

    /** 记录浏览行为所需的请求体；SPU ID 不能为空。 */
    @Data
    public static class AddReq {
        @NotNull private Long spuId;
    }
}
