#############################################
# VARIABLES
#############################################
variable "app_replicas" {
  description = "Number of pods - replicas for Dummy NetApp"
  type        = string
  default     = "2"
}

variable "fogus_namespace" {
  description = "repository to download image nef "
  type        = string
  default     = "evol5-fogusnetapp"
}

#############################################
#             FOGUS NETAPP FE
#############################################

variable "image_fogus_fe" {
  description = "repository to download Fogus Front-end "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:netappfe-1.0"
}
variable "name_fogus_fe" {
  description = "name of the fogus front-end"
  type        = string
  default     = "fogus_fe"
}

variable "fogus_fe_cpu_limit" {
  description = "repository to download image nef "
  type        = string
  default     = "125m"
}
variable "fogus_fe_cpu_request" {
  description = "repository to download image nef "
  type        = string
  default     = "65m"
}
variable "fogus_fe_memory_limit" {
  description = "repository to download image nef "
  type        = string
  default     = "512Mi"
}
variable "fogus_fe_memory_request" {
  description = "repository to download image nef "
  type        = string
  default     = "400Mi"
}
variable "fogus_fe_port" {
  description = "repository to download image nef "
  type        = number
  default     = 4200
}
variable "fogus_fe_target_port" {
  description = "repository to download image nef "
  type        = number
  default     = 4200
}

#############################################
#             FOGUS NETAPP POSTGRES
#############################################

variable "image_fogus_postgres" {
  description = "repository to download Fogus Postgres "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:netapppostgres-latest"
}
variable "name_fogus_postgres" {
  description = "name of the fogus postgres"
  type        = string
  default     = "fogus_postgres"
}

variable "fogus_postgres_cpu_limit" {
  description = "Postgres limit "
  type        = string
  default     = "125m"
}
variable "fogus_postgres_cpu_request" {
  description = "Postgres limit"
  type        = string
  default     = "65m"
}
variable "fogus_postgres_memory_limit" {
  description = "Postgres limit"
  type        = string
  default     = "512Mi"
}
variable "fogus_postgres_memory_request" {
  description = "Postgres limit"
  type        = string
  default     = "400Mi"
}
#No need
variable "fogus_postgres_port" {
  description = "Postgres port"
  type        = number
  default     = 5432
}
variable "fogus_postgres_target_port" {
  description = "Postgres target port "
  type        = number
  default     = 5432
}

#############################################
#             FOGUS NETAPP POSTGRES
#############################################

variable "image_fogus_postgres" {
  description = "repository to download Fogus Postgres "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:netapppostgres-latest"
}
variable "name_fogus_postgres" {
  description = "name of the fogus postgres"
  type        = string
  default     = "fogus_postgres"
}

variable "fogus_postgres_cpu_limit" {
  description = "Postgres limit "
  type        = string
  default     = "125m"
}
variable "fogus_postgres_cpu_request" {
  description = "Postgres limit"
  type        = string
  default     = "65m"
}
variable "fogus_postgres_memory_limit" {
  description = "Postgres limit"
  type        = string
  default     = "512Mi"
}
variable "fogus_postgres_memory_request" {
  description = "Postgres limit"
  type        = string
  default     = "400Mi"
}
#No need
variable "fogus_postgres_port" {
  description = "Postgres port"
  type        = number
  default     = 5432
}
variable "fogus_postgres_target_port" {
  description = "Postgres target port "
  type        = number
  default     = 5432
}

#############################################
#             FOGUS NETAPP DJANGO
#############################################

variable "image_fogus_django" {
  description = "repository to download Fogus django "
  type        = string
  default     = "709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:netappdjango-latest"
}
variable "name_fogus_django" {
  description = "name of the fogus django"
  type        = string
  default     = "fogus_django"
}

variable "fogus_django_cpu_limit" {
  description = "django limit "
  type        = string
  default     = "125m"
}
variable "fogus_django_cpu_request" {
  description = "django limit"
  type        = string
  default     = "65m"
}
variable "fogus_django_memory_limit" {
  description = "django limit"
  type        = string
  default     = "512Mi"
}
variable "fogus_django_memory_request" {
  description = "django limit"
  type        = string
  default     = "400Mi"
}
#No need
variable "fogus_django_port" {
  description = "django port"
  type        = number
  default     = 5432
}
variable "fogus_django_target_port" {
  description = "django target port "
  type        = number
  default     = 5432
}

