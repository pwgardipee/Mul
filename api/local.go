package main

import (
	"fmt"
	"io/ioutil"
	"strings"

	"github.com/I-Dont-Remember/Mul/api/db"
	"github.com/I-Dont-Remember/Mul/api/handlers"

	"github.com/labstack/echo"
	"github.com/labstack/echo/middleware"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
)

// Massage the echo framework request/response to match our AWS Lambda handlers
func adjust(fn func(events.APIGatewayProxyRequest, db.DB) (events.APIGatewayProxyResponse, error)) func(echo.Context) error {
	return func(c echo.Context) error {

		// TODO: validate that the string joining nonsense we're doing is actually working correctly
		headers := map[string]string{}
		for k, v := range c.Request().Header {
			headers[k] = strings.Join(v[:], ",")
		}

		paramNames := c.ParamNames()
		paramMap := map[string]string{}
		for _, name := range paramNames {
			paramMap[name] = c.Param(name)
		}

		queryMap := map[string][]string{}
		queryParams := map[string]string{}
		queryMap = c.QueryParams()
		for k, v := range queryMap {
			queryParams[k] = strings.Join(v[:], ",")
		}

		body, err := ioutil.ReadAll(c.Request().Body)
		if err != nil {
			panic(err)
		}

		request := events.APIGatewayProxyRequest{
			HTTPMethod:            c.Request().Method,
			Headers:               headers,
			PathParameters:        paramMap,
			QueryStringParameters: queryParams,
			Body: string(body),
		}

		dbClient, _ := db.Connect(true)
		proxyResponse, _ := fn(request, dbClient)

		if proxyResponse.StatusCode > 300 {
			fmt.Println("   [!] ", proxyResponse.Body)
		}

		// TODO: check this is actually doing what we thing it is
		for k, v := range proxyResponse.Headers {
			c.Response().Header().Set(k, v)
		}
		return c.JSONBlob(proxyResponse.StatusCode, []byte(proxyResponse.Body))
	}
}

func main() {
	port := ":4500"

	// try and create table when starting local API
	region := "us-east-2"
	localEndpoint := "http://localhost:4569/"
	sess, err := session.NewSession(
		&aws.Config{
			Region:   aws.String(region),
			Endpoint: aws.String(localEndpoint),
		})

	conn := dynamodb.New(sess)

	// try and create table since oftentimes it hasn't been locally
	cti := &dynamodb.CreateTableInput{
		AttributeDefinitions: []*dynamodb.AttributeDefinition{
			{
				AttributeName: aws.String("id"),
				AttributeType: aws.String("S"),
			},
		},
		KeySchema: []*dynamodb.KeySchemaElement{
			{
				AttributeName: aws.String("id"),
				KeyType:       aws.String("HASH"),
			},
		},
		ProvisionedThroughput: &dynamodb.ProvisionedThroughput{
			ReadCapacityUnits:  aws.Int64(1),
			WriteCapacityUnits: aws.Int64(1),
		},
		TableName: aws.String("MulUsers"),
	}
	_, err = conn.CreateTable(cti)
	fmt.Println(err)

	e := echo.New()

	// To see specific header, use ${header:foo} which will show foo's value
	// same for seeing cookie, query, and form
	e.Use(middleware.LoggerWithConfig(middleware.LoggerConfig{
		Format: "${method} ${uri} status:${status} latency:${latency_human} out:${bytes_out} bytes \n",
	}))

	e.Use(middleware.CORS())

	e.GET("/hello", adjust(handlers.Hello))

	e.POST("/user/", adjust(handlers.CreateUser))
	e.GET("/user/:id/", adjust(handlers.GetUser))
	e.POST("/user/:id/mulchunk", adjust(handlers.RequestMulChunk))
	e.POST("/user/:id/limit", adjust(handlers.SetLimit))
	e.POST("/user/:id/balance", adjust(handlers.AddToBalance))

	e.Logger.Fatal(e.Start(port))
}
