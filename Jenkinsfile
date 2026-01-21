pipeline {
    agent any

    environment {
        SONAR_TOKEN = credentials('sonar-new-cred')

        AWS_REGION     = 'us-east-1'
        AWS_ACCOUNT_ID = '123456789012'
        ECR_REPO       = 'saas-app'
        EKS_CLUSTER    = 'saas-eks-cluster'

        IMAGE_NAME = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
        IMAGE_TAG  = "${BUILD_NUMBER}"
    }

    stages {

        stage('Git Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/fasilcloud/saas-app-deployment.git'
            }
        }

        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh """
                      mvn clean verify sonar:sonar \
                      -Dsonar.projectKey=saas-app \
                      -Dsonar.projectName=saas-app \
                      -Dsonar.host.url=http://localhost:9000 \
                      -Dsonar.token=${SONAR_TOKEN}
                    """
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                  docker build -t ${ECR_REPO} .
                  docker tag ${ECR_REPO} ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('ECR Login & Push') {
            steps {
                sh """
                  aws ecr get-login-password --region ${AWS_REGION} \
                  | docker login --username AWS --password-stdin \
                  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

                  docker push ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('Trivy Image Scan') {
            steps {
                sh """
                  trivy image --exit-code 1 --severity HIGH,CRITICAL \
                  ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('Manual Approval for Deployment') {
            steps {
                script {
                    def decision = input(
                        message: 'Deploy saas-app to EKS?',
                        parameters: [
                            choice(
                                name: 'DEPLOY',
                                choices: ['Allow', 'Deny'],
                                description: 'Allow or Deny deployment'
                            )
                        ]
                    )

                    if (decision == 'Deny') {
                        error "Deployment denied by user"
                    }
                }
            }
        }

        stage('Update Kubernetes Manifest') {
            steps {
                dir('k8s-manifest') {
                    sh """
                      sed -i 's/replaceImageTag/${IMAGE_TAG}/g' deployment.yaml
                      cat deployment.yaml
                    """
                }
            }
        }

        stage('Deploy to EKS') {
            steps {
                script {
                    try {
                        sh """
                          aws eks update-kubeconfig \
                          --region ${AWS_REGION} \
                          --name ${EKS_CLUSTER}

                          kubectl apply -f k8s-manifest
                          kubectl rollout status deployment/saas-app --timeout=120s
                        """
                    } catch (err) {
                        echo "Deployment failed. Rolling back..."
                        sh "kubectl rollout undo deployment/saas-app"
                        error "Deployment failed and rollback executed"
                    }
                }
            }
        }
    }
}


