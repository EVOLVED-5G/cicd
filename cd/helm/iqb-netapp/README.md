# Helm of IQB Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to IQB Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| callbacks.callbacks.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:iqb-netapp-callbacks","tag":""}` | The docker image repository to use |
| callbacks.callbacks.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:iqb-netapp-callbacks"` | @default Chart version |
| callbacks.ports[0].name | string | `"netapp-callbacks"` |  |
| callbacks.ports[0].port | int | `5002` |  |
| callbacks.ports[0].targetPort | int | `5002` |  |
| callbacks.replicas | int | `1` |  |
| callbacks.type | string | `"ClusterIP"` |  |
| env.callbackAddress | string | `"callbacks:5002"` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | int | `30048` |  |
| env.capifPortHttps | int | `30548` |  |
| env.frontendAddress | string | `"my-iqb.apps.ocp-epg.hi.inet"` |  |
| env.keycloakAddress | string | `"keycloak:8980/auth"` |  |
| env.keycloakAdmin | string | `"admin"` |  |
| env.keycloakAdminPassword | string | `"admin"` |  |
| env.keycloakRealm | string | `"EVOLVED-5G"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefCallbackUrl | string | `""` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.netappId | string | `"myNetapp"` |  |
| env.netappIp | string | `"netapp"` |  |
| env.netappName | string | `"iqb"` |  |
| env.netappPort5G | int | `6000` |  |
| env.netappPortVapp | string | `""` |  |
| env.netappPortWeb | int | `5000` |  |
| env.netappServerVapp | string | `""` |  |
| env.pathToCerts | string | `"/app/capif_onboarding"` |  |
| env.vappAddress | string | `"NA"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| keycloak.keycloak.env.keycloakImport | string | `"/tmp/import/realm-export.json"` |  |
| keycloak.keycloak.env.keycloakLoglevel | string | `"DEBUG"` |  |
| keycloak.keycloak.env.keycloakPassword | string | `"admin"` |  |
| keycloak.keycloak.env.keycloakUser | string | `"admin"` |  |
| keycloak.keycloak.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:iqb-netapp-keycloak","tag":""}` | The docker image repository to use |
| keycloak.keycloak.image.tag | string | `""` | @default Chart version |
| keycloak.ports[0].name | string | `"keycloak"` |  |
| keycloak.ports[0].port | int | `8980` |  |
| keycloak.ports[0].targetPort | int | `8080` |  |
| keycloak.replicas | int | `1` |  |
| keycloak.type | string | `"ClusterIP"` |  |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.iqbNetapp.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:iqb-netapp-iqb_netapp"` | The docker image repository to use |
| netapp.iqbNetapp.image.tag | string | `""` | @default Chart version |
| netapp.ports[0].name | string | `"netapp-port-web"` |  |
| netapp.ports[0].port | int | `5000` |  |
| netapp.ports[0].targetPort | int | `5000` |  |
| netapp.ports[1].name | string | `"netapp-port-5g"` |  |
| netapp.ports[1].port | int | `6000` |  |
| netapp.ports[1].targetPort | int | `6000` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |






