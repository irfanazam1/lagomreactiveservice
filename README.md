# An example of reactive microservice using Lagom, Kafka, Akka Play HTTP server, and PostgreSQL 

This sample application presents a simple product catalog and shopping cart service built with Lagom, Kafka, Akka Play HTTP server, and PostgreSQL.  

It contains the following two services,

#### Shopping cart service:
 - It saves data to PostgreSQL database using Lagom's persistence API. It provides CRUD APIs on the shopping cart to view, add, update, and delete the products from the cart.

#### Product catalog service:
- It consumes a stream of events published to Kafka from the shopping cart service, and shows how to consume Kafka event stream work using Lagom.
- When the cart will be checked out, cart service will publish an event which product service will receive and adjust the product catalog.  
- The catalog is currently maintained in a memory store so it won't be replicated over the cluster.

## Technologies Used
- Lagom
- Lightbend
- Akka
- Play HTTP
- PostgreSQL
- Kafka
- Docker
- Java
- curl

## What is a reactive service
- https://www.reactivemanifesto.org/
- https://medium.com/@filia.aleks/microservice-performance-battle-spring-mvc-vs-webflux-80d39fd81bf0
- https://sites.google.com/a/mammatustech.com/mammatusmain/reactive-microservices

## Other reactive service frameworks
- Springboot Async
- Eclipse Vert.x

## Running the application

## Prerequisites
- Download and install Docker desktop for your version of OS.
- Install Java, either Oracle or OpenJDK, and configure the JAVA_PATH.
- Install some IDE like IntelliJ or Eclipse. It will allow you to debug or extend the code.
- Optionally install the Docker plugin to start or stop docker from the IDE.
- Download, install, and configure Maven.


### Running Docker
- Run the following docker-compose command. It will make the PostgresQL and Kafka available.
- Postgres will be available on port `5432` and Kafka on port `9092`.

```bash
cd into the project base directory
docker-compose up -d
```
### Running services 

```bash
mvn lagom:runAll
```

## Catalog service API

The catalog service offers two REST endpoints:

* Get the inventory of an item:

```bash
curl http://localhost:9000/catalog/123
```

* Add product quantity to a catalog: The following command is adding 4 items to product id 123

```bash
curl -H "Content-Type: application/json" -d 4 -X POST http://localhost:9000/calalog/123
curl -H "Content-Type: application/json" -d 6 -X POST http://localhost:9000/calalog/456
```

Note: Before using the cart APIs do call the add items API so that catalog service can decrement the item quantities upon kafka event when the cart is checked out.

## Shopping cart service

The shopping cart service offers four REST endpoints:

* Get the current contents of the shopping cart:

```bash
curl http://localhost:9000/cart/1
```

* Get a report of the shopping cart creation and checkout dates:

```bash
curl http://localhost:9000/cart/1/report
```

* Add an item in the shopping cart:

```bash
curl -H "Content-Type: application/json" -d '{"itemId": "123", "quantity": 2}' -X POST http://localhost:9000/cart/1
curl -H "Content-Type: application/json" -d '{"itemId": "456", "quantity": 1}' -X POST http://localhost:9000/cart/1
```

* Remove an item in the shopping cart:

```bash
curl -X DELETE http://localhost:9000/cart/1/item/123
```

* Adjust an item's quantity in the shopping cart:

```bash
curl -H "Content-Type: application/json" -X PATCH -d '{"quantity": 2}' http://localhost:9000/cart/1/item/123
```

* Check out the shopping cart (i.e., complete the transaction)

```bash
curl -X POST http://localhost:9000/cart/1/checkout
```

When the shopping cart is checked out, an event is published to the Kafka topic called `shopping-cart` by the shopping cart service. Such events look like this:

```json
{
  "id": "1",
  "items": [
    {"itemId": "123", "quantity": 2},
    {"itemId": "456", "quantity": 1}
  ],
  "checkedOut": true
}
```

The catalog service will decrement the item quantities which can be verified by calling the following APIs
```bash
curl http://localhost:9000/catalog/123
curl http://localhost:9000/catalog/456
```
