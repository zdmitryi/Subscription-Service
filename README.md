Сервис для хранения информации о подписках на некоторые сервисы. 

Стек: Kotlin, Spring Boot, PostgreSQL, Spring Data JPA, Flyway, OpenAPI + Swagger, JUnit + Mockito + WebMvcTest, Docker-Compose, Scheduler, Actuator + Micrometer.

Реализованы: Основное API для управления подписками, работа со статусами подписок через scheduler и с помощью API,  валидация данных, история изменения статусов подписок, сбор метрик и статистики, экспорт списка подписок в CSV-формат, покрытие тестами.

Что доработать: сделать большее покрытие unit-тестами, а также добавить интеграционные тесты с TestContainers.
