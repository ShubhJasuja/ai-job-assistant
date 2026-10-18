package com.jobassistant.controller;

import com.jobassistant.entity.Application;
import com.jobassistant.entity.ApplicationEvent;
import com.jobassistant.service.ApplicationService;
import com.jobassistant.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jobassistant.security.JwtService;

@WebMvcTest(ApplicationController.class)
public class ApplicationControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private ApplicationService applicationService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtService jwtService;

    @Test
    @WithMockUser
    void createApplication() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        Application app = Application.builder().id(1L).status(com.jobassistant.enums.ApplicationStatus.DISCOVERED).build();
        when(applicationService.create(10L, 1L)).thenReturn(app);
        mvc.perform(post("/api/v1/applications").with(csrf()).param("jobId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateStatus() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        Application app = Application.builder().id(2L).status(com.jobassistant.enums.ApplicationStatus.SHORTLISTED).build();
        when(applicationService.updateStatus(eq(2L), any(), eq(1L))).thenReturn(app);
        mvc.perform(put("/api/v1/applications/2/status").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHORTLISTED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getEvents() throws Exception {
        when(applicationService.getEventHistory(3L)).thenReturn(Collections.emptyList());
        mvc.perform(get("/api/v1/applications/3/events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void crossUserAccessDeniedViaService() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(applicationService.getEventHistory(3L)).thenThrow(new RuntimeException("does not belong"));
        mvc.perform(get("/api/v1/applications/3/events"))
                .andExpect(status().isInternalServerError());
    }
}
