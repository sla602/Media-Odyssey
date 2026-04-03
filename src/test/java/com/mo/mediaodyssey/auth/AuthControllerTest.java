package com.mo.mediaodyssey.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    // --- LOGIN ---
    @Test
    void testLogin_withInvalidCredentials_returnsUnauthorized() throws Exception {
        var payload = objectMapper.writeValueAsString(new UserDto("fake@email.com", "wrongpass"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_USER_NOT_FOUND"));
    }

    @Test
    void testLogin_withMissingFields_returnsBadRequest() throws Exception {
        var payload = objectMapper.writeValueAsString(new UserDto("", ""));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_BAD_REQUEST"));
    }

    // --- REGISTER ---
    @Test
    void testRegister_withValidData_returnsSuccess() throws Exception {
        String email = "testuser" + System.currentTimeMillis() + "@example.com";
        var payload = objectMapper.writeValueAsString(new UserDto(email, "password123"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_REGISTER_SUCCESS"));
    }

    @Test
    void testRegister_withExistingEmail_returnsUnauthorized() throws Exception {
        String email = "existinguser" + System.currentTimeMillis() + "@example.com";
        var payload = objectMapper.writeValueAsString(new UserDto(email, "password123"));
        // Register once
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isOk());
        // Register again with same email
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void testRegister_withMissingFields_returnsBadRequest() throws Exception {
        var payload = objectMapper.writeValueAsString(new UserDto("", ""));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_BAD_REQUEST"));
    }

    // --- VERIFY ---
    @Test
    void testVerify_withInvalidToken_returnsBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/verify?token=invalidtoken"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_INVALID_VERIFICATION_TOKEN"));
    }

    // --- RESEND ---
    @Test
    void testResend_withNonexistentEmail_returnsUnauthorized() throws Exception {
        var payload = objectMapper
                .writeValueAsString(new ResendVerifyTokenDto("notfound" + System.currentTimeMillis() + "@example.com"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_USER_NOT_FOUND"));
    }

    @Test
    void testResend_withMissingEmail_returnsBadRequest() throws Exception {
        var payload = objectMapper.writeValueAsString(new ResendVerifyTokenDto(""));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("AUTH_BAD_REQUEST"));
    }

    // --- Helper DTOs for requests ---
    static class UserDto {
        public String email;
        public String password;

        public UserDto(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    static class ResendVerifyTokenDto {
        public String email;

        public ResendVerifyTokenDto(String email) {
            this.email = email;
        }
    }
}
