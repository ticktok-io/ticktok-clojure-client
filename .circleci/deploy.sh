#!/usr/bin/env bash

set -e

echo "Configing git credentials"
git config --global user.email "${GIT_EMAIL}"
git config --global user.name "${GIT_USERNAME}"

if [ "${CIRCLE_BRANCH}" == "master" ]; then
    echo going to deploy
    lein release
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
