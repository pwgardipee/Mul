# Mul API

To make life quick and easy (and cheap :D) we are using a serverless API leveraging the [Serverless Framework.](https://serverless.com/) As a learning opportunity, we are using Golang as the language of choice for functions.

## Development

The Serverless cli tool is only necesary for deployment of the API (it is useful in other ways, but only install it if you feel like digging into it for fun). By abstracting the Lambda handlers, we can use both tests and a local framework for development.
Echo is the local framework, it's overkill for what we need but was dead simple to make the local API shim.  

Once we start using a database, for local development we can use [Localstack](https://github.com/localstack/localstack). This lets you use `docker compose` to spin up an "AWS" instance on your machine with databases and junk.  I'd recommend cloning the repo and running `docker compose up` when you want to use it, but I think there are other options if you are so inclined.

Once everything is installed, to develop on the API start up Localstack, then `go run local.go`. Currently changes will not be reflected unless you restart the `local.go` server.

If you would like it to auto-restart, there is a tool called `nodemon`([link](https://nodemon.io/)) that can be used to auto-restart functions.  I put a config file for it in `api/`, so it should work correctly by running `nodemon local.go` if you installed it globally, it is also possible to install and it on a per repo basis if we add a `package.json`.

To add a new endpoint, create the handler in the `handlers` package, add a new directory for it for the lambda build process (see `hello/` for reference), and add the necessary line in the Makefile to build it.

### Organization

There might be a simpler way, but the approach taken has a library of the API handlers that can be imported by `local.go` and the special function that gets built and sent up to AWS.  The idea was to abstract the business logic from the AWS junk, as well as leaving the option of a local API open.

* Note: When creating a new function, don't forget to add the relevant build line in the Makefile.

## Go Primer

There are a variety of ways to install and setup Go, especially now that they've changed the way the special $GOPATH variable you'll see mentioned works.  This change happened with their method of importing libraries at version 1.11, after that support for modules came out. I personally use [goenv](https://github.com/syndbg/goenv) because it seemed less annoying than my previous setup, but feel free to go with whatever works.  Mac's probably have some cool `brew` stuff.

We are using go modules in this repo, I'm still learning them so don't have much to say but figured it would be a good note to add.