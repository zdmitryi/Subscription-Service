package com.example.subscription.controller

import com.example.subscription.models.Subscription
import com.example.subscription.models.SubscriptionStatus
import com.example.subscription.utility.SubscriptionService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(SubscriptionController::class)
class SubscriptionControllerTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var subscriptionService: SubscriptionService

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    private val testSubscription = Subscription(
        id = 1L,
        userId = 1L,
        serviceName = "Netflix",
        startDate = java.time.LocalDate.now(),
        endDate = java.time.LocalDate.now().plusMonths(3),
        price = java.math.BigDecimal("47.97"),
        status = SubscriptionStatus.ACTIVE
    )

    @Test
    @WithMockUser
    fun `getSubscriptionById should return subscription`() {
        org.mockito.kotlin.whenever(subscriptionService.getSubscriptionById(1L)).thenReturn(testSubscription)

        mockMvc.perform(get("/subscription/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    @WithMockUser
    fun `getAllSubscriptions should return list`() {
        org.mockito.kotlin.whenever(subscriptionService.getAllSubscriptions(org.mockito.kotlin.any()))
            .thenReturn(listOf(testSubscription))

        mockMvc.perform(get("/subscription"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }


}