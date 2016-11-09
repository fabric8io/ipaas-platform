#!/usr/bin/groovy
def updateDependencies(source){

  def properties = []

  properties << ['<fabric8.version>','io/fabric8/kubernetes-api']

  properties << ['<fabric8.devops.version>','io/fabric8/devops/apps/jenkins']
  properties << ['<fabric8.forge.version>','io/fabric8/forge/apps/fabric8-forge']

  updatePropertyVersion{
    updates = properties
    repository = source
    project = 'fabric8io/ipaas-platform'
  }
}

def stage(){
  return stageProject{
    project = 'fabric8io/ipaas-platform'
    useGitTagForNextVersion = true
  }
}

def approveRelease(project){
  def releaseVersion = project[1]
  approve{
    room = null
    version = releaseVersion
    console = null
    environment = 'fabric8'
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.ipaas.platform.distro'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'distro'
    artifactExtensionToWatchInCentral = 'pom'
    promoteToDockerRegistry = 'docker.io'
    dockerOrganisation = 'fabric8'
    imagesToPromoteToDockerHub = []
    extraImagesToTag = null
  }
}

def mergePullRequest(prId){
  mergeAndWaitForPullRequest{
    project = 'fabric8io/ipaas-platform'
    pullRequestId = prId
  }

}

def approve(project){
  def releaseVersion = project[1]

  def stagedPlatform = "https://oss.sonatype.org/content/repositories/staging/io/fabric8/ipaas/platform/packages/ipaas-platform/${releaseVersion}/ipaas-platform-${releaseVersion}-openshift.yml"

  def proceedMessage = """
  The ipaas-platform is available for QA.  Please review and approve.

  minishift

  gofabric8 start --ipaas --package=${stagedPlatform}

  remote

  gofabric8 deploy --package=${stagedPlatform}

  Approve release?
  """

  hubotApprove message: proceedMessage, room: 'release'
  def id = approveRequestedEvent(app: "${env.JOB_NAME}", environment: 'community')

  try {
    input id: 'Proceed', message: "\n${proceedMessage}"
  } catch (err) {
    approveReceivedEvent(id: id, approved: false)
    throw err
  }
  approveReceivedEvent(id: id, approved: true)
}

return this;
