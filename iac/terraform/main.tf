#############################################
# DUMMY NETAPP
#############################################
resource "kubernetes_deployment" "dummy_netapp" {
  metadata {
    name      = var.netapp_name
    namespace = var.namespace_name
    labels = {
      app = var.netapp_name
    }
  }

  spec {
    replicas = var.app_replicas
    selector {
      match_labels = {
        app = var.netapp_name
      }
    }
    template {
      metadata {
        labels = {
          app = var.netapp_name
        }
      }
      spec {
        enable_service_links = false
        container {
          image = "dockerhub.hi.inet/evolved-5g/dummy-netapp:1.0.233"
          name  = var.netapp_name
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
    }
  }
}

resource "kubernetes_service" "dummy_netapp_service" {
  metadata {
    name      = var.netapp_name
    namespace = var.namespace_name
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
