package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 提供当前登录会员专属的购物车增删改查和条目计数接口。 */
@RestController
@RequestMapping("/api/trade/cart")
@RequiredArgsConstructor
public class CartController {

    /** 提供当前会员购物车的查询、条目维护和数量统计。 */
    private final CartService cartService;

    /** 将 SKU 和购买数量加入当前会员购物车。 */
    @PostMapping
    public Result<TradeCart> add(@Valid @RequestBody AddReq req, Authentication auth) {
        return Result.success(cartService.addCart(CurrentMember.id(auth), req.getSkuId(), req.getQuantity()));
    }

    /** 查询当前会员的购物车条目。 */
    @GetMapping
    public Result<List<TradeCart>> list(Authentication auth) {
        return Result.success(cartService.getCartList(CurrentMember.id(auth)));
    }

    /** 更新当前会员购物车条目的数量或勾选状态。 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateReq req, Authentication auth) {
        cartService.updateCart(id, CurrentMember.id(auth), req.getQuantity(), req.getChecked());
        return Result.success(null);
    }

    /** 删除当前会员指定购物车条目。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        cartService.deleteCart(id, CurrentMember.id(auth));
        return Result.success(null);
    }

    /** 批量删除当前会员提交的购物车条目 ID。 */
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@Valid @RequestBody BatchDeleteReq req, Authentication auth) {
        cartService.deleteBatch(CurrentMember.id(auth), req.getIds());
        return Result.success(null);
    }

    /** 查询当前会员购物车的条目计数。 */
    @GetMapping("/count")
    public Result<Long> count(Authentication auth) {
        return Result.success(cartService.getCartCount(CurrentMember.id(auth)));
    }

    /** 添加购物车请求；SKU 必填，购买数量至少为 1，默认购买 1 件。 */
    @Data
    public static class AddReq {
        @NotNull private Long skuId;
        @Min(1) private Integer quantity = 1;
    }

    /** 更新购物车请求；可提交数量和 0/1 勾选状态。 */
    @Data
    public static class UpdateReq {
        @Min(1) private Integer quantity;
        @Min(0) @Max(1) private Integer checked;
    }

    /** 批量删除请求；购物车条目 ID 列表不能为空。 */
    @Data
    public static class BatchDeleteReq {
        @NotEmpty private List<Long> ids;
    }
}
