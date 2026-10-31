package com.itsm.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Security and REST behaviour through the real HTTP layer. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebSecurityIT {

    @Autowired MockMvc mvc;

    @Test
    @DisplayName("TC-44 Anonymous browsers are sent to the login page; anonymous API calls get 401")
    void anonymousRedirected() throws Exception {
        mvc.perform(get("/incidents").accept(MediaType.TEXT_HTML)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/incidents")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-45 Invalid login is rejected")
    void invalidLogin() throws Exception {
        mvc.perform(post("/login").param("email", "admin@itsm.local").param("password", "wrong").with(csrf()))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    @WithUserDetails("rohan@itsm.local")
    @DisplayName("TC-46 Employees cannot open administrator pages")
    void employeeBlockedFromAdmin() throws Exception {
        mvc.perform(get("/admin/settings")).andExpect(status().isForbidden());
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("employee/home"));
    }

    @Test
    @WithUserDetails("admin@itsm.local")
    @DisplayName("TC-47 Each role gets its own home page")
    void adminDashboard() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("admin/dashboard"));
        mvc.perform(get("/admin/reports")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("admin@itsm.local")
    @DisplayName("TC-48 REST API returns problem JSON for missing incidents and validates input")
    void apiErrors() throws Exception {
        mvc.perform(get("/api/incidents/99999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("ResourceNotFound"));
        mvc.perform(post("/api/incidents").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"description\":\"\",\"affectedUsers\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("rohan@itsm.local")
    @DisplayName("TC-49 Live analysis API suggests category and predicts priority")
    void analyzeApi() throws Exception {
        mvc.perform(post("/api/intelligence/analyze").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"VPN keeps disconnecting\",\"description\":\"vpn drops every few minutes\","
                                + "\"serviceId\":5,\"impact\":\"MEDIUM\",\"urgency\":\"HIGH\",\"affectedUsers\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Network"))
                .andExpect(jsonPath("$.priority.level").value("P2"));
    }
}
