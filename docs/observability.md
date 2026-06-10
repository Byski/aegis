# Observability

The platform is monitored with the kube-prometheus-stack (Prometheus, Grafana,
Alertmanager) plus application-level metrics from both services.

## Metrics

- `shortener-api` exposes Micrometer metrics at `/actuator/prometheus`.
- `analytics-svc` exposes Prometheus metrics at `/metrics`.
- Each service is scraped through a `ServiceMonitor` (see
  `deploy/helm/observability/templates/servicemonitors.yaml`).

## SLOs and error budget

The public redirect path is the user-facing critical path, so the SLOs target it.

| SLO            | Objective | SLI                                                        |
|----------------|-----------|------------------------------------------------------------|
| Availability   | 99.9%     | Fraction of redirect responses that are not 5xx            |
| Latency (p99)  | < 300 ms  | p99 of redirect request duration over 5m                   |

A 404 for an unknown code is an expected outcome and does not count against the
availability budget; only 5xx responses do.

Recording rules compute the 5xx error ratio over 5m, 30m, 1h, and 6h windows.
Two multiwindow, multi-burn-rate alerts watch the availability budget:

- `RedirectErrorBudgetFastBurn` (critical): 5m and 1h error ratio both exceed
  14.4x the budget. This burns a 30-day budget in roughly two days.
- `RedirectErrorBudgetSlowBurn` (warning): 30m and 6h error ratio both exceed
  6x the budget.

`RedirectHighLatencyP99` (warning) fires when p99 redirect latency exceeds the
objective for 5m. Rules live in
`deploy/helm/observability/templates/prometheusrule-slo.yaml`.

## Dashboards

The `Aegis Platform` Grafana dashboard (provisioned from a labelled ConfigMap)
shows redirect request rate by status, the 5xx error ratio against the budget
line, redirect latency percentiles, current availability, the live burn rate,
and the analytics request rate.

## Viewing locally

```bash
make up             # application stack
make observability  # kube-prometheus-stack + aegis rules and dashboard

# Grafana (admin / prom-operator)
kubectl -n monitoring port-forward svc/kube-prometheus-stack-grafana 3000:80

# Prometheus
kubectl -n monitoring port-forward svc/kube-prometheus-stack-prometheus 9090:9090
```
