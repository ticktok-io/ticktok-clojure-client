#!/usr/bin/env bash

set -e
echo "Git branch: ${CIRCLE_BRANCH}"
echo "Creating lein profiles.clj"
echo "{:user {}
 :auth {:repository-auth {#"clojars" {:username #=(eval (System/getenv "CLOJARS_USERNAME"))
                                      :password  #=(eval (System/getenv "CLOJARS_PASSWORD"))}}}}" > ~/.lein/profiles.clj
echo "Verifying file created.."
cat ~/.lein/profiles.clj

echo "Configing git credentials"
git config --global user.email "${GIT_EMAIL}"
git config --global user.name "${GIT_USERNAME}"

if [ "${CIRCLE_BRANCH}" == "master" ] || [ "${CIRCLE_BRANCH}" == "release-*" ]; then
    echo going to deploy
    lein deploy
    echo deployed successfully
else
    echo No publishing from $CIRCLE_BRANCH branch
fi
