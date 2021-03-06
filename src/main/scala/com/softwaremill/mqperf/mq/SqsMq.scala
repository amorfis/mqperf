package com.softwaremill.mqperf.mq

import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient
import com.amazonaws.services.sqs.model.{DeleteMessageRequest, ReceiveMessageRequest, SendMessageBatchRequestEntry}
import com.softwaremill.mqperf.config.{AWSCredentialsFromEnv, AWSPreferences}
import com.typesafe.config.Config

import scala.collection.JavaConverters._

class SqsMq(val config: Config) extends Mq {
  private val asyncClient =
    AWSCredentialsFromEnv() match {
      case Some(awsCredentials) =>
        AWSPreferences.configure(new AmazonSQSAsyncClient(awsCredentials))
      case None =>
        throw new IllegalStateException("AWS credentials are missing!")
    }

  private val asyncBufferedClient = new AmazonSQSBufferedAsyncClient(asyncClient)

  private val queueUrl = asyncClient.createQueue("mqperf-test-queue").getQueueUrl

  override type MsgId = String

  override def createSender() = new MqSender {
    override def send(msgs: List[String]) = {
      asyncClient.sendMessageBatch(
        queueUrl,
        msgs.zipWithIndex.map { case (m, i) => new SendMessageBatchRequestEntry(i.toString, m) }.asJava
      )
    }
  }

  override def createReceiver() = new MqReceiver {
    override def receive(maxMsgCount: Int) = {
      asyncBufferedClient
        .receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(maxMsgCount))
        .getMessages
        .asScala
        .map(m => (m.getReceiptHandle, m.getBody))
        .toList
    }

    override def ack(ids: List[MsgId]) = {
      ids.foreach { id => asyncBufferedClient.deleteMessageAsync(new DeleteMessageRequest(queueUrl, id)) }
    }
  }
}
