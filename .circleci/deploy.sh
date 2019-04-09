#!/usr/bin/env bash

set -e

if [ "${CIRCLE_BRANCH}" == "release-t" ]; then
    echo going to deploy
    git config --global user.email "tal.vanish@gmail.com"
    git config --global user.name "wanishing"
    lein release
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
