package com.jobassistant.controller;

import com.jobassistant.dto.InterviewPreparationRequest;
import com.jobassistant.dto.InterviewPreparationResponse;
import com.jobassistant.service.CurrentUserService;
import com.jobassistant.service.InterviewPreparationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewPreparationController.class)
public class InterviewPreparationControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private InterviewPreparationService service;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private com.jobassistant.security.JwtService jwtService;

    @Test
    @WithMockUser
    void createInterviewPreparation() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(service.create(any())).thenReturn(new InterviewPreparationResponse(1L, null, null, "Q?", "Tips"));
        mvc.perform(post("/api/v1/interview-preparation").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suggestedQuestions\":\"Q?\",\"tips\":\"Tips\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void listInterviewPreparations() throws Exception {
        when(service.listForUser()).thenReturn(Collections.emptyList());
        mvc.perform(get("/api/v1/interview-preparation"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void crossUserAccessDeniedViaService() throws Exception {
        when(service.getById(99L)).thenThrow(new RuntimeException("does not belong"));
        mvc.perform(get("/api/v1/interview-preparation/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void unauthenticatedDenied() throws Exception {
        mvc.perform(get("/api/v1/interview-preparation"))
                .andExpect(status().isUnauthorized());
    }
}
