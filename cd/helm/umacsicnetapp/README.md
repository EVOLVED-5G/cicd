# Helm of UMACsic Network App

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: latest](https://img.shields.io/badge/AppVersion-latest-informational?style=for-the-badge) 

## Description

A Helm chart to UMACsic Network App in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| db.ports[0].name | string | `"db-port"` |  |
| db.ports[0].port | int | `5432` |  |
| db.ports[0].targetPort | int | `5432` |  |
| db.postgresContainer.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:umacsictest-postgres_container","tag":""}` | The docker image repository to use |
| db.postgresContainer.image.tag | string | `""` | @default Chart version |
| db.replicas | int | `1` |  |
| db.type | string | `"ClusterIP"` |  |
| env.callbackAddress | string | `""` |  |
| env.capifHostname | string | `"my-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | int | `30048` |  |
| env.capifPortHttps | int | `30548` |  |
| env.error404Help | string | `"False"` |  |
| env.flaskApp | string | `"main.py"` |  |
| env.frontendAddress | string | `"my-umacsic.apps.ocp-epg.hi.inet"` |  |
| env.nefAddress | string | `"my-nef.apps.ocp-epg.hi.inet"` |  |
| env.pathToCerts | string | `"/usr/src/app/certs"` |  |
| env.pgdata | string | `"/var/lib/postgresql/data"` |  |
| env.postgresDb | string | `"netappdb"` |  |
| env.postgresHostname | string | `"db"` |  |
| env.postgresPassword | string | `"secret"` |  |
| env.postgresPort | int | `5432` |  |
| env.postgresUser | string | `"netapp"` |  |
| env.secretKey | string | `"7110c8ae51a4b5af97be6534caefs0e4bb9bdcb3380af00sr50b23a5d1616bf319bc298105da20fe"` |  |
| env.showSqlalchemyLogMessages | string | `"False"` |  |
| env.sqlalchemyTrackModifications | string | `"False"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| netapp.netapp.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:umacsictest-netapp","tag":""}` | The docker image repository to use |
| netapp.netapp.image.tag | string | `""` | @default Chart version |
| netapp.ports[0].name | string | `"netapp-port"` |  |
| netapp.ports[0].port | int | `10001` |  |
| netapp.ports[0].targetPort | int | `10001` |  |
| netapp.replicas | int | `1` |  |
| netapp.type | string | `"ClusterIP"` |  |






