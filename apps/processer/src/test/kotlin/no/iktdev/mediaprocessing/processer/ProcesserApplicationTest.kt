package no.iktdev.mediaprocessing.processer

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    classes = [ProcesserApplication::class],
    properties = ["spring.flyway.enabled=true"]
)
class ProcesserApplicationTest {

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


        val context = SpringApplicationBuilder(ProcesserApplication::class.java)
            .properties("spring.main.web-application-type=none")
            .run()

        verify(exactly = 1) { Database.connect(any<DataSource>(), any(), any(), any(), any()) }
    }

}
