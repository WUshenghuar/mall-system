package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.FavoriteService;
import com.mall.security.user.CurrentMember;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public Result<Void> add(@RequestBody AddReq req, Authentication auth) {
        favoriteService.add(CurrentMember.id(auth), req.getSpuId());
        return Result.success(null);
    }

    @DeleteMapping("/{spuId}")
    public Result<Void> delete(@PathVariable Long spuId, Authentication auth) {
        favoriteService.delete(CurrentMember.id(auth), spuId);
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication auth) {
        return Result.success(favoriteService.selectPage(page, size, CurrentMember.id(auth)));
    }

    @GetMapping("/check/{spuId}")
    public Result<Boolean> check(@PathVariable Long spuId, Authentication auth) {
        return Result.success(favoriteService.isFavorited(CurrentMember.id(auth), spuId));
    }

    @Data
    public static class AddReq {
        @NotNull private Long spuId;
    }
}
