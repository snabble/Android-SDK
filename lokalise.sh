#!/usr/bin/env zsh
# 
# Lokalise CLI v2 can be found here: https://github.com/lokalise/lokalise-cli-2-go
#
# Setting the environment variable LOKALISE_ACCESS_TOKEN is necessary to execute this script.

function lokalise {
    lokalise2 \
        --token $LOKALISE_ACCESS_TOKEN \
        --project-id $1 \
        file download \
        --format xml \
        --unzip-to $2 \
        --export-empty-as skip \
        --include-description=false \
        $@
}

# ui
lokalise 3931709465f04f20a1bc18.55914019 ui/src/main/res --exclude-tags Onboarding,Shop,Scanner

# ui-toolkit
lokalise 3931709465f04f20a1bc18.55914019 ui-toolkit/src/main/res --include-tags Onboarding,Shop,Scanner

# sample app
lokalise 8964099365f434ac71f546.06213099 kotlin-sample/src/main/res
