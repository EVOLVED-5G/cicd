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
    name = "dummy-netapp"
    namespace = "evol5-capif"
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
          image = "dockerhub.hi.inet/evolved-5g/dummy-netapp:latest"
          name  = "dummy-netapp"
        }
      }
    }
  }
}

resource "kubernetes_service" "dummy_netapp_service" {
metadata {
    name = "dummy-netapp"
    namespace = "evol5-capif"
  }
  spec {
    selector = {
      app = kubernetes_deployment.dummy_netapp.spec.0.template.0.metadata[0].labels.app
    }
    port {
      port = 8080
      target_port = 8080
    }
  }
}