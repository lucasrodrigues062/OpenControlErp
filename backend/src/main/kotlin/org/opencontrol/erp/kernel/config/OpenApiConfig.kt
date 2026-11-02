package org.opencontrol.erp.kernel.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openControlOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("OpenControl ERP - Kernel API")
                    .description("Core enterprise extension engine, lifecycle manager, SPI registry and event outbox API")
                    .version("0.1.0")
                    .contact(
                        Contact()
                            .name("OpenControl Community")
                            .url("https://github.com/OpenControlErp")
                    )
                    .license(
                        License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                    )
            )
    }
}
