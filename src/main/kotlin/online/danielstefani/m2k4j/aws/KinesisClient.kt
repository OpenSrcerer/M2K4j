package online.danielstefani.m2k4j.aws

import online.danielstefani.m2k4j.mqtt.Mqtt5Consumer
import online.danielstefani.m2k4j.dto.Mqtt5Message
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import java.util.*
import java.util.concurrent.TimeUnit

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class KinesisClient(
    private val kinesisConfig: KinesisConfig
) : Mqtt5Consumer<List<PutRecordsRequestEntry>> {
    private class SynchronizedDeadLetterQueue {
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

    companion object {
        private val messageDlq = SynchronizedDeadLetterQueue()
        private val logger = LoggerFactory.getLogger(KinesisClient::class.java)

        // Arbitrary buffer size, could set as configurable on .env later
        private const val MAX_SAVE_BUFFER_SIZE = 3000
        private const val MAX_KINESIS_PUT_REQUEST_LENGTH = 500 // Defined by AWS
    }

    private val kinesisClient: KinesisAsyncClient =
        KinesisAsyncClient.builder() // Needs credentials file in .aws!
            .also {
                if (kinesisConfig.awsAccessKey?.isNotBlank() == true &&
                    kinesisConfig.awsSecretAccessKey?.isNotBlank() == true) {
                    it.credentialsProvider {
                        AwsBasicCredentials.create(kinesisConfig.awsAccessKey, kinesisConfig.awsSecretAccessKey)
                    }
                }
            }
            .region(Region.of(kinesisConfig.awsRegion))
            .build()

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
                    "Failed to send messages to Kinesis stream ${kinesisConfig.kinesisStreamArn}") }
            .doOnSuccess {
                if (it.isEmpty()) {
                    logger.info(
                        "[client->kinesis] // Successfully sent all (${messages.size}) messages " +
                                "to Kinesis stream ${kinesisConfig.kinesisStreamArn}")
                } else {
                    logger.info(
                        "[client->kinesis] // Sent ${messages.size - it.size}/${messages.size}" +
                                "(${messages.size}) of messages to Kinesis " +
                                "stream ${kinesisConfig.kinesisStreamArn}; " +
                                "Failed messaged were put in the DLQ, and will be " +
                                "retried later (only once) then discarded if they fail again.")
                }
            }
    }

    @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    private fun retryFailedMessagesOnce() {
        if (messageDlq.isEmpty())
            return

        val messageDlqSize = messageDlq.size()
        Flux.fromIterable(messageDlq.toList())
            .pushMessagesToKinesis()
            .doOnSubscribe {
                logger.info("[client->kinesis] //" +
                        "Retrying to send $messageDlqSize messages to ${kinesisConfig.kinesisStreamArn}")
            }
            .collectList()
            .doOnSuccess {
                if (it.isEmpty()) {
                    logger.info("[client->kinesis] //" +
                            "Successfully sent $messageDlqSize/$messageDlqSize DLQ " +
                            "messages to ${kinesisConfig.kinesisStreamArn}")
                } else {
                    logger.info("[client->kinesis] //" +
                            "Sent ${messageDlqSize - it.size}/$messageDlqSize DLQ " +
                            "messages to ${kinesisConfig.kinesisStreamArn}." +
                            "${it.size} messages still failed, discarding.")
                }
            }
            .subscribe()
    }

    private fun Flux<PutRecordsRequestEntry>.pushMessagesToKinesis(): Flux<PutRecordsRequestEntry> {
        return this
            .window(MAX_KINESIS_PUT_REQUEST_LENGTH) // Flux<Flux<PutRecordsRequestEntry>> (window of 500)
            .flatMap { it.collect() }
            .flatMap { req -> kinesisClient.putRecords(req).toMono().map { res -> Pair(req, res) } }
            .extractFailedRecords()
    }

    private fun Flux<PutRecordsRequestEntry>.collect(): Mono<PutRecordsRequest> {
        return this.collectList().map { entries ->
            PutRecordsRequest.builder()
                .streamARN(kinesisConfig.kinesisStreamArn)
                .records(entries)
                .build()
        }
    }

    private fun Flux<Pair<PutRecordsRequest, PutRecordsResponse>>.extractFailedRecords(): Flux<PutRecordsRequestEntry> {
        return this.flatMap {
            Flux.fromIterable(
                it.second.records()
                    .mapIndexed { i, entry -> if (entry.errorCode().isNotBlank()) it.first.records()[i] else null }
                    .filterNotNull()
            )
        }
    }
}