package com.shepherdmoney.interviewproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;


@SpringBootTest
@AutoConfigureMockMvc
class InterviewProjectApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void testCreateUser() throws Exception {
        CreateUserPayload payload = new CreateUserPayload();
        payload.setName("wenhao");
        payload.setEmail("wenhao@example.com");

        mockMvc.perform(put("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void testDeleteUser() throws Exception {
        // Assume a user with ID 1 exists in the database
        int userId = 1;

        mockMvc.perform(delete("/user")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteNonExistingUser() throws Exception {
        // Assume no user with ID 999 exists in the database
        int userId = 999;

        mockMvc.perform(delete("/user")
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCreditCardToUserSuccess() throws Exception {
        AddCreditCardToUserPayload payload = new AddCreditCardToUserPayload();
        payload.setUserId(1);
        payload.setCardIssuanceBank("Example Bank");
        payload.setCardNumber("1234567890123456");

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void addCreditCardToUserInvalidUserId() throws Exception {
        AddCreditCardToUserPayload payload = new AddCreditCardToUserPayload();
        payload.setUserId(-1);
        payload.setCardIssuanceBank("Example Bank");
        payload.setCardNumber("1234567890123456");

        mockMvc.perform(post("/credit-card")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCardOfUser_validUserId() throws Exception {
        int userId = 1; // Replace with a valid user ID in your database

        mockMvc.perform(get("/credit-card:all")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllCardOfUser_invalidUserId() throws Exception {
        int userId = -1; // Nonexistent user ID

        mockMvc.perform(get("/credit-card:all")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserIdForCreditCard_existingCreditCard_returnsUserId() throws Exception {
        // When requesting the user ID for the credit card number
        mockMvc.perform(get("/credit-card:user-id")
                        .param("creditCardNumber", "1234567890123456")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void getUserIdForCreditCard_nonExistingCreditCard_returnsBadRequest() throws Exception {
        // When requesting the user ID for a non-existing credit card number
        mockMvc.perform(get("/credit-card:user-id")
                        .param("creditCardNumber", "1111222233334444")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBalances_validTransaction() throws Exception {
        UpdateBalancePayload payload = new UpdateBalancePayload();
        payload.setCreditCardNumber("1234567890123456");
        payload.setTransactionTime(Instant.parse("2023-04-12T10:00:00Z"));
        payload.setTransactionAmount(10);
        UpdateBalancePayload[] payloads = {payload};

        mockMvc.perform(post("/credit-card:update-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloads)))
                .andExpect(status().isOk());
    }

    @Test
    void updateBalances_invalidTransaction() throws Exception {
        UpdateBalancePayload payload = new UpdateBalancePayload();
        payload.setCreditCardNumber("98765");
        payload.setTransactionTime(Instant.parse("2023-04-12T10:00:00Z"));
        payload.setTransactionAmount(10);
        UpdateBalancePayload[] payloads = {payload};

        mockMvc.perform(post("/credit-card:update-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }
}
