package com.pbsynth.referencedata.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      // Keep scheduled account sync off unless a test opts in with a mock source client.
      "refdata.account.sync.enabled=false"
    })
@AutoConfigureMockMvc
class SecurityControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void getById_returnsSeed() throws Exception {
    mockMvc
        .perform(get("/api/v1/securities/SEC-001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ric").value("VOD.L"));
  }

  @Test
  void getById_missing_returns404() throws Exception {
    mockMvc.perform(get("/api/v1/securities/UNKNOWN")).andExpect(status().isNotFound());
  }
}
