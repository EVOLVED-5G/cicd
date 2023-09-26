# Helm of CafaTech Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to CafaTech Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.callbackAddress | string | `"http://netapp:55555/nefcallbacks"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPathToCerts | string | `"certificates"` |  |
| env.capifPortHttp | string | `"80"` |  |
| env.capifPortHttps | string | `"443"` |  |
| env.frontencallbackdAddress | string | `"http://netapp:5555"` |  |
| env.frontendAddress | string | `"netapp.app.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.netappId | string | `"CAFA-NetApp-3"` |  |
| env.netappIp | string | `"http://netapp:5555"` |  |
| env.netappName | string | `"CafaTechNetApp3"` |  |
| env.netappPass | string | `"netapp_pass"` |  |
| env.netappUser | string | `"netapp_user"` |  |
| env.serverForVapp | string | `"netapp:5000"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","cosmote":"172.25.2.100","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.cafatechNetapp3.image.tag | string | `"cafatechnetapp4-cafatech-netapp-4"` | @default Chart version |
| netapp.ports[0].name | string | `"netapp"` |  |
| netapp.ports[0].port | int | `5555` |  |
| netapp.ports[0].targetPort | int | `5555` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |
| pipeline | string | `"verification"` |  |





