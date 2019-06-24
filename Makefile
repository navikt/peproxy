DOCKER  := docker
VERSION := $(shell cat ./VERSION)
REGISTRY:= repo.adeo.no:5443

.PHONY: all build test docker docker-push bump-version release deploy-prod deploy-preprod deploy-apps set-preprod set-prod

all: build test docker
release: tag docker-push

build:
	$(DOCKER) run --rm -t \
		-v ${PWD}:/usr/src \
		-w /usr/src \
		-u $(shell id -u) \
		-v ${HOME}/.m2:/var/maven/.m2 \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		maven:3.5-jdk-11 mvn -Duser.home=/var/maven clean package -DskipTests=true -B -V

test:
	$(DOCKER) run --rm -t \
		-v ${PWD}:/usr/src \
		-w /usr/src \
		-u $(shell id -u) \
		-v ${HOME}/.m2:/var/maven/.m2 \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		maven:3.5-jdk-11 mvn -Duser.home=/var/maven verify -B -e

docker:
	$(DOCKER) build --pull -t $(REGISTRY)/peproxy -t $(REGISTRY)/peproxy:$(VERSION) .

docker-push:
	$(DOCKER) push $(REGISTRY)/peproxy:$(VERSION)

bump-version:
	@echo $$(($$(cat ./VERSION) + 1)) > ./VERSION

tag:
	git add VERSION
	git commit -m "Bump version to $(VERSION) [skip ci]"
	git tag -a $(VERSION) -m "auto-tag from Makefile"

deploy-preprod: DEPLOY_ENV=preprod
deploy-preprod: deploy-apps
deploy-prod: DEPLOY_ENV=prod
deploy-prod: deploy-apps

deploy-apps:
	kubectl config use-context $(DEPLOY_ENV)-fss
	sed 's/{{version}}/$(VERSION)/' nais.yaml | sed 's/{{env}}/$(DEPLOY_ENV)/' | kubectl apply -f -
	kubectl rollout status -w deployment/peproxy
