package com.softwaremill.mqperf.config

import com.amazonaws.auth.BasicAWSCredentials
import com.typesafe.config.Config

object AWSCredentialsFromEnv {

  def apply(): Option[BasicAWSCredentials] = {

    def getEnv(name: String): Option[String] =
      sys.env.get(name).filterNot(_.isEmpty)

    for {
      accessKey <- getEnv("AWS_ACCESS_KEY_ID")
      secretKey <- getEnv("AWS_SECRET_ACCESS_KEY")
    } yield {
      new BasicAWSCredentials(accessKey, secretKey)
    }
  }

  def apply(config: Config): Option[BasicAWSCredentials] =
    for {
      awsKeyId <- config.getStringOpt("AWS_ACCESS_KEY_ID")
      awsSecretKey <- config.getStringOpt("AWS_SECRET_ACCESS_KEY")
    } yield new BasicAWSCredentials(awsKeyId, awsSecretKey)

}

