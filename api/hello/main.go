package main

import (
	"github.com/I-Dont-Remember/Mul/api/db"
	"github.com/I-Dont-Remember/Mul/api/handlers"
	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

func Handler(request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	d, err := db.Connect(false)
	if err != nil {
		return events.APIGatewayProxyResponse{
			StatusCode: 507,
		}, nil
	} else {
		return handlers.Hello(request, d)
	}
}

func main() {
	lambda.Start(Handler)
}
