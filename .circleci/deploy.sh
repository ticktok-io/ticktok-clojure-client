#!/usr/bin/env bash

set -e

if [ "${CIRCLE_BRANCH}" == "release" ]; then
    echo going to deploy
    lein deploy
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
