Sistema de Gestão de Cardápios - Trabalho Prático de AEDs III

Este projeto é o desenvolvimento do trabalho prático da disciplina de Algoritmos e Estruturas de Dados III, do curso de Ciência da Computação da PUC Minas. O objetivo é construir um sistema de gestão de "banco de dados" do zero, utilizando Java, com persistência de dados em ficheiros binários e a implementação de estruturas de indexação avançadas.

👥 Componentes do Grupo

Bruna de Paula Anselmi

David Nunes Ribeiro

Lucca Rafael Costa Resende

Mateus Caldeira Brant Campos Gomes

🚀 Sobre o Projeto

O sistema permite que pequenos estabelecimentos criem e administrem os seus próprios cardápios digitais. Todas as operações de CRUD (Create, Read, Update, Delete) são geridas por uma camada de acesso a dados (DAO) que interage diretamente com ficheiros binários, simulando o funcionamento de um SGBD.

As principais características técnicas implementadas nesta fase são:

Persistência em Ficheiros Binários: Todos os dados são guardados em ficheiros .db com controlo de registos de tamanho variável.

Exclusão Lógica: Utilização de "lápides" (tombstones) para marcar registos como excluídos sem os remover fisicamente.

Índice de Chave Primária: Implementação de um Hash Extensível para buscas por ID com performance de O(1).

Índice de Chave Secundária: Implementação de uma Árvore B+ para permitir buscas e listagens ordenadas por chaves secundárias (como o nome).

Interface Interativa: Um menu de consola permite ao utilizador interagir com o sistema e realizar todas as operações de CRUD.

🏛️ Arquitetura

O projeto segue o padrão arquitetural MVC (Model-View-Controller) + DAO (Data Access Object), com os pacotes organizados da seguinte forma:

app: Camada de View e Controller, responsável pela interface de consola e por orquestrar as operações.

model: Camada Model, contendo as classes de domínio (Empresa, Produto, etc.).

dao: Camada de acesso a dados, que encapsula toda a lógica de manipulação dos ficheiros binários.

indices: Pacote contendo as implementações das estruturas de dados de indexação (ExtensibleHash e BPlusTree).

🛠️ Pré-requisitos

Para compilar e executar este projeto, você precisará de:

Java JDK 17 (ou superior)

Apache Maven (geralmente já integrado nas IDEs modernas)

IntelliJ IDEA (recomendado) ou outra IDE Java de sua preferência.

⚙️ Como Compilar e Executar

Siga os passos abaixo para executar o projeto na sua máquina local:

Clone o repositório:

git clone https://github.com/Ddavidi/sistema-cardapio-aeds3-pucminas.git
