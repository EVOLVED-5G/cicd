# Helm of IninRmon Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to IninRmon Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.callbackAddress | string | `"rmonnetapp:80"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPath | string | `"/app/capif_onboarding/"` |  |
| env.capifPortHttp | string | `"30048"` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.collectorHost | string | `"https://evolved5g-collector.qmon.eu"` |  |
| env.collectorPass | string | `"test"` |  |
| env.collectorUser | string | `"test"` |  |
| env.frontendAddress | string | `"my-inin.apps.ocp-epg.hi.inet"` |  |
| env.mnHost | string | `"http://evolved5g-mn.qmon.eu"` |  |
| env.mnToken | string | `"d074feb62430a78e49b5a6da58cb81827e4229b9e3a4ecb28d2a3e47469871247e15ab95a9f34ac713682cebee1031c4da3a"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.netApiAddress | string | `"rmonnetapp:8888"` |  |
| env.netApiPass | string | `"pass"` |  |
| env.netApiProt | string | `"http"` |  |
| env.netApiUser | string | `"admin@my-email.com"` |  |
| env.netAppName | string | `"test"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.ports[0].name | string | `"netapp"` |  |
| netapp.ports[0].port | int | `80` |  |
| netapp.ports[0].targetPort | int | `80` |  |
| netapp.replicas | int | `1` |  |
| netapp.rmonnetapp.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5gvalidation:ininrmonnetapp","tag":""}` | The docker image repository to use |
| netapp.rmonnetapp.image.tag | string | `""` | @default Chart version |
| netapp.type | string | `"ClusterIP"` |  |






