docker run -d --name orders-db -e POSTGRES_DB=orders -e POSTGRES_USER=orders -e POSTGRES_PASSWORD=orders -p 5432:5432 -v ./data:/var/lib/postgresql/data postgres:16
