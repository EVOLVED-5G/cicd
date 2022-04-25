#############################################
# VARIABLES
#############################################
variable "app_replicas" {
  description = "Number of pods - replicas for Dummy NetApp"
  type        = string
  default     = "2"
}
variable "nef_namespace" {
  description = "repository to download image nef "
  type        = string
  default     = "evol5-capif"
}

#############################################
#               NEF_BACKEND
#############################################

variable "image_nef_bakend" {
  description = "repository to download image nef "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator_backend_1-1.0.10"
}
variable "name_nef_backend" {
  description = "repository to download image nef "
  type        = string
  default     = "nef-backend"
}
variable "nef_backend_cpu_limit" {
  description = "repository to download image nef "
  type        = string
  default     = "125m"
}
variable "nef_backend_cpu_request" {
  description = "repository to download image nef "
  type        = string
  default     = "65m"
}
variable "nef_backend_memory_limit" {
  description = "repository to download image nef "
  type        = string
  default     = "512Mi"
}
variable "nef_backend_memory_request" {
  description = "repository to download image nef "
  type        = string
  default     = "400Mi"
}
variable "nef_backend_port" {
  description = "repository to download image nef "
  type        = number
  default     = 80
}
variable "nef_backend_target_port" {
  description = "repository to download image nef "
  type        = number
  default     = 80
}

#############################################
#                 NEF_DB                 #
#############################################

variable "image_nef_db" {
  description = "Image of the NEF database "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator_db_1-1.0.10"
}
variable "name_nef_db" {
  description = "Name for the NEF Database "
  type        = string
  default     = "nef-db"
}
variable "nef_db_cpu_limit" {
  description = "CPU Limitfor the NEF db image "
  type        = string
  default     = "125m"
}
variable "nef_db_cpu_request" {
  description = "CPU for the NEF db image "
  type        = string
  default     = "65m"
}
variable "nef_db_memory_limit" {
  description = "Memory Limit for the NEF db image"
  type        = string
  default     = "512Mi"
}
variable "nef_db_memory_request" {
  description = "Memory for the NEF db image"
  type        = string
  default     = "400Mi"
}

#############################################
#                 NEF_MONGO                 #
#############################################

variable "image_nef_mongo" {
  description = "Image of the NEF mongo database "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator_mongo_1-1.0.10"
}
variable "name_nef_mongo" {
  description = "Name for the NEF mongo Database "
  type        = string
  default     = "nef-mongo"
}
variable "nef_mongo_cpu_limit" {
  description = "CPU Limitfor the NEF mongo db image "
  type        = string
  default     = "125m"
}
variable "nef_mongo_cpu_request" {
  description = "CPU for the NEF mongo db image "
  type        = string
  default     = "65m"
}
variable "nef_mongo_memory_limit" {
  description = "Memory Limit for the NEF mongo db image"
  type        = string
  default     = "512Mi"
}
variable "nef_mongo_memory_request" {
  description = "Memory for the NEF mongo db image"
  type        = string
  default     = "400Mi"
}

#############################################
#              NEF_EMULATOR
#############################################
#############################################
#               NEF_BACKEND
#############################################
resource "kubernetes_deployment" "nef_backend" {
  metadata {
    name      = var.name_nef_backend
    namespace = var.nef_namespace
    labels = {
      app = var.name_nef_backend
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.name_nef_backend
      }
    }
    template {
      metadata {
        labels = {
          app = var.name_nef_backend
        }
      }
      spec {
        enable_service_links = false
        container {
          image = var.image_nef_bakend
          name  = var.name_nef_backend
          resources {
            limits = {
              cpu    = var.nef_backend_cpu_limit
              memory = var.nef_backend_memory_limit
            }
            requests = {
              cpu    = var.nef_backend_cpu_request
              memory = var.nef_backend_memory_request
            }
          }

        }
      }
    }
  }
}

resource "kubernetes_service" "nef_db_service" {
  metadata {
    name      = var.name_nef_backend
    namespace = var.nef_namespace
  }
  spec {
    selector = {
      app = kubernetes_deployment.nef_db.spec.0.template.0.metadata[0].labels.app
    }
    port {
      port        = var.nef_backend_port
      target_port = var.nef_backend_target_port
    }
  }
}
#############################################
#                 NEF_MONGO
#############################################
resource "kubernetes_deployment" "nef_mongo" {
  metadata {
    name      = var.name_nef_mongo
    namespace = var.nef_namespace
    labels = {
      app = var.name_nef_mongo
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.name_nef_mongo
      }
    }
    template {
      metadata {
        labels = {
          app = var.name_nef_mongo
        }
      }
      spec {
        enable_service_links = false
        container {
          image = var.image_nef_mongo
          name  = var.name_nef_mongo
          resources {
            limits = {
              cpu    = var.nef_mongo_cpu_limit
              memory = var.nef_mongo_memory_limit
            }
            requests = {
              cpu    = var.nef_mongo_cpu_request
              memory = var.nef_mongo_memory_request
            }
          }
        }
      }
    }
  }
}

#############################################
#                  NEF_DB
#############################################
resource "kubernetes_deployment" "nef_db" {
  metadata {
    name      = var.name_nef_db
    namespace = var.nef_namespace
    labels = {
      app = var.name_nef_db
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.name_nef_db
      }
    }
    template {
      metadata {
        labels = {
          app = var.name_nef_db
        }
      }
      spec {
        enable_service_links = false
        container {
          image = var.image_nef_db
          name  = var.name_nef_db
          resources {
            limits = {
              cpu    = var.nef_db_cpu_limit
              memory = var.nef_db_memory_limit
            }
            requests = {
              cpu    = var.nef_db_cpu_request
              memory = var.nef_db_memory_request
            }
          }
        }
      }
    }
  }
}
