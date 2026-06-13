# Deploying hermetrics to Kubernetes / OpenShift

Three images — the Flink job, the control-plane API, and the UI — run by the Helm
chart in `deploy/helm/hermetrics`: Flink JobManager (Application Mode) +
TaskManagers, the API, and the UI, with the UI exposed via Ingress (k8s) or Route
(OpenShift). The platform is a single value (`platform: kubernetes|openshift`).

## 1. Build & push the images

```bash
REG=quay.io/yourorg                                   # your registry (with org)
docker build -t $REG/hermetrics-flink:latest -f Dockerfile     .   # the Flink job
docker build -t $REG/hermetrics-api:latest   -f Dockerfile.api .   # the API
docker build -t $REG/hermetrics-ui:latest    ui                    # the UI (context = ui/)
docker push $REG/hermetrics-flink:latest
docker push $REG/hermetrics-api:latest
docker push $REG/hermetrics-ui:latest
```

## 2. Provide the configs

`jobConfig` and `apiConfig` (chart values) are rendered into **Secrets** — they may
hold Kafka SASL credentials. Start from `job-config.example.json` and
`api-config.example.json`. Keep them out of your values file with `--set-file`.

## 3a. Kubernetes

```bash
helm install hermetrics deploy/helm/hermetrics \
  --set image.registry=$REG/ \
  --set expose.host=hermetrics.example.com \
  --set-file jobConfig=job-config.json \
  --set-file apiConfig=api-config.json
```

## 3b. OpenShift

```bash
helm install hermetrics deploy/helm/hermetrics \
  -f deploy/helm/hermetrics/values-openshift.yaml \
  --set image.registry=$REG/ \
  --set expose.host=hermetrics.apps.cluster.example.com \
  --set-file jobConfig=job-config.json \
  --set-file apiConfig=api-config.json
```

OpenShift runs every container as a random non-root uid in group 0. The API and
UI images are built for that (no fixed user, group/world-readable). The **Flink**
image must also tolerate an arbitrary uid — either make it group-0-writable in
your Flink Dockerfile:

```dockerfile
RUN chmod -R g+rwX /opt/flink
```

or grant its service account the `anyuid` SCC:

```bash
oc adm policy add-scc-to-user anyuid -z default -n <namespace>
```

## What you get

- **UI** at the Route/Ingress host. The Config tab edits the comparison config and
  **Apply** publishes it to the control topic — the running job hot-swaps with no
  restart. The Dashboard shows parity from the results/rollups topics.
- **Flink dashboard** via port-forward (or set `flink.exposeUi.enabled=true` with a
  host): `kubectl port-forward svc/hermetrics-jobmanager 8081:8081`.

Useful values: `image.*` (registry/repos/tags/pullSecrets), `flink.taskmanager.replicas`,
`flink.properties` (appended to flink-conf, e.g. `state.backend: rocksdb`),
`*.resources`, `expose.{host,tls,ingressClassName}`. The chart is verified with
`helm lint` and renders cleanly for both platforms.
