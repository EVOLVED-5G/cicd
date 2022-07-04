terraform {
  backend "s3" {
    encrypt        = true
    bucket         = "evolved5g-openshift-terraform-states"
    key            = "capif"
    region         = "eu-central-1"
    dynamodb_table = "terraform_locks"
  }
}
