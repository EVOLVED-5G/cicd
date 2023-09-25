{{/*
Expand the name of the chart.
*/}}
{{- define "8bells.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
 Define base aws repo
 */}}
{{- define "8bells.baseRepo" -}}
{{- printf " 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g" }}
{{- end }}

{{/*
 Define type of pipeline
 */}}
{{- define "8bells.pipeline" -}}
{{- if eq .Values.pipeline "validation" }}
{{- printf "validation" }}
{{- else }}
{{- printf "certification" }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "8bells.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "8bells.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "8bells.labels" -}}
helm.sh/chart: {{ include "8bells.chart" . }}
{{ include "8bells.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "8bells.selectorLabels" -}}
app.kubernetes.io/name: {{ include "8bells.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "8bells.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "8bells.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
