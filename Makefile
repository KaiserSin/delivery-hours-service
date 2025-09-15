.PHONY: start
start:
	./gradlew run

.PHONY: start-dependencies
start-dependencies:
	./gradlew composeUp

.PHONY: stop
stop:
	./gradlew composeDownForced
	./gradlew --stop

.PHONY: restart
restart: stop start

.PHONY: test
test:
	./gradlew test

.PHONY: lint
lint:
	./gradlew ktlintCheck

.PHONY: format
format:
	./gradlew ktlintFormat
