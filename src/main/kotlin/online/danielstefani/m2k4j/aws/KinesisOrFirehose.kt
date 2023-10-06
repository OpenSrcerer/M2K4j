package online.danielstefani.m2k4j.aws

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose

interface KinesisOrFirehose : AmazonKinesis, AmazonKinesisFirehose