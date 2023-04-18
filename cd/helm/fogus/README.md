# Helm of Fogus

![Version: 0.0.1](https://img.shields.io/badge/Version-0.0.1-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart of fogus for Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| dbnetapp.netapppostgres.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:fogusnetapp-netapppostgres","tag":"latest"}` | The docker image repository to use |
| dbnetapp.netapppostgres.image.tag | string | `"latest"` | @default Chart version |
| dbnetapp.netapppostgres.resources.limits.cpu | string | `"100m"` |  |
| dbnetapp.netapppostgres.resources.limits.memory | string | `"128Mi"` |  |
| dbnetapp.netapppostgres.resources.requests.cpu | string | `"100m"` |  |
| dbnetapp.netapppostgres.resources.requests.memory | string | `"128Mi"` |  |
| dbnetapp.ports[0].name | string | `"5432"` |  |
| dbnetapp.ports[0].port | int | `5432` |  |
| dbnetapp.ports[0].targetPort | int | `5432` |  |
| dbnetapp.replicas | int | `1` |  |
| dbnetapp.type | string | `"ClusterIP"` |  |
| django.netappdjango.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:fogusnetapp-netappdjango","tag":"latest"}` | The docker image repository to use |
| django.netappdjango.image.tag | string | `"latest"` | @default Chart version |
| django.netappdjango.resources.limits.cpu | string | `"100m"` |  |
| django.netappdjango.resources.limits.memory | string | `"128Mi"` |  |
| django.netappdjango.resources.requests.cpu | string | `"100m"` |  |
| django.netappdjango.resources.requests.memory | string | `"128Mi"` |  |
| django.ports[0].name | string | `"8000"` |  |
| django.ports[0].port | int | `8000` |  |
| django.ports[0].targetPort | int | `8000` |  |
| django.replicas | int | `1` |  |
| django.type | string | `"ClusterIP"` |  |
| env.callbackAddress | string | `"192.168.1.5:8000"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | string | `"30048"` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.frontendAddress | string | `"my-fogus.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.netappAddress | string | `"192.168.1.5:8000"` |  |
| env.pathToCerts | string | `"/code/capif_onboarding"` |  |
| env.postgresDb | string | `"evolvedb"` |  |
| env.postgresPassword | string | `"evolvepass"` |  |
| env.postgresUser | string | `"evolveclient"` |  |
| env.vappAddress | string | `"195.134.66.79:8443"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.netappfe.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:fogusnetapp-netappfe","tag":"latest"}` | The docker image repository to use |
| netapp.netappfe.image.tag | string | `"latest"` | @default Chart version |
| netapp.netappfe.resources | object | `{}` |  |
| netapp.ports[0].name | string | `"fe"` |  |
| netapp.ports[0].port | int | `4200` |  |
| netapp.ports[0].targetPort | int | `4200` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |






