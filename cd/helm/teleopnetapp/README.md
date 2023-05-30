# Helm of TeleOp Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to TeleOp Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.capifCallbackUrl | string | `"http://localhost:5000"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | string | `"30048"` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.frontendAddress | string | `"my-teleop.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| netapp.ports[0].name | string | `"netapp"` |  |
| netapp.ports[0].port | int | `5000` |  |
| netapp.ports[0].targetPort | int | `5000` |  |
| netapp.replicas | int | `1` |  |
| netapp.teleopNetapp.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:teleopnetapp","tag":""}` | The docker image repository to use |
| netapp.teleopNetapp.image.tag | string | `""` | @default Chart version |
| netapp.type | string | `"ClusterIP"` |  |






