#!/usr/bin/env bash

set -e
printenv

echo "Configing git credentials"
git config --global user.email "${GIT_EMAIL}"
git config --global user.name "${GIT_USERNAME}"

if [ "${CIRCLE_BRANCH}" == "master" ] || [ "${CIRCLE_BRANCH}" == "release-0" ]; then
    echo going to deploy
    lein deploy clojars
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
