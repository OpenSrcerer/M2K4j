package com.amazonaws.services.kinesis

import com.amazonaws.client.M2KKinesisAsyncClientParams
import com.amazonaws.services.kinesis.model.PutRecordsRequest
import com.amazonaws.services.kinesis.model.PutRecordsResult
import com.amazonaws.services.kinesisfirehose.model.*
import online.danielstefani.m2k4j.aws.KinesisOrFirehose

class M2KKinesis(
    builder: AmazonKinesisClientBuilder
) : AmazonKinesisAsyncClient(M2KKinesisAsyncClientParams(builder)), KinesisOrFirehose {
    override fun putRecords(putRecordsRequest: PutRecordsRequest?): PutRecordsResult {
        return super.putRecords(putRecordsRequest)
    }

    override fun putRecord(putRecordRequest: PutRecordRequest?): PutRecordResult {
        TODO("Not yet implemented")
    }

    override fun createDeliveryStream(createDeliveryStreamRequest: CreateDeliveryStreamRequest?): CreateDeliveryStreamResult {
        TODO("Not yet implemented")
    }

    override fun deleteDeliveryStream(deleteDeliveryStreamRequest: DeleteDeliveryStreamRequest?): DeleteDeliveryStreamResult {
        TODO("Not yet implemented")
    }

    override fun describeDeliveryStream(describeDeliveryStreamRequest: DescribeDeliveryStreamRequest?): DescribeDeliveryStreamResult {
        TODO("Not yet implemented")
    }

    override fun listDeliveryStreams(listDeliveryStreamsRequest: ListDeliveryStreamsRequest?): ListDeliveryStreamsResult {
        TODO("Not yet implemented")
    }

    override fun listTagsForDeliveryStream(listTagsForDeliveryStreamRequest: ListTagsForDeliveryStreamRequest?): ListTagsForDeliveryStreamResult {
        TODO("Not yet implemented")
    }

    override fun putRecordBatch(putRecordBatchRequest: PutRecordBatchRequest?): PutRecordBatchResult {
        TODO("Not yet implemented")
    }

    override fun startDeliveryStreamEncryption(startDeliveryStreamEncryptionRequest: StartDeliveryStreamEncryptionRequest?): StartDeliveryStreamEncryptionResult {
        TODO("Not yet implemented")
    }

    override fun stopDeliveryStreamEncryption(stopDeliveryStreamEncryptionRequest: StopDeliveryStreamEncryptionRequest?): StopDeliveryStreamEncryptionResult {
        TODO("Not yet implemented")
    }

    override fun tagDeliveryStream(tagDeliveryStreamRequest: TagDeliveryStreamRequest?): TagDeliveryStreamResult {
        TODO("Not yet implemented")
    }

    override fun untagDeliveryStream(untagDeliveryStreamRequest: UntagDeliveryStreamRequest?): UntagDeliveryStreamResult {
        TODO("Not yet implemented")
    }

    override fun updateDestination(updateDestinationRequest: UpdateDestinationRequest?): UpdateDestinationResult {
        TODO("Not yet implemented")
    }
}