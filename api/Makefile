.PHONY: build clean deploy

build: 
	export GO111MODULE=on
	env GOOS=linux go build -ldflags="-s -w" -o bin/hello hello/main.go
	env GOOS=linux go build -ldflags="-s -w" -o bin/getUser getUser/main.go
	env GOOS=linux go build -ldflags="-s -w" -o bin/createUser createUser/main.go
	env GOOS=linux go build -ldflags="-s -w" -o bin/requestMulChunk requestMulChunk/main.go
	env GOOS=linux go build -ldflags="-s -w" -o bin/setLimit setLimit/main.go
	env GOOS=linux go build -ldflags="-s -w" -o bin/AddToBalance AddToBalance/main.go

clean:
	rm -rf ./bin ./vendor Gopkg.lock

deploy: clean build
	sls deploy --verbose

