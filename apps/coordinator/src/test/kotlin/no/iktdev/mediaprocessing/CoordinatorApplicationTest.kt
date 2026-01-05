package no.iktdev.mediaprocessing

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.iktdev.mediaprocessing.coordinator.CoordinatorApplication
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [CoordinatorApplication::class],
    properties = ["spring.flyway.enabled=true"]
)
class CoordinatorApplicationTest {

    @Test
    fun `ExposedInitializer should connect to database`() {
        mockkObject(Database)

        every {
            Database.connect(
                any<DataSource>(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk()


        val context = SpringApplicationBuilder(CoordinatorApplication::class.java)
            .properties("spring.main.web-application-type=none")
            .run()

        verify(exactly = 1) { Database.connect(any<DataSource>(), any(), any(), any(), any()) }
    }

}