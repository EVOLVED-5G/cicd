# Helm of 8Bells Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to 8bells Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| adminer.NetappAdminer.image.tag | string | `"8bellsnetapp-8b_netapp_adminer"` | @default Chart version |
| adminer.env.adminerPassword | string | `"1234"` |  |
| adminer.env.adminerPortOne | string | `"8008"` |  |
| adminer.env.adminerPortTwo | string | `"8080"` |  |
| adminer.ports[0].name | string | `"8008"` |  |
| adminer.ports[0].port | int | `8008` |  |
| adminer.ports[0].targetPort | int | `8080` |  |
| adminer.replicas | int | `1` |  |
| adminer.type | string | `"ClusterIP"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | string | `"80"` |  |
| env.capifPortHttps | string | `"443"` |  |
| env.frontendAddress | string | `"netapp.app.ocp-epg.hi.inet"` |  |
| env.hostUrl | string | `"http://netapp"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.vappIp | string | `"10.10.10.40"` |  |
| env.vappPass | string | `"admin8b"` |  |
| env.vappUser | string | `"admin8b"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","cosmote":"172.25.2.100","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.Netapp.image.tag | string | `"8bellsnetapp-8b_netapp"` | @default Chart version |
| netapp.env.callbackAddress | string | `"http://netapp:5000/monitoring/callback:5000"` |  |
| netapp.env.callbackAdr | string | `"http://netapp:5000/monitoring/callback"` |  |
| netapp.env.nefPassword | string | `"pass"` |  |
| netapp.env.nefPort | int | `4443` |  |
| netapp.env.nefUser | string | `"admin@my-email.com"` |  |
| netapp.env.netappIp | string | `"http://netapp:5000"` |  |
| netapp.env.netappName | string | `"myNetapp"` |  |
| netapp.env.netappPort | string | `"5000"` |  |
| netapp.env.vappIp | string | `"10.10.10.40"` |  |
| netapp.env.vappPass | string | `"admin8b"` |  |
| netapp.env.vappUser | string | `"admin8b"` |  |
| netapp.ports[0].name | string | `"5000"` |  |
| netapp.ports[0].port | int | `5000` |  |
| netapp.ports[0].targetPort | int | `5000` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |
| pipeline | string | `"verification"` |  |
| postgres.NetappDb.image.tag | string | `"8bellsnetapp-8b_netapp_db"` | @default Chart version |
| postgres.env.postgresDb | string | `"postgres"` |  |
| postgres.env.postgresPassword | string | `"postgres"` |  |
| postgres.env.postgresPort | string | `"5432"` |  |
| postgres.env.postgresUsername | string | `"postgres"` |  |
| postgres.ports[0].name | string | `"5432"` |  |
| postgres.ports[0].port | int | `5432` |  |
| postgres.ports[0].targetPort | int | `5432` |  |
| postgres.replicas | int | `1` |  |
| postgres.type | string | `"ClusterIP"` |  |





