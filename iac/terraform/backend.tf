terraform {
  backend "s3" {
    encrypt        = true
    bucket         = "evolved5g-dummyapp-tfstate"
    key            = "test"
    region         = "eu-central-1"
    dynamodb_table = "terraform_locks"
  }
}
