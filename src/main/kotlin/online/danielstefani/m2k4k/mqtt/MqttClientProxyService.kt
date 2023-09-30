package online.danielstefani.m2k4k.mqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.Mqtt5RxClient
import com.hivemq.client.mqtt.mqtt5.advanced.Mqtt5ClientAdvancedConfig
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription
import io.reactivex.Completable
import io.reactivex.Flowable
import online.danielstefani.m2k4k.dto.Mqtt5Message
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.integration.support.MutableMessageBuilder
import org.springframework.messaging.MessageChannel
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class MqttClientProxyService(
    @Qualifier("inputChannel") private val inputChannel: MessageChannel,
    @Qualifier("errorChannel") private val errorChannel: MessageChannel,
    private val mqttConfig: MqttConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MqttConfig::class.java)
    }

    // Singleton
    private var mqttClient: Mqtt5RxClient? = null

    /**
     * Rebuilds (replaces) current singleton client.
     */
    fun rebuildMqttClient(): Mqtt5RxClient {
        shutdownClient().blockingAwait() // Kill current client (null checks inside)

        // ---- Build Client ----
        val client = Mqtt5Client.builder()
            .identifier(
                "${mqttConfig.clientId}-${UUID.randomUUID()}".apply {
                    logger.info("[client->mqtt->reaper] // Building new MQTT client: $this")
                }
            )
            .serverHost(mqttConfig.host!!)
            .serverPort(mqttConfig.port!!)
            .also {
                if (mqttConfig.username?.isNotBlank() == true &&
                    mqttConfig.password?.isNotBlank() == true) {
                    it.simpleAuth(
                        Mqtt5SimpleAuth.builder()
                            .username(mqttConfig.username!!)
                            .password(mqttConfig.password!!.toByteArray(Charsets.UTF_8))
                            .build()
                    )
                }
            }
            .advancedConfig(
                Mqtt5ClientAdvancedConfig.builder()
                    .allowServerReAuth(true)
                    .build()
            )
            .buildRx()

        return client.apply {
            mqttClient = this
            mqttClient!!
                .connectScenario()
                .applySubscription()
        }
    }

    /**
     * Define connection rules for MQTT Client building above.
     */
    private fun Mqtt5RxClient.connectScenario(): Mqtt5RxClient {
        this.connectWith()
            .noSessionExpiry()
            .cleanStart(true)
            .applyConnect()
            .doOnSuccess { logger.info("[client->mqtt] // " +
                    "Connected to ${mqttConfig.host}:${mqttConfig.port}, ${it.reasonCode}") }
            .doOnError { logger.error("[client->mqtt] // " +
                    "Connection failed to ${mqttConfig.host}:${mqttConfig.port}, ${it.message}") }
            .doOnSubscribe { logger.info("[client->mqtt] // " +
                    "Connecting to... ${mqttConfig.host}:${mqttConfig.port}")
            }
            .retryWhen {
                Flowable.timer(30, TimeUnit.SECONDS)
                    .doOnNext { logger.info("[client->mqtt] // " +
                            "Retrying connection to ${mqttConfig.host}:${mqttConfig.port}...") }
            }
            .ignoreElement()
            .blockingAwait()

        return mqttClient!!
    }

    /**
     * Subscribe to the given topics.
     */
    private fun Mqtt5RxClient.applySubscription() {
        this.subscribePublishesWith()
            .addSubscriptions(
                mqttConfig.getSubscriptions()
                    .map {
                        Mqtt5Subscription.builder()
                            .topicFilter(it)
                            .qos(MqttQos.AT_LEAST_ONCE)
                            .build()
                    }
            )
            .applySubscribe()
            .doOnError { errorChannel.send(MutableMessageBuilder.withPayload(it).build()) }
            .doOnNext {
                inputChannel.send(MutableMessageBuilder.withPayload(Mqtt5Message(it)).build())
            }
            .doOnSubscribe {
                logger.info("[client->mqtt] // " + "Subscribing to topics [" +
                        "${mqttConfig.getSubscriptions().joinToString(", ") { "'${it}'" }}]")
            }
            .subscribe()
    }

    private fun shutdownClient(): Completable {
        if (mqttClient == null || !mqttClient!!.state.isConnected)
            return Completable.complete()
                .doOnComplete { logger.info("[client->mqtt->reaper] // " +
                        "Old MQTT client was null or disconnected, ignoring it") }

        return mqttClient!!.disconnect()
            .doOnComplete { logger.info("[client->mqtt->reaper] // " +
                    "Reaped old MQTT client ${mqttClient!!.config.clientIdentifier.get()}") }
    }
}