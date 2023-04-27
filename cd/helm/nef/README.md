# Helm of NEF

![Version: 0.0.1](https://img.shields.io/badge/Version-0.0.1-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: 2.0.1](https://img.shields.io/badge/AppVersion-2.0.1-informational?style=for-the-badge) 

## Description

A Helm chart to NEF in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backend.backend.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-backend-1"` | The docker image repository to use |
| backend.backend.image.tag | string | `""` | @default Chart version |
| backend.backend.resources.limits.cpu | string | `"100m"` |  |
| backend.backend.resources.limits.memory | string | `"128Mi"` |  |
| backend.backend.resources.requests.cpu | string | `"100m"` |  |
| backend.backend.resources.requests.memory | string | `"128Mi"` |  |
| backend.ingress.host | string | `"nef-test.apps.ocp-epg.hi.inet"` |  |
| backend.ports[0].name | string | `"backend"` |  |
| backend.ports[0].port | int | `8888` |  |
| backend.ports[0].targetPort | int | `80` |  |
| backend.replicas | int | `1` |  |
| backend.type | string | `"ClusterIP"` |  |
| db.db.env.pgdata | string | `"/var/lib/postgresql/data/pgdata"` |  |
| db.db.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-db-1"` | The docker image repository to use |
| db.db.image.tag | string | `""` | @default Chart version |
| db.db.resources.limits.cpu | string | `"100m"` |  |
| db.db.resources.limits.memory | string | `"128Mi"` |  |
| db.db.resources.requests.cpu | string | `"100m"` |  |
| db.db.resources.requests.memory | string | `"128Mi"` |  |
| db.ports[0].name | string | `"db"` |  |
| db.ports[0].port | int | `5432` |  |
| db.ports[0].targetPort | int | `5432` |  |
| db.replicas | int | `1` |  |
| db.type | string | `"ClusterIP"` |  |
| env.backendCorsOrigins | string | `"[\"https://5g-api-emulator.medianetlab.eu\",\"http://localhost\"]"` |  |
| env.capifHostname | string | `"capif.apps.ocp-epg.hi.inet"` |  |
| env.capifHttpPort | string | `"30048"` |  |
| env.capifHttpsPort | string | `"30548"` |  |
| env.dockerImageBackend | string | `"backend"` |  |
| env.dockerImageFrontend | string | `"frontend"` |  |
| env.domain | string | `"localhost"` |  |
| env.emailsFromEmail | string | `"user@my-email.com"` |  |
| env.firstSuperuser | string | `"admin@my-email.com"` |  |
| env.firstSuperuserPassword | string | `"pass"` |  |
| env.mongoClient | string | `"mongodb://mongo:27017"` |  |
| env.mongoPassword | string | `"pass"` |  |
| env.mongoUser | string | `"root"` |  |
| env.pgadminDefaultEmail | string | `"admin@my-email.com"` |  |
| env.pgadminDefaultPassword | string | `"pass"` |  |
| env.pgadminListenPort | string | `"5050"` |  |
| env.postgresDb | string | `"app"` |  |
| env.postgresPassword | string | `"pass"` |  |
| env.postgresServer | string | `"db"` |  |
| env.postgresUser | string | `"postgres"` |  |
| env.projectName | string | `"NEF_Emulator"` |  |
| env.secretKey | string | `"2D47CF2958CEC7CC86C988E9F9684"` |  |
| env.sentryDsn | string | `""` |  |
| env.serverHost | string | `"https://localhost"` |  |
| env.serverName | string | `"localhost"` |  |
| env.serverPort | string | `"8888"` |  |
| env.smtpHost | string | `"mail.host.com"` |  |
| env.smtpPassword | string | `"pass"` |  |
| env.smtpPort | string | `"465"` |  |
| env.smtpTls | string | `"True"` |  |
| env.smtpUser | string | `"user"` |  |
| env.usePublicKeyVerification | string | `"True"` |  |
| env.usersOpenRegistration | string | `"true"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing  |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| mongo.mongo.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-mongo_nef-1"` | The docker image repository to use |
| mongo.mongo.image.tag | string | `""` | @default Chart version |
| mongo.mongo.resources.limits.cpu | string | `"100m"` |  |
| mongo.mongo.resources.limits.memory | string | `"128Mi"` |  |
| mongo.mongo.resources.requests.cpu | string | `"100m"` |  |
| mongo.mongo.resources.requests.memory | string | `"128Mi"` |  |
| mongo.ports[0].name | string | `"mongo"` |  |
| mongo.ports[0].port | int | `27017` |  |
| mongo.ports[0].targetPort | int | `27017` |  |
| mongo.replicas | int | `1` |  |
| mongo.type | string | `"ClusterIP"` |  |
| mongoExpress.mongoExpress.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-mongo-express-1"` | The docker image repository to use |
| mongoExpress.mongoExpress.image.tag | string | `""` | @default Chart version |
| mongoExpress.mongoExpress.resources.limits.cpu | string | `"100m"` |  |
| mongoExpress.mongoExpress.resources.limits.memory | string | `"128Mi"` |  |
| mongoExpress.mongoExpress.resources.requests.cpu | string | `"100m"` |  |
| mongoExpress.mongoExpress.resources.requests.memory | string | `"128Mi"` |  |
| mongoExpress.ports[0].name | string | `"mongo-express"` |  |
| mongoExpress.ports[0].port | int | `8081` |  |
| mongoExpress.ports[0].targetPort | int | `8081` |  |
| mongoExpress.replicas | int | `1` |  |
| mongoExpress.type | string | `"ClusterIP"` |  |
| pgadmin.pgadmin.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-pgadmin-1"` | The docker image repository to use |
| pgadmin.pgadmin.image.tag | string | `""` | @default Chart version |
| pgadmin.pgadmin.resources.limits.cpu | string | `"100m"` |  |
| pgadmin.pgadmin.resources.limits.memory | string | `"128Mi"` |  |
| pgadmin.pgadmin.resources.requests.cpu | string | `"100m"` |  |
| pgadmin.pgadmin.resources.requests.memory | string | `"128Mi"` |  |
| pgadmin.ports[0].name | string | `"pgadmin"` |  |
| pgadmin.ports[0].port | int | `5050` |  |
| pgadmin.ports[0].targetPort | int | `5050` |  |
| pgadmin.replicas | int | `1` |  |
| pgadmin.type | string | `"ClusterIP"` |  |






