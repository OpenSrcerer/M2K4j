package online.danielstefani.m2k4k.mqtt

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.messaging.MessageChannel
import java.lang.IllegalArgumentException

@Configuration
@EnableIntegration
class MqttConfig {

    @Value("\${mqtt.username}")
    var username: String? = null

    @Value("\${mqtt.password}")
    var password: String? = null

    @Value("\${mqtt.host}")
    var host: String? = null

    @Value("\${mqtt.port}")
    var port: Int? = null

    @Value("\${mqtt.clientid:}")
    var clientId: String? = null

    // Expected to be comma-delimited
    @Value("\${mqtt.subscriptions}")
    var subscriptions: String? = null

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Qualifier("inputChannel")
    fun mqttInputChannel(): MessageChannel {
        return DirectChannel()
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Qualifier("errorChannel")
    fun mqttErrorChannel(): MessageChannel {
        return DirectChannel()
    }

    internal fun getSubscriptions(): List<String> {
        if (subscriptions.isNullOrEmpty())
            throw IllegalArgumentException("No subscriptions provided!")

        return subscriptions!!.split(",")
    }
}
