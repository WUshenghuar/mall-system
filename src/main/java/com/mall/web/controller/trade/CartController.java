package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.trade.entity.TradeCart;
import com.mall.trade.service.CartService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public Result<TradeCart> add(@RequestBody AddReq req, Authentication auth) {
        return Result.success(cartService.addCart(CurrentMember.id(auth), req.getSkuId(), req.getQuantity()));
    }

    @GetMapping
    public Result<List<TradeCart>> list(Authentication auth) {
        return Result.success(cartService.getCartList(CurrentMember.id(auth)));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateReq req, Authentication auth) {
        cartService.updateCart(id, CurrentMember.id(auth), req.getQuantity(), req.getChecked());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        cartService.deleteCart(id, CurrentMember.id(auth));
        return Result.success(null);
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody BatchDeleteReq req, Authentication auth) {
        cartService.deleteBatch(CurrentMember.id(auth), req.getIds());
        return Result.success(null);
    }

    @GetMapping("/count")
    public Result<Long> count(Authentication auth) {
        return Result.success(cartService.getCartCount(CurrentMember.id(auth)));
    }

    @Data
    public static class AddReq {
        @NotNull private Long skuId;
        @Min(1) private Integer quantity = 1;
    }

    @Data
    public static class UpdateReq {
        private Integer quantity;
        private Integer checked;
    }

    @Data
    public static class BatchDeleteReq {
        @NotNull private List<Long> ids;
    }
}
