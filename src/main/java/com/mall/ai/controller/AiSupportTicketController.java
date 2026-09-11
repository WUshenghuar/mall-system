package com.mall.ai.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.ai.entity.AiSupportTicket;
import com.mall.ai.service.AiSupportTicketService;
import com.mall.common.result.Result;
import com.mall.security.user.CurrentMember;
import com.mall.security.user.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/tickets")
@RequiredArgsConstructor
public class AiSupportTicketController {
    private final AiSupportTicketService ticketService;

    @PostMapping
    public Result<AiSupportTicket> create(@Valid @RequestBody HandoffReq req, Authentication auth) {
        return Result.success(ticketService.create(CurrentMember.id(auth), req.getConversationId(), req.getMessage()));
    }

    @GetMapping("/mine")
    public Result<IPage<AiSupportTicket>> mine(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                Authentication auth) {
        return Result.success(ticketService.selectMemberPage(CurrentMember.id(auth), page, size));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('order:support:list')")
    public Result<IPage<AiSupportTicket>> page(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(ticketService.selectAdminPage(page, size, status));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('order:support:handle')")
    public Result<Void> claim(@PathVariable Long id, Authentication auth) {
        ticketService.claim(id, ((LoginUser) auth.getPrincipal()).getUserId());
        return Result.success(null);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('order:support:handle')")
    public Result<Void> resolve(@PathVariable Long id, @Valid @RequestBody ResolveReq req) {
        ticketService.resolve(id, req.getNote());
        return Result.success(null);
    }

    @Data
    public static class HandoffReq {
        @Size(max = 64) private String conversationId;
        @NotBlank @Size(max = 1000) private String message;
    }

    @Data
    public static class ResolveReq {
        @Size(max = 500) private String note;
    }
}
