package online.danielstefani.m2k4j.aws

import io.mockk.*
import online.danielstefani.m2k4j.MessageUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse
import software.amazon.awssdk.services.kinesis.model.PutRecordsResultEntry

@SpringBootTest
class KinesisServiceTest {

    // ---- Injections ----
    @Autowired
    private lateinit var kinesisConfig: KinesisConfig

    // ---- Spies ----
    private lateinit var kinesisService: KinesisService

    // ---- Mocks ----
    private val kinesisClient: KinesisClient = mockk()
    private val kinesisDlq: KinesisService.SynchronizedDeadLetterQueue =
        spyk(KinesisService.SynchronizedDeadLetterQueue())

    @BeforeEach
    fun initializeTest() {
        kinesisService = spyk(KinesisService(kinesisConfig, kinesisClient, kinesisDlq))
    }

    @Test
    fun whenPushMessagesToKinesis_thenExpectMaxBatchToBe500() {
        val messagesToSend = 1985 // WE NEED TO GO BACK!
        val messagesSent = mutableListOf<PutRecordsRequest>()

        every { kinesisClient.putRecords(any()) } answers {
            messagesSent.add(this.args[0] as PutRecordsRequest)
            Mono.just(
                Pair(PutRecordsRequest.builder().build(), PutRecordsResponse.builder().build()))
        }

        pushNMessages(messagesToSend).block()

        verify(exactly = getExpectedKinesisCalls(messagesToSend)) {
            kinesisClient.putRecords(any())
        }
        assertEquals( // Makes sure packages are max 500 msgs
            true,
            messagesSent.all { it.records().size <= 500 }
        )
    }

    @Test
    fun whenPushMessageToKinesisFails_thenShouldPushToDlq() {
        val messagesToSend = 1985 // WE NEED TO GO BACK!
        everyPutRecordsReturnError()

        pushNMessages(messagesToSend).block()

        verify(exactly = messagesToSend) { kinesisDlq.add(any()) }
        assertEquals(kinesisDlq.size(), messagesToSend)
    }

    @Test
    fun whenEveryMinuteAndDlqHasItems_thenShouldRetryOnce() {
        val messagesToSend = 1985 // WE NEED TO GO BACK!
        everyPutRecordsReturnError()

        pushNMessages(messagesToSend).block()
        kinesisService.retryFailedMessagesOnce() // Retry failed messages

        // Multiplied times two cause once when it
        // first failed, and once for the retry
        verify(exactly = getExpectedKinesisCalls(messagesToSend) * 2) {
            kinesisClient.putRecords(any())
        }
        assertEquals(kinesisDlq.size(), 0)
    }

    private fun everyPutRecordsReturnError() {
        every { kinesisClient.putRecords(any()) } answers {
            val req = this.args[0] as PutRecordsRequest
            val res = req.records().map {
                PutRecordsResultEntry.builder().errorCode("400").errorMessage("test").build()
            }
            Mono.just(Pair(req, PutRecordsResponse.builder().records(res).build()))
        }
    }

    private fun pushNMessages(messagesToSend: Int): Mono<List<PutRecordsRequestEntry>> {
        return Flux.just(MessageUtils.genDefaultMessage())
            .repeat(messagesToSend.toLong() - 1)
            .map { it.next().payload }
            .collectList()
            .flatMap { kinesisService.pushMessages(it) }
    }

    private fun getExpectedKinesisCalls(messagesToSend: Int): Int {
        return messagesToSend / KinesisService.MAX_KINESIS_PUT_REQUEST_LENGTH +
                if (messagesToSend % KinesisService.MAX_KINESIS_PUT_REQUEST_LENGTH > 0) 1 else 0
    }
}