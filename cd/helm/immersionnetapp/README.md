# Helm of Immersion Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart for Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | int | `30048` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.frontendAddress | string | `"my-immersion.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.netappId | string | `"imm_netapp"` |  |
| env.netappIp | string | `"netapp"` |  |
| env.netappName | string | `"IMM_Netapp"` |  |
| env.netappPort5G | int | `9999` |  |
| env.netappPortVapp | int | `9877` |  |
| env.netappPortWeb | int | `9998` |  |
| env.netappServerVapp | string | `"127.0.0.1"` |  |
| env.pathToCerts | string | `"/usr/src/app/capif_onboarding"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.immNetappContainer.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5gvalidation:immersionnetapp-imm_netapp_container","tag":""}` | The docker image repository to use |
| netapp.immNetappContainer.image.tag | string | `""` | @default Chart version |
| netapp.ports[0].name | string | `"9876"` |  |
| netapp.ports[0].port | int | `9876` |  |
| netapp.ports[0].targetPort | int | `9876` |  |
| netapp.ports[1].name | string | `"netapp-port-vapp"` |  |
| netapp.ports[1].port | int | `9877` |  |
| netapp.ports[1].targetPort | int | `9877` |  |
| netapp.ports[2].name | string | `"netapp-port-web"` |  |
| netapp.ports[2].port | int | `9998` |  |
| netapp.ports[2].targetPort | int | `9998` |  |
| netapp.ports[3].name | string | `"netapp-port-5g"` |  |
| netapp.ports[3].port | int | `9999` |  |
| netapp.ports[3].targetPort | int | `9999` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |






