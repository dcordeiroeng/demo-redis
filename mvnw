@RestController
class CustomPrometheusEndpoint(
    // Injete o bean que você criou no passo 3
    private val prometheusMeterRegistry: PrometheusMeterRegistry 
) {
    @GetMapping("/actuator/prometheus", produces = [TEXT_PLAIN_VALUE]) // Especificar o produces é uma boa prática
    fun scrape(): String {
        return prometheusMeterRegistry.scrape()
    }

    companion object {
        const val TEXT_PLAIN_VALUE = "text/plain; version=0.0.4; charset=utf-8"
    }
}


@Configuration
class MetricsConfig {
    @Bean
    fun prometheusMeterRegistry(): PrometheusMeterRegistry {
        // ATENÇÃO: Usará configurações padrão. Propriedades como 
        // management.metrics.export.prometheus.* NÃO serão aplicadas automaticamente.
        return PrometheusMeterRegistry(io.micrometer.prometheus.PrometheusConfig.DEFAULT)

        // Se precisar aplicar properties, teria que injetar PrometheusProperties
        // e configurar o registry manualmente aqui, replicando a lógica da auto-config.
    }
}
