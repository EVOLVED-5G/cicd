# Helm of Fogus

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to GMI Aero Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.capifCallbackUrl | string | `"http://localhost:5000"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | string | `"30048"` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.frontendAddress | string | `"my-gmi.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefCallbackUrl | string | `"http://my-nef.apps.ocp-epg.hi.inet/monitoring/callback"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.netappId | string | `"gmi_netapp"` |  |
| env.netappName | string | `"GMI_Netapp"` |  |
| env.netappPath | string | `"/src"` |  |
| env.netappPortVapp | int | `8383` |  |
| env.pathToCerts | string | `"/code/src/capif_onboarding"` |  |
| env.ueRequestedIp | string | `"10.0.0.1"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.gmiNetappContainer.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:gmiaeronetapp-gmi_netapp_container","tag":""}` | The docker image repository to use |
| netapp.gmiNetappContainer.image.tag | string | `""` | @default Chart version |
| netapp.ports[0].name | string | `"netapp"` |  |
| netapp.ports[0].port | int | `8383` |  |
| netapp.ports[0].targetPort | int | `8383` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |






