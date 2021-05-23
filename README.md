# `Aliucord-plugins-template`

Template for an [Aliucord](https://github.com/Aliucord) plugins repo

## Instructions

1. Generate a repo from this template, making sure to tick "Include all branches".
2. Edit `updater.json`, replacing `USERNAME` and `REPONAME` with your GitHub repo and user name.
3. Edit `ExamplePlugin/src/main/java/com/aliucord/plugins/ExamplePlugin.java`, replacing `USERNAME` and `REPONAME` with your GitHub repo and user name, `DISCORD USERNAME` and `123456789` with your Discord username and user id.
4. Edit `settings.gradle` to include your plugins
5. Clone the [Aliucord](https://github.com/Aliucord) repo to `../repo` (one directory up under the name repo)
6. Create a GitHub [PAT](https://github.com/settings/tokens) with scope `repo`
7. Add that PAT to the repo's Actions Secrets with the name `ACCESS_TOKEN`