#############################################
#               DEPLOYMENT
#############################################
#############################################
#             FOGUS NETAPP FE
#############################################

resource "kubernetes_deployment" "fogus_fe" {
  metadata {
    name      = var.name_fogus_fe
    namespace = var.fogus_namespace
    labels = {
      app = var.name_fogus_fe
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.name_fogus_fe
      }
    }
    template {
      metadata {
        labels = {
          app = var.name_fogus_fe
        }
      }
      spec {
        enable_service_links = false
        container {
          image = var.image_fogus_fe
          name  = var.name_fogus_fe

        
          resources {
            limits = {
              cpu    = var.fogus_fe_cpu_limit
              memory = var.fogus_fe_memory_limit
            }
            requests = {
              cpu    = var.fogus_fe_cpu_request
              memory = var.fogus_fe_memory_request
            }
          }
        }
        image_pull_secrets {
          name = "regcred"
        }
      }
    }
  }
}


resource "kubernetes_service" "fogus_fe_service" {
  metadata {
    name      = var.name_fogus_fe
    namespace = var.fogus_namespace
  }
  spec {
    selector = {
      app = kubernetes_deployment.fogus_fe.spec.0.template.0.metadata[0].labels.app
    }
    port {
      port        = var.fogus_fe_port
      target_port = var.fogus_fe_target_port
    }
  }
}

#############################################
#             FOGUS NETAPP POSTGRES
#############################################

resource "kubernetes_deployment" "fogus_postgres" {
  metadata {
    name      = var.name_fogus_postgres
    namespace = var.fogus_namespace
    labels = {
      app = var.name_fogus_postgres
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.name_fogus_postgres
      }
    }
    template {
      metadata {
        labels = {
          app = var.name_fogus_postgres
        }
      }
      spec {
        enable_service_links = false
        container {
          image = var.image_fogus_postgres
          name  = var.name_fogus_postgres


          env {
            name  = "POSTGRES_DB"
            value = "evolvedb"
          }
          env {
            name  = "POSTGRES_USER"
            value = "evolveclient"
          }
          env {
            name  = "POSTGRES_PASSWORD"
            value = "evolvepass"
          }

        
          resources {
            limits = {
              cpu    = var.fogus_postgres_cpu_limit
              memory = var.fogus_postgres_memory_limit
            }
            requests = {
              cpu    = var.fogus_postgres_cpu_request
              memory = var.fogus_postgres_memory_request
            }
          }
          volume_mount {
              mount_path = "/code"
              name       = "app-backend"
            }
          }
          volume {
            name = "app-backend"
            empty_dir {
            }
          }
        }
        image_pull_secrets {
          name = "regcred"
        }
      }
    }
  }
}


resource "kubernetes_service" "fogus_postgres_service" {
  metadata {
    name      = var.name_fogus_postgres
    namespace = var.fogus_namespace
  }
  spec {
    selector = {
      app = kubernetes_deployment.fogus_postgres.spec.0.template.0.metadata[0].labels.app
    }
    port {
      port        = var.fogus_postgres_port
      target_port = var.fogus_postgres_target_port
    }
  }
}

#############################################
#         FOGUS NETAPP DJANGO
#############################################

resource "kubernetes_deployment" "fogus_django" {
  metadata {
    name      = var.name_fogus_django
    namespace = var.fogus_namespace
    labels = {
      app = var.name_fogus_django
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.name_fogus_django
      }
    }
    template {
      metadata {
        labels = {
          app = var.name_fogus_django
        }
      }
      spec {
        enable_service_links = false
        container {
          image = var.image_fogus_django
          name  = var.name_fogus_django

        
          resources {
            limits = {
              cpu    = var.fogus_django_cpu_limit
              memory = var.fogus_django_memory_limit
            }
            requests = {
              cpu    = var.fogus_django_cpu_request
              memory = var.fogus_django_memory_request
            }
          }
        }
        image_pull_secrets {
          name = "regcred"
        }
      }
    }
  }
}


resource "kubernetes_service" "fogus_django_service" {
  metadata {
    name      = var.name_fogus_django
    namespace = var.fogus_namespace
  }
  spec {
    selector = {
      app = kubernetes_deployment.fogus_django.spec.0.template.0.metadata[0].labels.app
    }
    port {
      port        = var.fogus_django_port
      target_port = var.fogus_django_target_port
    }
  }
}