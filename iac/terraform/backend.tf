terraform {
  backend "s3" {
    encrypt        = true
    region         = "eu-central-1"
    dynamodb_table = "terraform_locks"
  }
}
