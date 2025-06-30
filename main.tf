provider "aws" {
  region = "us-east-2"
}

resource "aws_kinesis_stream" "pims_raw_data" {
  name        = "pims-raw-data"
  shard_count = 1
}

resource "aws_s3_bucket" "raw_data_bucket" {
  bucket        = "pims-raw-data-bucket"
  force_destroy = true
}

resource "aws_iam_role" "lambda_role" {
  name = "lambda-kinesis-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect    = "Allow",
      Principal = { Service = "lambda.amazonaws.com" },
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_policy_attach" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaKinesisExecutionRole"
}

resource "aws_lambda_function" "kinesis_consumer" {
  filename         = "lambda-java-consumer/lambda-java-consumer.zip"
  function_name    = "KinesisToS3Consumer"
  role             = aws_iam_role.lambda_role.arn
  handler          = "com.example.KinesisHandler::handleRequest"
  runtime          = "java11"
  source_code_hash = filebase64sha256("lambda-java-consumer/lambda-java-consumer.zip")

  environment {
    variables = {
      BUCKET_NAME = aws_s3_bucket.raw_data_bucket.bucket
    }
  }
}

resource "aws_lambda_event_source_mapping" "kinesis_trigger" {
  event_source_arn  = aws_kinesis_stream.pims_raw_data.arn
  function_name     = aws_lambda_function.kinesis_consumer.arn
  starting_position = "LATEST"
}
