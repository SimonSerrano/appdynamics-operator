# Generated Project Skeleton

A simple operator that copies the value in a spec to a ConfigMap. 


## Integration tests

### Prerequisites

- Run minikube or similar

## Security

- Setup RBAC :
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: test-operator
  namespace: test-operator-ns
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: test-operator-role
rules:
  - apiGroups: ["apps"]
    resources: ["deployments"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["configmaps"]
    verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: test-operator-rolebinding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: test-operator-role
subjects:
  - kind: ServiceAccount
    name: test-operator
    namespace: test-operator-ns

```
