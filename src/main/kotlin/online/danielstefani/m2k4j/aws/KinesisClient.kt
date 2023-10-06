package online.danielstefani.m2k4j.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.model.PutRecordsRequest
import com.amazonaws.services.kinesis.model.PutRecordsResult
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Service
class KinesisClient(
    private val kinesisConfig: KinesisConfig
) {
    val kinesisClient: AmazonKinesis =
        AmazonKinesisAsyncClient.builder() // Needs credentials file in .aws!
            .also {
                if (kinesisConfig.awsAccessKey?.isNotBlank() == true &&
                    kinesisConfig.awsSecretAccessKey?.isNotBlank() == true) {

                    it.withCredentials(object : AWSCredentialsProvider {
                        val credentials = BasicAWSCredentials(
                            kinesisConfig.awsAccessKey,
                            kinesisConfig.awsSecretAccessKey
                        )

                        override fun getCredentials(): AWSCredentials {
                            return credentials
                        }

                        override fun refresh() {}
                    })
                }
            }
            .withRegion(Regions.fromName(kinesisConfig.awsRegion!!))
            .build()

    fun putRecords(req: PutRecordsRequest): Mono<Pair<PutRecordsRequest, PutRecordsResult>> {
        return kinesisClient
            .putRecords(req)
            .toMono()
            .map { res -> Pair(req, res) }
    }
}