#!/usr/bin/env bash

set -e

if [ "${CIRCLE_BRANCH}" == "master" ] || [ "${CIRCLE_BRANCH}" == realease-* ]; then
    lein release-tasks
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
