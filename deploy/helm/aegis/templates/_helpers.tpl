{{- define "aegis.labels" -}}
app.kubernetes.io/part-of: aegis
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}
