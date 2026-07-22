package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.entity.MemberAddress;
import com.mall.member.service.AddressService;
import com.mall.security.user.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public Result<List<MemberAddress>> list(Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        return Result.success(addressService.getList(user.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<MemberAddress> detail(@PathVariable Long id) {
        return Result.success(addressService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody MemberAddress address, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        address.setUserId(user.getUserId());
        addressService.add(address);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MemberAddress address) {
        address.setId(id);
        addressService.update(address);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id, Authentication auth) {
        LoginUser user = (LoginUser) auth.getPrincipal();
        addressService.setDefault(id, user.getUserId());
        return Result.success(null);
    }
}
