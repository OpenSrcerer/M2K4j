package online.danielstefani.m2k4j.aws

import com.amazonaws.services.kinesis.model.PutRecordsRequest
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry
import com.amazonaws.services.kinesis.model.PutRecordsResult
import online.danielstefani.m2k4j.dto.Mqtt5Message
import online.danielstefani.m2k4j.mqtt.Mqtt5Consumer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class KinesisService(
    private val kinesisConfig: KinesisConfig,
    private val kinesisClient: KinesisClient,
    private val messageDlq: SynchronizedDeadLetterQueue = SynchronizedDeadLetterQueue()
) : Mqtt5Consumer<List<PutRecordsRequestEntry>> {
    companion object {
        private val logger = LoggerFactory.getLogger(KinesisService::class.java)

        // Arbitrary buffer size, could set as configurable on .env later
        private const val MAX_SAVE_BUFFER_SIZE = 3000
        internal const val MAX_KINESIS_PUT_REQUEST_LENGTH = 500 // Defined by AWS
    }

    class SynchronizedDeadLetterQueue {
        private val messageDlq = LinkedList<PutRecordsRequestEntry>()

        @Synchronized
        fun toList(): List<PutRecordsRequestEntry> {
            val dlq = messageDlq.toList()
            messageDlq.clear()
            return dlq
        }

        @Synchronized
        fun add(element: PutRecordsRequestEntry): Boolean { return messageDlq.add(element) }

        fun isEmpty(): Boolean { return messageDlq.isEmpty() }

        fun size(): Int { return messageDlq.size }
    }

    override fun pushMessages(messages: List<Mqtt5Message>): Mono<List<PutRecordsRequestEntry>> {
        return Flux.fromIterable(messages)
            .onBackpressureBuffer(MAX_SAVE_BUFFER_SIZE)
            .parallel()
            .map { it.toPutRecordsRequest(
                PartitioningStrategy.getComputedStrategy(kinesisConfig.kinesisPartitioningStrategy!!)
            ) }
            .sequential()
            .pushMessagesToKinesis() // Returns unsuccessful attempts, prepared for a retry
            .doOnNext { messageDlq.add(it) } // Put messages that didn't go through in the DLQ to try again later
            .collectList()
            .doOnError { logger.error("[client->kinesis] // " +
                    "Failed to send messages to Kinesis stream ${kinesisConfig.kinesisStreamName}") }
            .doOnSuccess {
                if (it.isEmpty()) {
                    logger.info(
                        "[client->kinesis] // Successfully sent all (${messages.size}) messages " +
                                "to Kinesis stream ${kinesisConfig.kinesisStreamName}")
                } else {
                    logger.info(
                        "[client->kinesis] // Sent ${messages.size - it.size}/${messages.size} " +
                                "(${(messages.size - it.size) / messages.size}%) of messages to Kinesis " +
                                "stream ${kinesisConfig.kinesisStreamName}; " +
                                "Failed messaged were put in the DLQ, and will be " +
                                "retried later (only once) then discarded if they fail again.")
                }
            }
    }

    @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    internal fun retryFailedMessagesOnce() {
        if (messageDlq.isEmpty())
            return

        val messageDlqSize = messageDlq.size()
        Flux.fromIterable(messageDlq.toList())
            .pushMessagesToKinesis()
            .doOnSubscribe {
                logger.info("[client->kinesis] //" +
                        "Retrying to send $messageDlqSize messages to ${kinesisConfig.kinesisStreamName}")
            }
            .collectList()
            .doOnSuccess {
                if (it.isEmpty()) {
                    logger.info("[client->kinesis] //" +
                            "Successfully sent $messageDlqSize/$messageDlqSize DLQ " +
                            "messages to ${kinesisConfig.kinesisStreamName}")
                } else {
                    logger.info("[client->kinesis] //" +
                            "Sent ${messageDlqSize - it.size}/$messageDlqSize DLQ " +
                            "messages to ${kinesisConfig.kinesisStreamName}." +
                            "${it.size} messages still failed, discarding.")
                }
            }
            .subscribe()
    }

    private fun Flux<PutRecordsRequestEntry>.pushMessagesToKinesis(): Flux<PutRecordsRequestEntry> {
        return this
            .window(MAX_KINESIS_PUT_REQUEST_LENGTH) // Flux<Flux<PutRecordsRequestEntry>> (window of 500)
            .flatMap { it.collect() }
            .flatMap { req -> kinesisClient.putRecords(req) }
            .extractFailedRecords()
    }

    private fun Flux<PutRecordsRequestEntry>.collect(): Mono<PutRecordsRequest> {
        return this.collectList().map { entries ->
            PutRecordsRequest()
                .withStreamName(kinesisConfig.kinesisStreamName)
                .withRecords(entries)
        }
    }

    private fun Flux<Pair<PutRecordsRequest, PutRecordsResult>>.extractFailedRecords(): Flux<PutRecordsRequestEntry> {
        return this.flatMap {
            Flux.fromIterable(
                it.second.records
                    .mapIndexed { i, entry ->
                        if (entry.errorCode?.isNotBlank() == true) it.first.records[i] else null }
                    .filterNotNull()
            )
        }
    }
}