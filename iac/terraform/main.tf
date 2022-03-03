#############################################
# VARIABLES
#############################################
variable "app_replicas" {
  description = "Number of pods - replicas for Dummy NetApp"
  type        = string
  default     = "1"
}

#############################################
# DUMMY NETAPP
#############################################
resource "kubernetes_deployment" "dummy_netapp" {
  metadata {
    name      = "dummy-netapp"
    namespace = "evolved5g"
    labels = {
      app = "dummy-netapp"
    }
  }
  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = "dummy-netapp"
      }
    }
    template {
      metadata {
        labels = {
          app = "dummy-netapp"
        }
      }
      spec {
        enable_service_links = false
        container {
          image = "dockerhub.hi.inet/evolved-5g/dummy-netapp:1.0.233"
          name  = "dummy-netapp"
          resources {
            limits = {
              cpu    = "125m"
              memory = "256Mi"
            }
            requests = {
              cpu    = "65m"
              memory = "50Mi"
            }
          }
        }
      }

      resource "kubernetes_service" "dummy_netapp_service" {
        metadata {
          name      = "dummy-netapp"
          namespace = "evolved5g"
        }
        spec {
          selector = {
            app = kubernetes_deployment.dummy_netapp.spec.0.template.0.metadata[0].labels.app
          }
          port {
            port        = 8080
            target_port = 8080
          }
        }
      }
    }
  }
}
