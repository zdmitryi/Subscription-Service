package com.example.subscription

import com.example.subscription.mappers.SubscriptionMapper
import com.example.subscription.models.Subscription
import com.example.subscription.models.SubscriptionStatus
import com.example.subscription.models.User
import com.example.subscription.models.ServicePriceEntity
import com.example.subscription.models.SubscriptionEntity
import com.example.subscription.repository.ServicePriceRepository
import com.example.subscription.repository.SubscriptionHistoryRepository
import com.example.subscription.repository.SubscriptionRepository
import com.example.subscription.repository.UserRepository
import com.example.subscription.utility.SubscriptionService
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTests {

    @Mock
    private lateinit var meterRegistry: MeterRegistry

    @Mock
    private lateinit var subscriptionRepository: SubscriptionRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var subscriptionHistoryRepository: SubscriptionHistoryRepository

    @Mock
    private lateinit var servicePriceRepository: ServicePriceRepository

    @Spy
    private var subscriptionMapper = SubscriptionMapper()

    @InjectMocks
    private lateinit var subscriptionService: SubscriptionService

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1L,
            username = "testuser",
            email = "test@example.com",
            password = "password123"
        )

        val mockCounter = org.mockito.kotlin.mock<Counter>()
        whenever(meterRegistry.counter(any(), any<String>())).thenReturn(mockCounter)
        whenever(meterRegistry.counter(any())).thenReturn(mockCounter)
    }

    @Test
    fun `getSubscriptionById should return subscription when exists`() {
        val entity = SubscriptionEntity().apply {
            id = 1L
            userId = 1L
            serviceName = "Service1"
            startDate = LocalDate.now()
            endDate = LocalDate.now().plusMonths(3)
            price = BigDecimal("200")
            status = SubscriptionStatus.ACTIVE
        }
        whenever(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity))

        val result = subscriptionService.getSubscriptionById(1L)

        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Service1", result.serviceName)
    }

    @Test
    fun `createSubscription should throw exception when price is not multiple`() {
        val priceEntity = ServicePriceEntity().apply {
            serviceName = "Service1"
            monthlyPrice = BigDecimal("200")
        }

        val subscriptionToCreate = Subscription(
            userId = 1L,
            serviceName = "Service1",
            price = BigDecimal("700"),
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusMonths(3),
            status = SubscriptionStatus.ACTIVE
        )

        whenever(userRepository.existsById(1L)).thenReturn(true)
        whenever(servicePriceRepository.findByServiceName("Service1")).thenReturn(priceEntity)

        val exception = assertThrows<IllegalArgumentException> {
            subscriptionService.createSubscription(subscriptionToCreate, testUser)
        }
        assertTrue(exception.message?.contains("multiple") == true)
    }

    @Test
    fun `cancelSubscription should change status to CANCELLED`() {
        val entity = SubscriptionEntity().apply {
            id = 1L
            userId = 1L
            serviceName = "Service1"
            startDate = LocalDate.now().minusMonths(1)
            endDate = LocalDate.now().plusMonths(2)
            price = BigDecimal("200")
            status = SubscriptionStatus.ACTIVE
        }

        whenever(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(subscriptionRepository.save(any<SubscriptionEntity>())).thenAnswer { invocation ->
            invocation.arguments[0] as SubscriptionEntity
        }

        val result = subscriptionService.cancelSubscription(1L, testUser)

        assertEquals(SubscriptionStatus.CANCELLED, result.status)
    }

    @Test
    fun `suspendSubscription should change status to SUSPENDED`() {
        val entity = SubscriptionEntity().apply {
            id = 1L
            userId = 1L
            serviceName = "Service1"
            startDate = LocalDate.now().minusMonths(1)
            endDate = LocalDate.now().plusMonths(2)
            price = BigDecimal("200")
            status = SubscriptionStatus.ACTIVE
        }

        whenever(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(subscriptionRepository.save(any<SubscriptionEntity>())).thenAnswer { invocation ->
            invocation.arguments[0] as SubscriptionEntity
        }

        val result = subscriptionService.suspendSubscription(1L, testUser)

        assertEquals(SubscriptionStatus.SUSPENDED, result.status)
    }

    @Test
    fun `activateSubscription should change status to ACTIVE`() {
        val entity = SubscriptionEntity().apply {
            id = 1L
            userId = 1L
            serviceName = "Service1"
            startDate = LocalDate.now().minusMonths(1)
            endDate = LocalDate.now().plusMonths(2)
            price = BigDecimal("200")
            status = SubscriptionStatus.SUSPENDED
        }

        whenever(subscriptionRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(subscriptionRepository.save(any<SubscriptionEntity>())).thenAnswer { invocation ->
            invocation.arguments[0] as SubscriptionEntity
        }

        val result = subscriptionService.activateSubscription(1L, testUser)

        assertEquals(SubscriptionStatus.ACTIVE, result.status)
    }
}