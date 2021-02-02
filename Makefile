version := 0.1
colon   := :

.PHONY: build
build:
	daml build -o target/know-your-customer.dar
	cd triggers && daml build -o ../target/know-your-customer-triggers.dar

.PHONY: clean
clean:
	rm -rf target
	rm -rf .daml
