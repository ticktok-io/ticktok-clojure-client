#!/usr/bin/env bash

set -e
echo "Creating lein profiles.clj"
echo "{:user {}
 :auth {:repository-auth {#"clojars" {:username #=(eval (System/getenv "CLOJARS_USERNAME"))
                                      :password  #=(eval (System/getenv "CLOJARS_PASSWORD"))}}}}" > ~/.lein/profiles.clj
echo "Config git credentials"
git config --global user.email "${GIT_EMAIL}"
git config --global user.name "${GIT_USERNAME}"

if [ "${CIRCLE_BRANCH}" == "master" ] || [ "${CIRCLE_BRANCH}" == release-* ]; then
    echo going to deploy
    lein deploy
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
