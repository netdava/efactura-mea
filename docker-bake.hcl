# Documentation for bake file
# https://docs.docker.com/build/bake/reference/
#

variable "REPO" {
    default = "ieugen/efactura"
}

variable "TAG" {
  default = "latest"
}

target "default" {
    dockerfile = "Dockerfile"
    context = "."
    pull = true
    tags = ["${REPO}:${TAG}"]
    platforms = ["linux/amd64", "linux/arm64"]
}