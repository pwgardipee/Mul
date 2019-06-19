package db

import (
	"errors"
	"fmt"
	"strconv"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
	"github.com/aws/aws-sdk-go/service/dynamodb/dynamodbattribute"
)

var (
	MulChunkSizeKB = 20
)

// trying to use dependency injection, it worked for my API but seems flawed in that if your interface has a lot of methods,
// then anything you want to inherit from has to implement every one of those even if you only need one.  Maybe I did it wrong
// and am supposed to do something where interface only has some connection stuff and just gets a struct reference that can
// have whatever methods attached.  IDK.
type DB interface {
	AddMulChunk(string, string) (int, error)
	updateNumField(string, string, string, int) (int, error)
	CreateUser(User) error
	GetUser(string) (User, error)
	SetLimit(string, int) error
	AddToBalance(string, int) error
}

// Dynamo implements DB
type Dynamo struct {
	conn  *dynamodb.DynamoDB
	Table string
}

// User is json helper for getting info from DB
type User struct {
	ID           string `json:"id"`
	Limit        int    `json:"limit"`
	CentsBalance int    `json:"cents_balance"`
	DataUsed     int    `json:"data_used"`
	DataProvided int    `json:"data_provided"`
}

// Connect returns a DynamoDB connection; local or remote
func Connect(isLocal bool) (DB, error) {
	region := "us-east-1"
	localEndpoint := "http://localhost:4569/"
	// env := os.Getenv("API_ENV")

	// if env != "local" && env != "prod" && env != "dev" {
	// 	// TODO: probably trying to run a test, we should probably pull in and clean up code for passing Mockdb to tests
	// 	return Dynamo{}, nil
	// }

	d := &Dynamo{}
	d.Table = "MulUsers"

	sess, err := session.NewSession(&aws.Config{Region: aws.String(region)})
	if isLocal {
		sess, err = session.NewSession(
			&aws.Config{
				Region:   aws.String(region),
				Endpoint: aws.String(localEndpoint),
			})
	}

	if err != nil {
		return nil, err
	}

	d.conn = dynamodb.New(sess)
	return d, nil
}

// addMulChunk returns the updated data used value for client side
func (db Dynamo) AddMulChunk(clientID string, providerID string) (int, error) {
	err := db.checkUser(clientID, "addMulChunk")
	if err != nil {
		return -1, err
	}

	// attempt to update client and provider with new data chunk used
	_, err = db.updateNumField("#PROVIDED", "data_provided", providerID, MulChunkSizeKB*1024)
	if err != nil {
		return -1, err
	}
	dataUsed, err := db.updateNumField("#USED", "data_used", clientID, MulChunkSizeKB*1024)
	if err != nil {
		return -1, err
	}
	return dataUsed, nil
}

func (db Dynamo) updateNumField(attributeName string, jsonName string, id string, amount int) (int, error) {
	ui := &dynamodb.UpdateItemInput{
		ExpressionAttributeNames: map[string]*string{
			attributeName: aws.String(jsonName),
		},
		ExpressionAttributeValues: map[string]*dynamodb.AttributeValue{
			":val": {
				N: aws.String(strconv.Itoa(amount)),
			},
		},
		Key: map[string]*dynamodb.AttributeValue{
			"id": {
				S: aws.String(id),
			},
		},
		TableName:        aws.String(db.Table),
		UpdateExpression: aws.String(fmt.Sprintf("SET %s = %s + :val", attributeName, attributeName)),
		ReturnValues:     aws.String("UPDATED_NEW"),
	}

	result, err := db.conn.UpdateItem(ui)
	if err != nil {
		return 0, err
	}

	if attr, ok := result.Attributes[jsonName]; ok {
		// now convert the aws string value to integer
		return strconv.Atoi(*attr.N)
	}

	return -1, errors.New("Couldn't get attribute")
}

func (db Dynamo) CreateUser(u User) error {
	av, err := dynamodbattribute.MarshalMap(u)
	if err != nil {
		return err
	}

	pi := &dynamodb.PutItemInput{
		Item:      av,
		TableName: aws.String(db.Table),
	}

	_, err = db.conn.PutItem(pi)
	return err
}

// for non-existent users, this returns a User object with nil-type for each element (0,"", etc.)
func (db Dynamo) GetUser(id string) (User, error) {
	u := User{}

	gi := &dynamodb.GetItemInput{
		Key: map[string]*dynamodb.AttributeValue{
			"id": {
				S: aws.String(id),
			},
		},
		TableName: aws.String(db.Table),
	}
	result, err := db.conn.GetItem(gi)
	if err != nil {
		return u, err
	}

	err = dynamodbattribute.UnmarshalMap(result.Item, &u)
	return u, err
}

func (db Dynamo) SetLimit(id string, limit int) error {
	err := db.checkUser(id, "setLimit")
	if err != nil {
		return err
	}

	ui := &dynamodb.UpdateItemInput{
		ExpressionAttributeNames: map[string]*string{
			"#LIMIT": aws.String("limit"),
		},
		ExpressionAttributeValues: map[string]*dynamodb.AttributeValue{
			":l": {
				N: aws.String(strconv.Itoa(limit)),
			},
		},
		Key: map[string]*dynamodb.AttributeValue{
			"id": {
				S: aws.String(id),
			},
		},
		TableName:        aws.String(db.Table),
		UpdateExpression: aws.String("set #LIMIT = :l"),
	}

	_, err = db.conn.UpdateItem(ui)
	return err
}

func (db Dynamo) AddToBalance(id string, addition int) error {
	err := db.checkUser(id, "AddToBalance")
	if err != nil {
		return err
	}

	// set balance, but we really should add it to existing balance
	// ui := &dynamodb.UpdateItemInput{
	// 	ExpressionAttributeValues: map[string]*dynamodb.AttributeValue{
	// 		":v": {
	// 			N: aws.String(strconv.Itoa(balance)),
	// 		},
	// 	},
	// 	Key: map[string]*dynamodb.AttributeValue{
	// 		"id": {
	// 			S: aws.String(id),
	// 		},
	// 	},
	// 	TableName:        aws.String(db.Table),
	// 	UpdateExpression: aws.String("set cents_balance = :v"),
	// }

	// _, err = db.conn.UpdateItem(ui)
	
	_, err = db.updateNumField("#BALANCE", "cents_balance", id, addition)
	return err
}

// private method but still wanted access to to db struct
func (db Dynamo) checkUser(id string, method string) error {
	u, err := db.GetUser(id)
	if err != nil {
		fmt.Println(err)
		return errors.New("failed checking user for " + method)
	}
	if u.ID == "" {
		u.ID = id
		err = db.CreateUser(u)
		if err != nil {
			return errors.New("failed creating user for " + method)
		}	
	}
	return nil
}