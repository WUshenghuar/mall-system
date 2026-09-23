package com.mall.web.controller.member;

import com.mall.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest {
    @Test
    void genericMemberUpdateIsNotExposed() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MemberController(mock(MemberService.class))).build();

        mvc.perform(put("/api/member/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":999999,\"level\":2,\"status\":0}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
