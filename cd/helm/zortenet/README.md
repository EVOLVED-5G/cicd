# Helm of Zortenet Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to Zortenet Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.callbackAddress | string | `"app:5000"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | int | `30048` |  |
| env.capifPortHttps | int | `30548` |  |
| env.frontendAddress | string | `"my-immersion.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.pathToCerts | string | `"/zortenet_netapp/capif_onboarding"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.ports[0].name | string | `"app"` |  |
| netapp.ports[0].port | int | `5000` |  |
| netapp.ports[0].targetPort | int | `5000` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |
| netapp.zorteNetapp.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:zortenetnetapp-zorte_netapp","tag":""}` | The docker image repository to use |
| netapp.zorteNetapp.image.tag | string | `""` | @default Chart version |






