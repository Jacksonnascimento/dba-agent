# Nome do executável
APP_NAME=dba-agent

# Diretório de saída
BUILD_DIR=build

.PHONY: all clean build-windows build-linux build-all run

# Default target
all: build-all

# Limpa a pasta de build
clean:
	@echo "Limpando diretório de build..."
	rm -rf $(BUILD_DIR)

# Cria o diretório de build se não existir
prepare:
	mkdir -p $(BUILD_DIR)

# Compila para Windows (amd64)
build-windows: prepare
	@echo "Compilando para Windows (amd64)..."
	GOOS=windows GOARCH=amd64 go build -ldflags="-s -w" -o $(BUILD_DIR)/$(APP_NAME).exe .

# Compila para Linux (amd64)
build-linux: prepare
	@echo "Compilando para Linux (amd64)..."
	GOOS=linux GOARCH=amd64 go build -ldflags="-s -w" -o $(BUILD_DIR)/$(APP_NAME)-linux .

# Compila para ambas as plataformas
build-all: clean build-windows build-linux
	@echo "Builds concluídos com sucesso! Binários disponíveis na pasta $(BUILD_DIR)/"

# Roda o projeto localmente
run:
	@echo "Executando o agente localmente..."
	go run .

# Instala as dependências do Go
deps:
	go mod tidy