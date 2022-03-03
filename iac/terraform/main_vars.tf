#############################################
# VARIABLES
#############################################
variable "app_replicas" {
  description = "Number of pods - replicas for Dummy NetApp"
  type        = string
  default     = "1"
}

variable "namespace_name" {
  description = "name of the namespace"
  type        = string
  default     = "dummy-netapp"
}

variable "netapp_name" {
  description = "name of the netapp"
  type        = string
  default     = "dummy-netapp"
}



