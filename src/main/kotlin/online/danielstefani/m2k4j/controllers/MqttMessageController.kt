package online.danielstefani.m2k4j.controllers

import online.danielstefani.m2k4j.aws.KinesisClient
import online.danielstefani.m2k4j.dto.Mqtt5Message
import online.danielstefani.m2k4j.mqtt.MqttClientProxyService
import online.danielstefani.m2k4j.mqtt.MqttConfig
import org.slf4j.LoggerFactory
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.messaging.Message
import org.springframework.messaging.support.ErrorMessage
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.util.retry.Retry
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * Entry point for counter messages.
 *
 * MqttConfig -> The broker emits messages and sends them to a channel defined here.
 *
 * MqttClientProxyService -> The client is built here, channel config is done here.
 * @see MqttConfig
 * @see MqttClientProxyService
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
class MqttMessageController(
    private val kinesisClient: KinesisClient,
    private val mqttClientProxyService: MqttClientProxyService,
    private val mqttCache: MqttCache = MqttCache()
) {
    private var toQueue = false

    class MqttCache {
        internal var messageCacheQueue = LinkedList<Mqtt5Message>()
        internal var messageCache = mutableListOf<Mqtt5Message>()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MqttMessageController::class.java)

        /* Arbitrary values */
        private val SAVE_TIMEOUT = Duration.ofMillis(2000)
        private const val MAX_SAVE_ATTEMPTS = 3L

        /* Just simple statistics */
        private var messagesReceivedStartClock = Instant.now()
        private var messagesReceivedEndClock = Instant.now()
        private var messagesReceived = 0
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    fun receive(message: Message<Mqtt5Message>) {
        messagesReceived += 1 // Statistics
        // Add to cache (messages are not pushed instantly, but in batches)
        if (toQueue)
            mqttCache.messageCacheQueue.add(message.payload)
        else
            mqttCache.messageCache.add(message.payload)
        logger.trace("[handler->receiver] Added message to cache: ${message.payload.payload}")
    }

    /**
     * Just a method to handle error messages that come from the client.
     */
    @ServiceActivator(inputChannel = "mqttErrorChannel")
    fun handleMqttError(errorMessage: ErrorMessage) {
        logger.error("[handler->error]: $errorMessage")
    }

    /**
     * CRON job basically: clear cache every X amount of time
     * This aggregates everything into one query and reduces load
     */
    @Scheduled(initialDelay = 20, fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    fun flush() {
        if (mqttCache.messageCache.isEmpty()) {
            logger.info("[handler->flusher] No messages received during this period, ignoring.")
            return
        }

        toQueue = true // Redirect messages to queue
        val localCache = mqttCache.messageCache // Yoink the cache to this scope
        mqttCache.messageCache = mutableListOf() // Replace the other cache with a new one (prevents copying)
        toQueue = false // Redirect messages back to list cache
        mqttCache.messageCache.addAll(mqttCache.messageCacheQueue) // Add all messages that came in during handover to actual cache
        mqttCache.messageCacheQueue.clear() // Clear queue

        // Send the messages using local scope cache
        kinesisClient.pushMessagesToKinesis(localCache)
            .retryWhen(Retry.backoff(MAX_SAVE_ATTEMPTS, SAVE_TIMEOUT))
            .log("[handler->flusher]", Level.FINE)
            .subscribe()
    }

    @Scheduled(initialDelay = 0, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun restartStatistics() {
        messagesReceivedEndClock = Instant.now()
        logger.info("[handler->statistics] Messages { ($messagesReceivedStartClock - $messagesReceivedEndClock) " +
                "[Received $messagesReceived] }")
        messagesReceivedStartClock = Instant.now()
        messagesReceived = 0
        mqttClientProxyService.rebuildMqttClient()
    }
}