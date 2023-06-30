# Helm of NEF

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: 2.2.2](https://img.shields.io/badge/AppVersion-2.2.2-informational?style=for-the-badge) 

## Description

A Helm chart to NEF in Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backend.backend.image.repository | string | `"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-backend-1"` | The docker image repository to use |
| backend.backend.image.tag | string | `""` | @default Chart version |
| backend.backend.imagePullPolicy | string | `"Always"` |  |
| backend.ingress.host | string | `"localization-nef.apps.ocp-epg.hi.inet"` |  |
| backend.ports[0].port | int | `80` |  |
| backend.ports[0].protocol | string | `"TCP"` |  |
| backend.ports[0].targetPort | int | `80` |  |
| backend.replicas | int | `1` |  |
| backend.type | string | `"ClusterIP"` |  |
| db.db.env.pgdata | string | `"/var/lib/postgresql/data/pgdata"` |  |
| db.db.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-db-1","tag":""}` | The docker image repository to use |
| db.db.image.tag | string | `""` | @default Chart version |
| db.ports[0].port | int | `5432` |  |
| db.ports[0].protocol | string | `"TCP"` |  |
| db.ports[0].targetPort | int | `0` |  |
| db.replicas | int | `1` |  |
| db.type | string | `"ClusterIP"` |  |
| env.backendCorsOrigins | string | `"[\"https://5g-api-emulator.medianetlab.eu\",\"http://localhost\"]"` |  |
| env.backendTag | string | `"1.0"` |  |
| env.capifHostname | string | `"localization-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifHttpPort | string | `"80"` |  |
| env.capifHttpsPort | string | `"443"` |  |
| env.dockerImageBackend | string | `"dimfrag/nef-backend"` |  |
| env.dockerImageFrontend | string | `"frontend"` |  |
| env.dockerImageProxy | string | `"dimfrag/nef-nginx"` |  |
| env.domain | string | `"localhost"` |  |
| env.emailsFromEmail | string | `"user@my-email.com"` |  |
| env.externalNet | string | `"true"` |  |
| env.firstSuperuser | string | `"admin@my-email.com"` |  |
| env.firstSuperuserPassword | string | `"pass"` |  |
| env.meConfigMongodbUrl | string | `"mongodb://root:pass@nef-mongo:27017/"` |  |
| env.mongoClient | string | `"mongodb://nef-mongo:27017/"` |  |
| env.mongoExpressEnableAdmin | string | `"true"` |  |
| env.mongoPassword | string | `"pass"` |  |
| env.mongoUser | string | `"root"` |  |
| env.nginxHttp | string | `"8090"` |  |
| env.nginxHttps | string | `"4443"` |  |
| env.pgadminDefaultEmail | string | `"admin@my-email.com"` |  |
| env.pgadminDefaultPassword | string | `"pass"` |  |
| env.pgadminListenPort | string | `"5050"` |  |
| env.postgresDb | string | `"app"` |  |
| env.postgresPassword | string | `"pass"` |  |
| env.postgresUser | string | `"postgres"` |  |
| env.projectName | string | `"NEF_Emulator"` |  |
| env.proxyTag | string | `"1.0"` |  |
| env.secretKey | string | `"2D47CF2958CEC7CC86C988E9F9684"` |  |
| env.sentryDsn | string | `""` |  |
| env.serverHost | string | `"http://localhost"` |  |
| env.serverName | string | `"localhost"` |  |
| env.serverPort | string | `"8888"` |  |
| env.smtpHost | string | `"mail.host.com"` |  |
| env.smtpPassword | string | `"pass"` |  |
| env.smtpPort | string | `"465"` |  |
| env.smtpTls | string | `"True"` |  |
| env.smtpUser | string | `"user"` |  |
| env.usePublicKeyVerification | string | `"true"` |  |
| env.usersOpenRegistration | string | `"true"` |  |
| environment | string | `"kubernetes-uma"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing  |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| mongo.nefMongo.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-mongo_nef-1","tag":""}` | The docker image repository to use |
| mongo.nefMongo.image.tag | string | `""` | @default Chart version |
| mongo.ports[0].port | int | `27017` |  |
| mongo.ports[0].protocol | string | `"TCP"` |  |
| mongo.ports[0].targetPort | int | `0` |  |
| mongo.replicas | int | `1` |  |
| mongo.type | string | `"ClusterIP"` |  |
| mongoExpress.mongoExpress.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:nef_emulator-mongo-express-1","tag":""}` | The docker image repository to use |
| mongoExpress.mongoExpress.image.tag | string | `""` | @default Chart version |
| mongoExpress.ports[0].port | int | `8081` |  |
| mongoExpress.ports[0].protocol | string | `"TCP"` |  |
| mongoExpress.ports[0].targetPort | int | `0` |  |
| mongoExpress.replicas | int | `1` |  |
| mongoExpress.type | string | `"ClusterIP"` |  |






