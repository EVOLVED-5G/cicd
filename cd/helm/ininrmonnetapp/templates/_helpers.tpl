{{/*
Expand the name of the chart.
*/}}
{{- define "inin.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
 Define base aws repo
 */}}
{{- define "inin.baseRepo" -}}
{{- printf " 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g" }}
{{- end }}

{{/*
 Define type of pipeline
 */}}
{{- define "inin.pipeline" -}}
{{- if eq .Values.pipeline "validation" }}
{{- printf "validation" }}
{{- else if eq .Values.pipeline "certification" }}
{{- printf "certification" }}
{{- else if eq .Values.pipeline "verification" }}
{{- printf "verification" }}
{{- else }}
{{- printf "validation" }}
{{- end }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "inin.fullname" -}}
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
{{- define "inin.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "inin.labels" -}}
helm.sh/chart: {{ include "inin.chart" . }}
{{ include "inin.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "inin.selectorLabels" -}}
app.kubernetes.io/name: {{ include "inin.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "inin.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "inin.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
