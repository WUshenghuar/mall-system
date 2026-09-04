package com.mall.web.controller.member;

import com.mall.common.result.Result;
import com.mall.member.entity.MemberAddress;
import com.mall.member.service.AddressService;
import com.mall.security.user.CurrentMember;
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
        return Result.success(addressService.getList(CurrentMember.id(auth)));
    }

    @GetMapping("/{id}")
    public Result<MemberAddress> detail(@PathVariable Long id, Authentication auth) {
        MemberAddress address = addressService.getById(id);
        if (address == null || !CurrentMember.id(auth).equals(address.getUserId())) {
            throw new com.mall.common.exception.BusinessException("地址不存在或无权访问");
        }
        return Result.success(address);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody MemberAddress address, Authentication auth) {
        address.setUserId(CurrentMember.id(auth));
        addressService.add(address);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MemberAddress address, Authentication auth) {
        MemberAddress existing = addressService.getById(id);
        if (existing == null || !CurrentMember.id(auth).equals(existing.getUserId())) throw new com.mall.common.exception.BusinessException("无权操作该地址");
        address.setId(id);
        addressService.update(address);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        MemberAddress existing = addressService.getById(id);
        if (existing == null || !CurrentMember.id(auth).equals(existing.getUserId())) throw new com.mall.common.exception.BusinessException("无权操作该地址");
        addressService.delete(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id, Authentication auth) {
        MemberAddress existing = addressService.getById(id);
        if (existing == null || !CurrentMember.id(auth).equals(existing.getUserId())) {
            throw new com.mall.common.exception.BusinessException("无权操作该地址");
        }
        addressService.setDefault(id, CurrentMember.id(auth));
        return Result.success(null);
    }
}
