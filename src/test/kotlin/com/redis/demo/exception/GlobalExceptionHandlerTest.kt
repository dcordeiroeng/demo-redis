package com.redis.demo.exception

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = [DummyController::class])
@Import(GlobalExceptionHandler::class)
class GlobalExceptionHandlerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `should return 503 and error message on RedisConnectionFailureException`() {
        mockMvc.perform(get("/fail").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error").value("Service temporarily unavailable. Please try again later."))
    }
}

@RestController
class DummyController {
    @GetMapping("/fail")
    fun fail(): String {
        throw RedisConnectionFailureException("Cannot connect")
    }
}