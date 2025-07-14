def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch = config.gitBranch ?: 'master'  

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            git checkout ${gitBranch}
            git pull origin ${gitBranch}

            echo "Updating image tag in deployment..."
            sed -i "s|image: darshanm416/easyshop-app:.*|image: darshanm416/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
              sed -i "s|image: darshanm416/easyshop-migration:.*|image: darshanm416/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
              sed -i "s|host: .*|host: easyshop.darshanm.space|g" ${manifestsPath}/10-ingress.yaml
            fi

            if git diff --quiet; then
              echo "No changes detected in manifests."
            else
              git add ${manifestsPath}/*.yaml
              git commit -m "Update image tags to ${imageTag} and domain [ci skip]"
              git remote set-url origin https://\$GIT_USERNAME:\$GIT_PASSWORD@github.com/Darshanm416/tws-e-commerce-app_hackathon.git
              git push origin ${gitBranch}
              echo "Changes committed and pushed to GitHub."
            fi
        """
    }
}
