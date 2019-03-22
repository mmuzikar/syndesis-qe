#!/bin/bash
BASE_DIR=$(dirname "$(readlink -f "$0")")

# Variables used:
#SERVER          - server image
#META            - meta image
#UI              - ui image
#S2I             - s2i image
#OPERATOR        - operator
#TAG             - tag which is expected by the operator
#OAUTH_PROXY     - oauth proxy image, defaults to "registry.access.redhat.com/openshift3/oauth-proxy:v3.10.45"
#OAUTH_PROXY_TAG - oauth proxy image stream tag, defaults to "v1.1.0"
#PROMETHEUS      - prometheus image, defaults to "registry.access.redhat.com/openshift3/prometheus:v3.9.25"
#PROMETHEUS_TAG  - prometheus image stream tag, defaults to "v2.1.0"

# Generated template is located in /tmp/prod.yml and the content it printed when the script is done

# Script can be also used as ./generate.sh | xclip -selection c
# to copy the template to clipboard

VARS="SERVER META UI S2I OPERATOR OAUTH_PROXY PROMETHEUS TAG OAUTH_PROXY_TAG PROMETHEUS_TAG"

# If the properties are defined, don't source
[[ ! "z${OPERATOR}" == "z" ]] || source "${BASE_DIR}"/vars

cp -f "${BASE_DIR}"/prod.template.yml /tmp/prod-resources.yml

for var in ${VARS}; do
	case "$var" in
		"PROMETHEUS"|"OAUTH_PROXY"|*"TAG")
			REPLACEMENT="${!var}";;
		*)
			REPLACEMENT="${REGISTRY}/${REGISTRY_NAMESPACE}/${!var}";;
	esac
	sed -i "s#\\\$$var\\\$#${REPLACEMENT}#g" /tmp/prod-resources.yml
done

cat /tmp/prod-resources.yml
