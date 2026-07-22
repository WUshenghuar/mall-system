package com.mall.web.controller.trade;

import com.mall.common.result.Result;
import com.mall.security.user.LoginUser;
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
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(cartService.addCart(user.getUserId(), req.getSkuId(), req.getQuantity()));
    }

    @GetMapping
    public Result<List<TradeCart>> list(Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(cartService.getCartList(user.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UpdateReq req) {
        cartService.updateCart(id, req.getQuantity(), req.getChecked());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.deleteCart(id);
        return Result.success(null);
    }

    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody BatchDeleteReq req, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        cartService.deleteBatch(user.getUserId(), req.getIds());
        return Result.success(null);
    }

    @GetMapping("/count")
    public Result<Long> count(Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(cartService.getCartCount(user.getUserId()));
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
