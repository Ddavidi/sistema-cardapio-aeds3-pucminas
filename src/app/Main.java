package app;

import dao.DAO;
import model.Categoria;
import model.Produto;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Apagar ficheiros de base de dados e de ÍNDICES antigos para um teste limpo
            new File("produtos.db").delete();
            new File("produtos.hash.dir").delete();
            new File("produtos.hash.bkt").delete();
            new File("produtos.bptree.idx").delete();

            new File("categorias.db").delete();
            new File("categorias.hash.dir").delete();
            new File("categorias.hash.bkt").delete();
            new File("categorias.bptree.idx").delete();

            // Habilitar a Árvore B+ para o DAO de produtos
            DAO<Produto> produtoDAO = new DAO<>("produtos.db", Produto.class, true);
            DAO<Categoria> categoriaDAO = new DAO<>("categorias.db", Categoria.class, false);

            // --- INÍCIO DOS TESTES ---
            System.out.println("--- 1. CRIAÇÃO DE CATEGORIAS ---");
            Categoria cBebidas = new Categoria("Bebidas");
            Categoria cLanches = new Categoria("Lanches");
            int idBebidas = categoriaDAO.create(cBebidas);
            int idLanches = categoriaDAO.create(cLanches);
            System.out.println("Categoria 'Bebidas' criada com ID: " + idBebidas);
            System.out.println("Categoria 'Lanches' criada com ID: " + idLanches);

            System.out.println("\n--- 2. CRIAÇÃO DE PRODUTOS (Relacionamento 1:N) ---");
            Produto pRefri = new Produto("Refrigerante", "Lata 350ml", 5.0f, idBebidas);
            Produto pXBurger = new Produto("X-Burger", "Pão, bife, queijo e presunto", 15.0f, idLanches);
            Produto pSuco = new Produto("Suco Natural", "Laranja 500ml", 8.0f, idBebidas);
            Produto pAgua = new Produto("Água Mineral", "Garrafa 500ml", 3.0f, idBebidas);
            int idRefri = produtoDAO.create(pRefri);
            int idXBurger = produtoDAO.create(pXBurger);
            int idSuco = produtoDAO.create(pSuco);
            int idAgua = produtoDAO.create(pAgua);
            System.out.println("Produto 'Refrigerante' criado com ID: " + idRefri);
            System.out.println("Produto 'X-Burger' criado com ID: " + idXBurger);
            System.out.println("Produto 'Suco Natural' criado com ID: " + idSuco);
            System.out.println("Produto 'Água Mineral' criado com ID: " + idAgua);

            System.out.println("\n--- 3. LEITURA (READ por ID com Hash) ---");
            Produto produtoBuscado = produtoDAO.read(idXBurger);
            System.out.println("Busca pelo ID " + idXBurger + ": " + produtoBuscado);

            System.out.println("\n--- 4. LISTAGEM ORDENADA POR NOME (com B+ Tree) ---");
            List<Produto> produtosOrdenados = produtoDAO.listAllSortedBySecondaryKey();
            System.out.println("Todos os produtos cadastrados, em ordem alfabética:");
            for (Produto p : produtosOrdenados) {
                System.out.println(p);
            }

            System.out.println("\n--- 5. EXCLUSÃO (DELETE) ---");
            System.out.println("Excluindo produto 'Refrigerante' (ID " + idRefri + ")...");
            produtoDAO.delete(idRefri);
            System.out.println("Tentando buscar produto com ID " + idRefri + " após exclusão: " + produtoDAO.read(idRefri));

            System.out.println("\nLista ordenada de produtos após a exclusão:");
            produtosOrdenados = produtoDAO.listAllSortedBySecondaryKey();
            for (Produto p : produtosOrdenados) {
                System.out.println(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

