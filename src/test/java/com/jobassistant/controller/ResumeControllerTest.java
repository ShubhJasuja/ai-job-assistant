package com.jobassistant.controller;
import org.springframework.security.test.context.support.WithMockUser;

import com.jobassistant.dto.ResumeRequest;
import com.jobassistant.dto.ResumeResponse;
import com.jobassistant.entity.Resume;
import com.jobassistant.entity.User;
import com.jobassistant.repository.ResumeRepository;
import com.jobassistant.service.CurrentUserService;
import com.jobassistant.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jobassistant.security.JwtService;

@WebMvcTest(ResumeController.class)
public class ResumeControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private ResumeService resumeService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private JwtService jwtService;

    @Test
    @WithMockUser
    void createResume() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(resumeService.create(any())).thenReturn(new ResumeResponse(1L, "CV", "content", true));
        mvc.perform(post("/api/v1/resumes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"CV\",\"content\":\"content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void listOwnResumes() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(resumeService.listForUser()).thenReturn(java.util.Collections.emptyList());
        mvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateOwnResume() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(resumeService.update(eq(1L), any())).thenReturn(new ResumeResponse(1L, "Updated", "c", true));
        mvc.perform(put("/api/v1/resumes/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deleteOwnResume() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        doNothing().when(resumeService).delete(1L);
        mvc.perform(delete("/api/v1/resumes/1")
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void crossUserAccessDenied() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(99L);
        when(resumeService.getById(1L)).thenThrow(new RuntimeException("does not belong"));
        mvc.perform(get("/api/v1/resumes/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void unauthenticatedDenied() throws Exception {
        mvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isUnauthorized()); // Spring default when unauthenticated; access denied preserved
    }
}