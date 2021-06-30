# rest-api-core

This library contains **all** the business logic of the Rest API microservice which is to be generated with `service-generation-utility`. The Rest API microservice is to accept REST requests, process them and either work with DB or send events to Kafka for `Kafka API` microservice for further processing.

# Related components
* `model-core`
* Kafka
* DB

# Usage
1. retrieve operations (GET)
   * `GenericQueryService` / `GenericSearchService` to be sub-classed as a Service-layer
   * `AbstractQueryHandler` / `AbstractSearchHandler` to be sub-classed as a DB-layer
2. modifying operations (POST, PUT, PATCH, DELETE)
   * `GenericService` to be sub-classed as a Service-layer
3. Rest Controllers - to be created for accepting REST requests, marshalling and calling the mentioned Services

# Deployment
The library is delivered as a docker image with all dependencies inside.

### License
rest-api-core is Open Source software released under the Apache 2.0 license.