# Helm of dummy-network-application

![Version: 0.1.0](https://img.shields.io/badge/Version-0.1.0-informational?style=for-the-badge)
![Type: application](https://img.shields.io/badge/Type-application-informational?style=for-the-badge) 
![AppVersion: 0.0.1](https://img.shields.io/badge/AppVersion-0.0.1-informational?style=for-the-badge) 

## Description

A Helm chart for Kubernetes

## Usage

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| env.capifCallbackPort | string | `"8080"` |  |
| env.capifCallbackUrl | string | `"http://dummy-network-app-capif.apps.ocp-epg.hi.inet:5000"` |  |
| env.capifHostname | string | `"dummy-network-app-capif.apps.ocp-epg.hi.inet"` |  |
| env.capifPortHttp | string | `"30048"` |  |
| env.capifPortHttps | string | `"30548"` |  |
| env.nefAddress | string | `"dummy-network-app-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefCallbackIp | string | `"dummy-network-app-nef.apps.ocp-epg.hi.inet"` |  |
| env.nefCallbackPort | string | `"8080"` |  |
| env.nefPassword | string | `"pass"` |  |
| env.nefPort | string | `"30548"` |  |
| env.nefUser | string | `"admin@my-email.com"` |  |
| env.pathToCerts | string | `"/usr/src/app/capif_onboarding"` |  |
| env.redisHost | string | `"redis-db"` |  |
| env.redisPort | string | `"6379"` |  |
| env.requestsCaBundle | string | `"/usr/src/app/ca.crt"` |  |
| env.sslCertFile | string | `"/usr/src/app/ca.crt"` |  |
| env.tsnIp | string | `"host.docker.internal"` |  |
| env.tsnPort | string | `"8899"` |  |
| environment | string | `"openshift"` | The Environment variable. It accepts: 'kuberentes-athens', 'kuberentes-uma', 'openshift' |
| ingress_ip | object | `{"athens":"10.161.1.126","uma":"10.11.23.49"}` | If env: 'kuberentes-athens' or env: 'kuberentes-uma', use the Ip address dude for the kubernetes to your Ingress Controller ej: kubectl -n NAMESPACE_CAPIF get ing s |
| kubernetesClusterDomain | string | `"cluster.local"` |  |
| pythonApp.pythonApp.image | object | `{"repository":"709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:dummy-network-app-python-app","tag":""}` | The docker image repository to use |
| pythonApp.pythonApp.image.tag | string | `""` | @default Chart version |
| pythonApp.replicas | int | `1` |  |
| redisDb.ports[0].name | string | `"redis"` |  |
| redisDb.ports[0].port | int | `6379` |  |
| redisDb.ports[0].targetPort | int | `6379` |  |
| redisDb.redisDb.image | object | `{"repository":"redis","tag":"latest"}` | The docker image repository to use |
| redisDb.redisDb.image.tag | string | `"latest"` | @default Chart version |
| redisDb.replicas | int | `1` |  |
| redisDb.type | string | `"ClusterIP"` |  |






