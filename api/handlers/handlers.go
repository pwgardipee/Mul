package handlers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"

	"github.com/I-Dont-Remember/Mul/api/db"
	"github.com/aws/aws-lambda-go/events"
)

// mostly stolen from the hello world example :D
func Hello(req events.APIGatewayProxyRequest, d db.DB) (events.APIGatewayProxyResponse, error) {
	var buf bytes.Buffer

	body, err := json.Marshal(map[string]interface{}{
		"message": "Go Serverless v1.0! Your function executed super successfully!",
	})
	if err != nil {
		return events.APIGatewayProxyResponse{StatusCode: 404}, err
	}
	json.HTMLEscape(&buf, body)

	resp := events.APIGatewayProxyResponse{
		StatusCode:      200,
		IsBase64Encoded: false,
		Body:            buf.String(),
		Headers: map[string]string{
			"Content-Type":           "application/json",
			"X-MyCompany-Func-Reply": "hello-handler",
		},
	}

	return resp, nil
}

func CreateUser(req events.APIGatewayProxyRequest, d db.DB) (events.APIGatewayProxyResponse, error) {
	// pull provider id from json
	var data map[string]interface{}
	err := json.Unmarshal([]byte(req.Body), &data)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	id := data["id"].(string)

	user := db.User{
		ID: id,
	}
	err = d.CreateUser(user)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	return happy(map[string]interface{}{}), nil
}

// POST /user/<id>/mulchunk , have client and provider ids in body
func RequestMulChunk(req events.APIGatewayProxyRequest, d db.DB) (events.APIGatewayProxyResponse, error) {
	// To request chunk, we need to know client who wants it and provider it should be from
	clientID := req.PathParameters["id"]

	// pull provider id from json
	var data map[string]interface{}
	err := json.Unmarshal([]byte(req.Body), &data)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	providerID := data["provider_id"].(string)

	// check business logic of whether we will allow a new chunk
	provider, err := d.GetUser(providerID)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	// check if able to use another chunk of the providers data
	if (provider.DataProvided + db.MulChunkSizeKB) > provider.Limit {
		log.Println("Not able to user another chunk of datak")
		return serverError(), nil
	}

	log.Printf("%s - requesting chunk from %s\n", clientID, providerID)
	dataUsed, err := d.AddMulChunk(clientID, providerID)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	return happy(map[string]interface{}{
		"data_used": dataUsed,
	}), nil
}

// GET /user/<id>/
func GetUser(req events.APIGatewayProxyRequest, d db.DB) (events.APIGatewayProxyResponse, error) {
	uid := req.PathParameters["id"]

	user, err := d.GetUser(uid)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	marshalled, err := json.Marshal(user)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	return events.APIGatewayProxyResponse{
		StatusCode: 200,
		Body:       string(marshalled),
	}, nil
}

// POST /user/<id>/limit
func SetLimit(req events.APIGatewayProxyRequest, d db.DB) (events.APIGatewayProxyResponse, error) {
	uid := req.PathParameters["id"]

	var data map[string]interface{}
	err := json.Unmarshal([]byte(req.Body), &data)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	limit := int(data["limit"].(float64))
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	log.Printf("%s - setting limit %d\n", uid, limit)
	err = d.SetLimit(uid, limit)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}
	return happy(map[string]interface{}{}), nil
}

// POST /user/<id>/balance this can be used for payments by passing negative integers
func AddToBalance(req events.APIGatewayProxyRequest, d db.DB) (events.APIGatewayProxyResponse, error) {
	uid := req.PathParameters["id"]

	var data map[string]interface{}
	err := json.Unmarshal([]byte(req.Body), &data)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	fmt.Println(data)
	balance := int(data["balance"].(float64))
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	log.Printf("%s - adding to balance %d\n", uid, balance)
	err = d.AddToBalance(uid, balance)
	if err != nil {
		log.Println(err)
		return serverError(), nil
	}

	return happy(map[string]interface{}{}), nil
}

func happy(data map[string]interface{}) events.APIGatewayProxyResponse {
	body, err := json.Marshal(data)
	if err != nil {
		log.Println(err)
		return serverError()
	}
	return events.APIGatewayProxyResponse{
		StatusCode: 200,
		Body:       string(body),
		Headers: map[string]string{
			"Content-Type": "application/json",
		},
	}
}

func serverError() events.APIGatewayProxyResponse {
	return events.APIGatewayProxyResponse{
		StatusCode: 500,
	}
}
