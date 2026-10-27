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

/** 提供会员收货地址维护入口，并在控制器层校验每条地址的所有权。 */
@RestController
@RequestMapping("/api/member/address")
@RequiredArgsConstructor
public class AddressController {

    /** 处理会员收货地址的查询、增删改及默认地址设置。 */
    private final AddressService addressService;

    /** 查询当前会员的全部收货地址。 */
    @GetMapping
    public Result<List<MemberAddress>> list(Authentication auth) {
        return Result.success(addressService.getList(CurrentMember.id(auth)));
    }

    /** 查询地址详情，并确认该地址属于当前登录会员。 */
    @GetMapping("/{id}")
    public Result<MemberAddress> detail(@PathVariable Long id, Authentication auth) {
        MemberAddress address = addressService.getById(id);
        if (address == null || !CurrentMember.id(auth).equals(address.getUserId())) {
            throw new com.mall.common.exception.BusinessException("地址不存在或无权访问");
        }
        return Result.success(address);
    }

    /** 新增收货地址；会员 ID 从认证主体中获取，不接受请求体指定归属。 */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody MemberAddress address, Authentication auth) {
        address.setUserId(CurrentMember.id(auth));
        addressService.add(address);
        return Result.success(null);
    }

    /** 更新地址前验证所有权，并以路径 ID 作为待更新地址的主键。 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MemberAddress address, Authentication auth) {
        MemberAddress existing = addressService.getById(id);
        if (existing == null || !CurrentMember.id(auth).equals(existing.getUserId())) throw new com.mall.common.exception.BusinessException("无权操作该地址");
        address.setId(id);
        addressService.update(address);
        return Result.success(null);
    }

    /** 删除地址前验证所有权，避免通过猜测 ID 操作其他会员的地址。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        MemberAddress existing = addressService.getById(id);
        if (existing == null || !CurrentMember.id(auth).equals(existing.getUserId())) throw new com.mall.common.exception.BusinessException("无权操作该地址");
        addressService.delete(id);
        return Result.success(null);
    }

    /** 将指定地址设为当前会员的默认地址；服务负责维护默认地址约束。 */
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
