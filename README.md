Search through PR comments for configuration for the current build. 
For example, if your current PR (frontend repo) needs a specific version of the backend repo for e2e testing:

Add `backend: my-special-branch` in a PR comment. 

Then in your Jenkinsfile:
```
def branch = searchPR query: /.*`backend:\s([^\s]*)`.*/, default: 'master', credentialsId: '34f60eac-db1d-4d89-84a7-1fed98bc78c7'
```

`credentialsId` is your github token, make sure you have the right permissions. 



