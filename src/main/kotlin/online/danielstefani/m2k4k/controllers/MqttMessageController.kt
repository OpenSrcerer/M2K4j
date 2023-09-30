package online.danielstefani.m2k4k.controllers

import online.danielstefani.m2k4k.aws.KinesisClient
import online.danielstefani.m2k4k.dto.Mqtt5Message
import online.danielstefani.m2k4k.mqtt.MqttClientProxyService
import online.danielstefani.m2k4k.mqtt.MqttConfig
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
@Component
class MqttMessageController(
    private val kinesisClient: KinesisClient,
    private val mqttClientProxyService: MqttClientProxyService,
) {
    private var toQueue = false
    private val messageCacheQueue = LinkedList<Mqtt5Message>()
    private var messageCache = mutableListOf<Mqtt5Message>()

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
            messageCacheQueue.add(message.payload)
        else
            messageCache.add(message.payload)
        logger.trace("[stream->receiver] Added message to cache: ${message.payload.payload}")
    }

    /**
     * Just a method to handle error messages that come from the client.
     */
    @ServiceActivator(inputChannel = "mqttErrorChannel")
    fun handleMqttError(errorMessage: ErrorMessage) {
        logger.error("[stream->error]: $errorMessage")
    }

    /**
     * CRON job basically: clear cache every X amount of time
     * This aggregates everything into one query and reduces load
     */
    @Scheduled(initialDelay = 20, fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    fun flush() {
        if (messageCache.isEmpty()) {
            logger.info("[stream->flusher] No messages received during this period, ignoring.")
            return
        }

        toQueue = true // Redirect messages to queue
        val localCache = messageCache // Yoink the cache to this scope
        messageCache = mutableListOf() // Replace the other cache with a new one (prevents copying)
        toQueue = false // Redirect messages back to list cache
        messageCache.addAll(messageCacheQueue) // Add all messages that came in during handover to actual cache
        messageCacheQueue.clear() // Clear queue

        // Send the messages using local scope cache
        kinesisClient.pushMessagesToKinesis(localCache)
            .retryWhen(Retry.backoff(MAX_SAVE_ATTEMPTS, SAVE_TIMEOUT))
            .log("[stream->flusher]", Level.FINE)
            .subscribe()
    }

    @Scheduled(initialDelay = 0, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun restartStatistics() {
        messagesReceivedEndClock = Instant.now()
        logger.info("[stream->statistics] Messages { ($messagesReceivedStartClock - $messagesReceivedEndClock) " +
                "[Received $messagesReceived] }")
        messagesReceivedStartClock = Instant.now()
        messagesReceived = 0
        mqttClientProxyService.rebuildMqttClient()
    }
}