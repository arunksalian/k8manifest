pipeline {
    agent {
        kubernetes {
            label 'maven-agent'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: docker
    image: docker:dind
    securityContext:
      privileged: true
    env:
    - name: DOCKER_TLS_CERTDIR
      value: ""

  - name: maven
    image:  arunksalian/jenkinagent:1.0.2
    command: ['cat']
    tty: true
    volumeMounts:
    - name: maven-repo
      mountPath: /root/.m2
  volumes:
  - name: maven-repo
    emptyDir: {}
"""
        }
    }
    
    environment {
        DOCKER_IMAGE = "arunksalian/cicd-1"   // ✅ Define environment variables here
        DOCKER_CREDENTIALS = "64fdbc0c-de0b-4b2d-a366-890b666e6427"
    }


    stages {
        stage('Install Git') {
            steps {
                container('maven') {
                    sh 'apt-get update && apt-get install -y git'
                }
            }
        }
        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/arunksalian/cicd-sample.git'

            }
        }
        stage('Build with Maven') {
            steps {
                container('maven') {
                    sh 'mvn clean package'
                }
            }
        }
        
        stage ('Docker Login') {
            steps {
                container('docker') {
                    script {
                        withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            sh "docker login -u $DOCKER_USER -p $DOCKER_PASS"
                        }
                    }
                }
            }
        }
        
        stage('Build and push Docker Image') {
            steps {
                container('docker') {
                    sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} ."
                    sh "docker push  ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                }
            }
        }

    }
}
