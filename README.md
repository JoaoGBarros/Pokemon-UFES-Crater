# Pokemon-UFES-Crater

Repositório para o Trabalho 2 da disciplina de Redes de Computadores, 2025/1.

## Descrição

Este projeto é uma implementação do desafio "Servidor de Jogo Multiplayer Básico", proposto na disciplina de Redes de Computadores.

O sistema consiste em um servidor autoritativo feito em Java que gerencia o estado do jogo, e um cliente web interativo feito em React. Os jogadores podem se conectar, se ver em um mundo compartilhado, se locomover em tempo real e batalhar uns contra os outros. A comunicação em tempo real é realizada utilizando WebSockets.

O jogo foi inspirado no Pokemon Crater, um antigo jogo web de Pokemon criado por fãs que permitia multiplos jogadores explorarem o mesmo mapa e batalhar contra si. Pokemons selvagens eram encontros unicos aos jogadores, enquanto batalha pvp precisava de sincronizar ambos participantes.

## Tecnologias Utilizadas

* **Backend:** Java (21)
* **Frontend:** React (com Vite)
* **Comunicação:** WebSockets (utilizando a biblioteca `Java-WebSocket`)
* **Banco de Dados:** SQLite (para persistência de dados do jogador)
* **Containerização:** Docker & Docker Compose
* **Gerenciador de Dependências (Backend):** Maven
* **Gerenciador de Dependências (Frontend):** NPM

## Como Executar

### Requisitos

Antes de começar, certifique-se de que você tem a seguinte ferramenta instalada na sua máquina:

* [Docker](https://www.docker.com/get-started)

### Instruções de Execução

Com o Docker em execução, siga os passos abaixo para iniciar toda a aplicação.

**1. Clone o Repositório**

```bash
git clone https://github.com/JoaoGBarros/Pokemon-UFES-Crater.git
cd Pokemon-UFES-Crater
```

**2. Inicie os Contêineres**

Para iniciar todos os serviços (backend e frontend) em modo de desenvolvimento com live-reload, execute o seguinte comando na pasta raiz do projeto. A flag --build é recomendada na primeira execução para garantir que as imagens Docker sejam construídas corretamente.

```bash 
docker-compose up -d --build
```

**3. Acessando a Aplicação**

Após os contêineres estarem em execução, os serviços estarão disponíveis nos seguintes endereços:

| Serviço             | URL                    |
| :------------------ | :----------------------|
| **Frontend (UI)**   | `http://localhost:3000`|
| **Servidor do Jogo**   | `ws://localhost:8887`|

## Funcionalidades Implementadas

- [x] Estrutura do projeto com Docker para fácil configuração.

- [x] Conexão persistente entre Cliente (React) e Servidor (Java) via WebSockets.

- [x] Gerenciamento de múltiplos jogadores em um lobby/mapa compartilhado.

- [x] Sincronização de movimento dos jogadores em tempo real.

- [x] Sistema de batalha 1v1 em turnos.

- [x] Sistema de chat no jogo.

- [x] Batalha contra Pokemons Selvagens.


## Possiveis Melhorias Futuras

- Adição de NPCs (Non-Player Characters).

- Expansão do mundo com mais mapas.

- Implementação de um sistema de inventário e itens.

- Persistência de dados do jogador (posição e time de Pokémon) em um banco de dados SQLite.

- Melhorias visuais do site e do mapa.
