terraform {
  backend "s3" {
    encrypt        = true
    bucket         = "evolve5g-dummy-terraform-states"
    key            = "dummynetapp"
    region         = "eu-central-1"
    dynamodb_table = "terraform_locks"
  }
}
