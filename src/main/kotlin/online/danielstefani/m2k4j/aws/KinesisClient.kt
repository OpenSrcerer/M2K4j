package online.danielstefani.m2k4j.aws

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

@Service
class KinesisClient(
    private val kinesisConfig: KinesisConfig
) {
    val kinesisClient: KinesisAsyncClient =
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

    fun putRecords(req: PutRecordsRequest): Mono<Pair<PutRecordsRequest, PutRecordsResponse>> {
        return kinesisClient
            .putRecords(req)
            .toMono()
            .map { res -> Pair(req, res) }
    }
}