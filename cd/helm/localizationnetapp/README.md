# Helm of localization Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: 4.0](https://img.shields.io/badge/AppVersion-4.0-informational?style=for-the-badge) 

## Description

A Helm chart to localization Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.callbackAddress | string | `"netapp:8000"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | string | `"30048"` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.capifUsername | string | `"user1"` |  |
| env.ddsExternalAddress | string | `"10.11.23.53"` |  |
| env.environment | string | `"development"` |  |
| env.frontendAddress | string | `"localization.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefPort | string | `"30548"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.network | string | `"evolved5G_dds"` |  |
| env.pathToCerts | string | `"/evolved5g/cfg/capif_onboarding"` |  |
| env.rosDomainId | string | `"1"` |  |
| env.ueExternalId | string | `"10002@domain.com"` |  |
| env.vappAddress | string | `"195.134.66.79:7000"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| netapp.localizationNetapp.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:localizationnetapp","tag":""}` | The docker image repository to use |
| netapp.localizationNetapp.image.tag | string | `""` | @default Chart version |
| netapp.portsUdp[0].name | string | `"netapp-upd-0"` |  |
| netapp.portsUdp[0].port | int | `7660` |  |
| netapp.portsUdp[0].protocol | string | `"UDP"` |  |
| netapp.portsUdp[0].targetPort | int | `7660` |  |
| netapp.portsUdp[1].name | string | `"netapp-upd-1"` |  |
| netapp.portsUdp[1].port | int | `7661` |  |
| netapp.portsUdp[1].protocol | string | `"UDP"` |  |
| netapp.portsUdp[1].targetPort | int | `7661` |  |
| netapp.portsUdp[2].name | string | `"netapp-upd-2"` |  |
| netapp.portsUdp[2].port | int | `7662` |  |
| netapp.portsUdp[2].protocol | string | `"UDP"` |  |
| netapp.portsUdp[2].targetPort | int | `7662` |  |
| netapp.portsUdp[3].name | string | `"netapp-upd-3"` |  |
| netapp.portsUdp[3].port | int | `7663` |  |
| netapp.portsUdp[3].protocol | string | `"UDP"` |  |
| netapp.portsUdp[3].targetPort | int | `7663` |  |
| netapp.portsUdp[4].name | string | `"netapp-upd-4"` |  |
| netapp.portsUdp[4].port | int | `7664` |  |
| netapp.portsUdp[4].protocol | string | `"UDP"` |  |
| netapp.portsUdp[4].targetPort | int | `7664` |  |
| netapp.portsUdp[5].name | string | `"netapp-upd-5"` |  |
| netapp.portsUdp[5].port | int | `7665` |  |
| netapp.portsUdp[5].protocol | string | `"UDP"` |  |
| netapp.portsUdp[5].targetPort | int | `7665` |  |
| netapp.ports[0].name | string | `"netapp"` |  |
| netapp.ports[0].port | int | `8000` |  |
| netapp.ports[0].targetPort | int | `8000` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |
| netapp.typeUdp | string | `"LoadBalancer"` |  |






