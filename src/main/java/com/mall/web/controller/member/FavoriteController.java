package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.service.FavoriteService;
import com.mall.security.user.CurrentMember;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 提供会员 SPU 收藏增删、列表和收藏状态查询接口。 */
@RestController
@RequestMapping("/api/member/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    /** 维护会员商品收藏关系并查询收藏状态。 */
    private final FavoriteService favoriteService;

    /** 将请求中的 SPU 加入当前会员的收藏夹。 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddReq req, Authentication auth) {
        favoriteService.add(CurrentMember.id(auth), req.getSpuId());
        return Result.success(null);
    }

    /** 移除当前会员对指定 SPU 的收藏。 */
    @DeleteMapping("/{spuId}")
    public Result<Void> delete(@PathVariable Long spuId, Authentication auth) {
        favoriteService.delete(CurrentMember.id(auth), spuId);
        return Result.success(null);
    }

    /** 分页查询当前会员收藏的商品。 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication auth) {
        return Result.success(favoriteService.selectPage(page, size, CurrentMember.id(auth)));
    }

    /** 查询指定 SPU 是否已被当前会员收藏。 */
    @GetMapping("/check/{spuId}")
    public Result<Boolean> check(@PathVariable Long spuId, Authentication auth) {
        return Result.success(favoriteService.isFavorited(CurrentMember.id(auth), spuId));
    }

    /** 添加收藏所需的请求体；SPU ID 不能为空。 */
    @Data
    public static class AddReq {
        @NotNull private Long spuId;
    }
}
