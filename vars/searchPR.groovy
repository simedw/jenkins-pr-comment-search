
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.regex.Pattern

import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import hudson.util.Secret
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

Secret getStringCredential(String id) {
    Secret secret = SystemCredentialsProvider.getInstance().credentials.find {
        it in StringCredentialsImpl && it.id == id
    }?.secret
    if(!secret) {
        throw new RuntimeException("Could not find a String Credential with the id '${id}'")
    }
    secret
}


@NonCPS
String getPRText(gh_token, pr_number, repository) {
	String api_endpoint = "https://api.github.com/repos/${repository}/pulls/${pr_number}"
	Reader reader = new URL(api_endpoint).newReader(requestProperties: ['Authorization': "token ${gh_token}".toString(), 'Accept': 'application/vnd.github.v3+json'])
	return new JsonSlurper().parse(reader).body
}

@NonCPS
String[] getPRComments(gh_token, pr_number, repository) {
	String api_endpoint = "https://api.github.com/repos/${repository}/issues/${pr_number}/comments"
	Reader reader = new URL(api_endpoint).newReader(requestProperties: ['Authorization': "token ${gh_token}".toString(), 'Accept': 'application/vnd.github.v3+json'])
	return (new JsonSlurper().parse(reader).collect { it.body })
}

@NonCPS
String extractQueryFromComments(List<String> comments, default_value, query) {
	String current = default_value
	comments.each {
        results = (it =~ query).findAll()
        if (results) {
            current = results[-1][1]
		}
	}
	return current
}

@NonCPS
String getRepository() {
    matches = (env.CHANGE_URL =~ /.*github.com\/(.*)\/pull\/.*/).findAll()
    if(matches) {
        return matches[0][1]
    }
    return null
}

def call(Map config) {
    assert config.default
    def pr_number = config.pr_number ?: env.CHANGE_ID
    if (pr_number == null) {
        println("No PR number, returning default value")
        return config.default
    }
    def repository = config.repository ?: getRepository()
    if (repository == null) {
        assert false: "couldn't determine the repository"
    }
    assert config.credentialsId
    assert config.query

    String gh_token = getStringCredential(config.credentialsId)
    def comments = []
    comments.add(getPRText(gh_token, pr_number, repository))
    comments.addAll(getPRComments(gh_token, pr_number, repository))
    return extractQueryFromComments(comments, config.default, config.query)
}
