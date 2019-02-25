#!/usr/bin/env bash

set -e

if [ "${CIRCLE_BRANCH}" == "release" ]; then
    echo going to deploy
    lein release
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
