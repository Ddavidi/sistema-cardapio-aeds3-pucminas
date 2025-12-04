package app;

import compressao.BackupManager;
import dao.DAO;
import model.Cardapio;
import model.Categoria;
import model.Empresa;
import model.Produto;
import model.ProdutoCardapio;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {

    // --- DAOs GLOBAIS ---
    private static DAO<Empresa> empresaDAO;
    private static DAO<Cardapio> cardapioDAO;
    private static DAO<Produto> produtoDAO;
    private static DAO<Categoria> categoriaDAO;
    private static DAO<ProdutoCardapio> produtoCardapioDAO;

    public static void main(String[] args) {
        try {
            inicializarDAOs();
            Scanner console = new Scanner(System.in);
            int opcao;
            do {
                System.out.println("\n\n--- MENU PRINCIPAL ---");
                System.out.println("1) Gerenciar Empresas");
                System.out.println("2) Gerenciar Cardápios");
                System.out.println("3) Gerenciar Categorias");
                System.out.println("4) Gerenciar Produtos");
                System.out.println("5) Gerenciar Relações (Produto-Cardápio)");
                System.out.println("6) Realizar Backup Completo (Compressão)"); // NOVA OPÇÃO
                System.out.println("9) Apagar TODOS os dados (Resetar)");
                System.out.println("0) Sair");
                System.out.print("Opção: ");

                try {
                    opcao = console.nextInt();
                } catch (java.util.InputMismatchException e) {
                    opcao = -1;
                }
                console.nextLine();

                switch (opcao) {
                    case 1: menuEmpresas(console); break;
                    case 2: menuCardapios(console); break;
                    case 3: menuCategorias(console); break;
                    case 4: menuProdutos(console); break;
                    case 5: menuProdutoCardapio(console); break;

                    case 6: // LÓGICA DO BACKUP
                        System.out.print("Digite a versão do backup (ex: 1): ");
                        int versao = console.nextInt();
                        console.nextLine();

                        System.out.println("Fechando conexões para garantir integridade...");
                        fecharDAOs(); // Garante que tudo foi escrito no disco

                        // Chama o gestor de backup
                        BackupManager.createBackup(versao);

                        System.out.println("Reabrindo conexões...");
                        inicializarDAOs(); // Reabre para continuar a usar o programa
                        break;

                    case 9: confirmarEApagarDados(console); break;
                    case 0: System.out.println("Saindo do sistema..."); break;
                    default: System.out.println("Opção inválida!");
                }
            } while (opcao != 0);

            console.close();
            fecharDAOs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MÉTODOS DE MENU E UTILITÁRIOS ---

    public static void inicializarDAOs() throws Exception {
        empresaDAO = new DAO<>("empresas.db", Empresa.class, true);
        cardapioDAO = new DAO<>("cardapios.db", Cardapio.class, true);
        produtoDAO = new DAO<>("produtos.db", Produto.class, true);
        categoriaDAO = new DAO<>("categorias.db", Categoria.class, false);
        produtoCardapioDAO = new DAO<>("produtocardapio.db", ProdutoCardapio.class, true);
    }

    public static void fecharDAOs() throws Exception {
        if (empresaDAO != null) empresaDAO.close();
        if (cardapioDAO != null) cardapioDAO.close();
        if (produtoDAO != null) produtoDAO.close();
        if (categoriaDAO != null) categoriaDAO.close();
        if (produtoCardapioDAO != null) produtoCardapioDAO.close();
    }

    public static void confirmarEApagarDados(Scanner console) throws Exception {
        System.out.println("\n--- ATENÇÃO! ---");
        System.out.println("Esta ação irá apagar permanentemente TODOS os ficheiros da base de dados.");
        System.out.print("Tem a certeza que deseja continuar? (S/N): ");
        String confirmacao = console.nextLine().toUpperCase();

        if (confirmacao.equals("S")) {
            System.out.println("A fechar conexões...");
            fecharDAOs();
            System.out.println("A apagar ficheiros...");

            // Dados
            new File("empresas.db").delete(); new File("empresas.hash.dir").delete(); new File("empresas.hash.bkt").delete(); new File("empresas.bptree.idx").delete();
            new File("cardapios.db").delete(); new File("cardapios.hash.dir").delete(); new File("cardapios.hash.bkt").delete(); new File("cardapios.bptree.idx").delete();
            new File("produtos.db").delete(); new File("produtos.hash.dir").delete(); new File("produtos.hash.bkt").delete(); new File("produtos.bptree.idx").delete();
            new File("categorias.db").delete(); new File("categorias.hash.dir").delete(); new File("categorias.hash.bkt").delete();
            new File("produtocardapio.db").delete(); new File("produtocardapio.hash.dir").delete(); new File("produtocardapio.hash.bkt").delete(); new File("produtocardapio.bptree.idx").delete();

            // Chaves RSA (Opcional: pode querer mantê-las)
            new File("public.key").delete();
            new File("private.key").delete();

            System.out.println("Base de dados resetada. A reiniciar conexões...");
            inicializarDAOs();
            System.out.println("Sistema pronto para ser usado novamente.");
        } else {
            System.out.println("Operação cancelada.");
        }
    }

    // --- MENUS DE ENTIDADES ---

    public static void menuEmpresas(Scanner console) throws Exception {
        int opcao;
        do {
            System.out.println("\n--- GERENCIAR EMPRESAS ---");
            System.out.println("1) Criar nova empresa");
            System.out.println("2) Listar empresas");
            System.out.println("3) Buscar empresa por ID");
            System.out.println("4) Atualizar empresa");
            System.out.println("5) Excluir empresa");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            try {
                opcao = console.nextInt();
            } catch (java.util.InputMismatchException e) {
                opcao = -1;
            }
            console.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome da empresa: ");
                    String nome = console.nextLine();
                    System.out.print("CNPJ: ");
                    String cnpj = console.nextLine();
                    System.out.print("Telefones (separados por ';'): ");
                    String telefonesStr = console.nextLine();
                    String[] telefones = telefonesStr.split(";");

                    Empresa novaEmpresa = new Empresa(nome, cnpj, telefones);
                    int id = empresaDAO.create(novaEmpresa);
                    System.out.println("Empresa criada com sucesso! ID: " + id);
                    break;
                case 2:
                    List<Empresa> empresas = empresaDAO.listAllSortedBySecondaryKey();
                    if (empresas.isEmpty()) System.out.println("Nenhuma empresa cadastrada.");
                    else empresas.forEach(System.out::println);
                    break;
                case 3:
                    System.out.print("ID da empresa: ");
                    int idBusca = console.nextInt();
                    Empresa empresa = empresaDAO.read(idBusca);
                    if (empresa != null) System.out.println(empresa);
                    else System.out.println("Empresa não encontrada.");
                    break;
                case 4:
                    System.out.print("ID da empresa para atualizar: ");
                    int idUpdate = console.nextInt();
                    console.nextLine();
                    Empresa empresaAtualizar = empresaDAO.read(idUpdate);
                    if (empresaAtualizar != null) {
                        System.out.print("Novo nome (atual: " + empresaAtualizar.getNome() + "): ");
                        String novoNome = console.nextLine();
                        if (!novoNome.isEmpty()) empresaAtualizar.setNome(novoNome);

                        empresaDAO.update(empresaAtualizar);
                        System.out.println("Empresa atualizada!");
                    } else System.out.println("Empresa não encontrada.");
                    break;
                case 5:
                    System.out.print("ID da empresa para excluir: ");
                    int idDelete = console.nextInt();
                    if (empresaDAO.delete(idDelete)) System.out.println("Empresa excluída!");
                    else System.out.println("Erro ao excluir.");
                    break;
                case 0: break;
                default: System.out.println("Opção inválida!");
            }
        } while (opcao != 0);
    }

    public static void menuCardapios(Scanner console) throws Exception {
        int opcao;
        do {
            System.out.println("\n--- GERENCIAR CARDÁPIOS ---");
            System.out.println("1) Criar novo cardápio");
            System.out.println("2) Listar cardápios");
            System.out.println("3) Buscar cardápio por ID");
            System.out.println("4) Atualizar cardápio");
            System.out.println("5) Excluir cardápio");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            try {
                opcao = console.nextInt();
            } catch (java.util.InputMismatchException e) {
                opcao = -1;
            }
            console.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome do cardápio: ");
                    String nome = console.nextLine();
                    System.out.print("Descrição: ");
                    String descricao = console.nextLine();

                    Cardapio novoCardapio = new Cardapio(nome, descricao, true);
                    int id = cardapioDAO.create(novoCardapio);
                    System.out.println("Cardápio criado com sucesso! ID: " + id);
                    break;
                case 2:
                    List<Cardapio> cardapios = cardapioDAO.listAllSortedBySecondaryKey();
                    if (cardapios.isEmpty()) System.out.println("Nenhum cardápio cadastrado.");
                    else cardapios.forEach(System.out::println);
                    break;
                case 3:
                    System.out.print("ID do cardápio: ");
                    int idBusca = console.nextInt();
                    Cardapio cardapio = cardapioDAO.read(idBusca);
                    if (cardapio != null) System.out.println(cardapio);
                    else System.out.println("Cardápio não encontrado.");
                    break;
                case 4:
                    System.out.print("ID do cardápio para atualizar: ");
                    int idUpdate = console.nextInt();
                    console.nextLine();
                    Cardapio cardapioAtualizar = cardapioDAO.read(idUpdate);
                    if (cardapioAtualizar != null) {
                        System.out.print("Novo nome (atual: " + cardapioAtualizar.getNome() + "): ");
                        String novoNome = console.nextLine();
                        if (!novoNome.isEmpty()) cardapioAtualizar.setNome(novoNome);

                        cardapioDAO.update(cardapioAtualizar);
                        System.out.println("Cardápio atualizado!");
                    } else System.out.println("Cardápio não encontrado.");
                    break;
                case 5:
                    System.out.print("ID do cardápio para excluir: ");
                    int idDelete = console.nextInt();
                    if (cardapioDAO.delete(idDelete)) System.out.println("Cardápio excluído!");
                    else System.out.println("Erro ao excluir.");
                    break;
                case 0: break;
                default: System.out.println("Opção inválida!");
            }
        } while (opcao != 0);
    }

    public static void menuProdutos(Scanner console) throws Exception {
        int opcao;
        do {
            System.out.println("\n--- GERENCIAR PRODUTOS ---");
            System.out.println("1) Criar novo produto");
            System.out.println("2) Listar produtos");
            System.out.println("3) Buscar produto por ID");
            System.out.println("4) Atualizar produto");
            System.out.println("5) Excluir produto");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            try {
                opcao = console.nextInt();
            } catch (java.util.InputMismatchException e) {
                opcao = -1;
            }
            console.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome do produto: ");
                    String nome = console.nextLine();
                    System.out.print("Descrição: ");
                    String descricao = console.nextLine();

                    System.out.println("Categorias disponíveis:");
                    List<Categoria> categorias = categoriaDAO.listAll();
                    if (categorias.isEmpty()) {
                        System.out.println("Nenhuma categoria cadastrada. Crie uma categoria primeiro.");
                        break;
                    }
                    categorias.forEach(c -> System.out.println("ID: " + c.getID() + " - " + c.getNome()));
                    System.out.print("Digite o ID da categoria do produto: ");
                    int idCategoria = console.nextInt();

                    Produto novoProduto = new Produto(nome, descricao, idCategoria);
                    int id = produtoDAO.create(novoProduto);
                    System.out.println("Produto criado com sucesso! ID: " + id);
                    break;
                case 2:
                    List<Produto> produtos = produtoDAO.listAllSortedBySecondaryKey();
                    if (produtos.isEmpty()) System.out.println("Nenhum produto cadastrado.");
                    else produtos.forEach(System.out::println);
                    break;
                case 3:
                    System.out.print("ID do produto: ");
                    int idBusca = console.nextInt();
                    Produto produto = produtoDAO.read(idBusca);
                    if (produto != null) System.out.println(produto);
                    else System.out.println("Produto não encontrado.");
                    break;
                case 4:
                    System.out.print("ID do produto para atualizar: ");
                    int idUpdate = console.nextInt();
                    console.nextLine();
                    Produto produtoAtualizar = produtoDAO.read(idUpdate);
                    if (produtoAtualizar != null) {
                        System.out.print("Novo nome (atual: " + produtoAtualizar.getNome() + "): ");
                        String novoNome = console.nextLine();
                        if (!novoNome.isEmpty()) produtoAtualizar.setNome(novoNome);

                        produtoDAO.update(produtoAtualizar);
                        System.out.println("Produto atualizado!");
                    } else System.out.println("Produto não encontrado.");
                    break;
                case 5:
                    System.out.print("ID do produto para excluir: ");
                    int idDelete = console.nextInt();
                    if (produtoDAO.delete(idDelete)) System.out.println("Produto excluído!");
                    else System.out.println("Erro ao excluir.");
                    break;
                case 0: break;
                default: System.out.println("Opção inválida!");
            }
        } while (opcao != 0);
    }

    public static void menuCategorias(Scanner console) throws Exception {
        int opcao;
        do {
            System.out.println("\n--- GERENCIAR CATEGORIAS ---");
            System.out.println("1) Criar nova categoria");
            System.out.println("2) Listar categorias");
            System.out.println("3) Buscar categoria por ID");
            System.out.println("4) Atualizar categoria");
            System.out.println("5) Excluir categoria");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            try {
                opcao = console.nextInt();
            } catch (java.util.InputMismatchException e) {
                opcao = -1;
            }
            console.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome da categoria: ");
                    String nome = console.nextLine();
                    Categoria novaCategoria = new Categoria(nome);
                    int id = categoriaDAO.create(novaCategoria);
                    System.out.println("Categoria criada com sucesso! ID: " + id);
                    break;
                case 2:
                    List<Categoria> categorias = categoriaDAO.listAll();
                    if (categorias.isEmpty()) System.out.println("Nenhuma categoria cadastrada.");
                    else categorias.forEach(System.out::println);
                    break;
                case 3:
                    System.out.print("ID da categoria: ");
                    int idBusca = console.nextInt();
                    Categoria categoria = categoriaDAO.read(idBusca);
                    if (categoria != null) System.out.println(categoria);
                    else System.out.println("Categoria não encontrada.");
                    break;
                case 4:
                    System.out.print("ID da categoria para atualizar: ");
                    int idUpdate = console.nextInt();
                    console.nextLine();
                    Categoria categoriaAtualizar = categoriaDAO.read(idUpdate);
                    if (categoriaAtualizar != null) {
                        System.out.print("Novo nome (atual: " + categoriaAtualizar.getNome() + "): ");
                        String novoNome = console.nextLine();
                        if (!novoNome.isEmpty()) categoriaAtualizar.setNome(novoNome);
                        categoriaDAO.update(categoriaAtualizar);
                        System.out.println("Categoria atualizada!");
                    } else System.out.println("Categoria não encontrada.");
                    break;
                case 5:
                    System.out.print("ID da categoria para excluir: ");
                    int idDelete = console.nextInt();
                    if (categoriaDAO.delete(idDelete)) System.out.println("Categoria excluída!");
                    else System.out.println("Erro ao excluir.");
                    break;
                case 0: break;
                default: System.out.println("Opção inválida!");
            }
        } while (opcao != 0);
    }

    public static void menuProdutoCardapio(Scanner console) throws Exception {
        int opcao;
        do {
            System.out.println("\n--- GERENCIAR RELAÇÕES (PRODUTO-CARDÁPIO) ---");
            System.out.println("1) Adicionar Produto a um Cardápio");
            System.out.println("2) Listar Produtos de um Cardápio (Otimizado)");
            System.out.println("3) Listar Cardápios que contêm um Produto (Não Otimizado)");
            System.out.println("4) Remover Relação Produto-Cardápio");
            System.out.println("5) Listar todas as relações (para depuração)");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            try {
                opcao = console.nextInt();
            } catch (java.util.InputMismatchException e) {
                opcao = -1;
            }
            console.nextLine();

            switch (opcao) {
                case 1:
                    adicionarProdutoAoCardapio(console);
                    break;
                case 2:
                    listarProdutosDoCardapio(console);
                    break;
                case 3:
                    listarCardapiosDoProduto(console);
                    break;
                case 4:
                    removerProdutoDoCardapio(console);
                    break;
                case 5:
                    List<ProdutoCardapio> relacoes = produtoCardapioDAO.listAll();
                    if(relacoes.isEmpty()) System.out.println("Nenhuma relação cadastrada.");
                    else relacoes.forEach(System.out::println);
                    break;
                case 0: break;
                default: System.out.println("Opção inválida!");
            }
        } while (opcao != 0);
    }

    private static void adicionarProdutoAoCardapio(Scanner console) throws Exception {
        System.out.println("\n--- Adicionar Produto ao Cardápio ---");

        System.out.println("Cardápios disponíveis:");
        List<Cardapio> cardapios = cardapioDAO.listAll();
        if (cardapios.isEmpty()) { System.out.println("Nenhum cardápio cadastrado. Crie um cardápio primeiro."); return; }
        cardapios.forEach(System.out::println);
        System.out.print("ID do Cardápio: ");
        int idCardapio = console.nextInt();

        System.out.println("\nProdutos disponíveis:");
        List<Produto> produtos = produtoDAO.listAll();
        if (produtos.isEmpty()) { System.out.println("Nenhum produto cadastrado. Crie um produto primeiro."); return; }
        produtos.forEach(System.out::println);
        System.out.print("ID do Produto: ");
        int idProduto = console.nextInt();

        System.out.print("Preço do produto neste cardápio: ");
        float preco = console.nextFloat();

        ProdutoCardapio novaRelacao = new ProdutoCardapio(idProduto, idCardapio, preco);
        int idRelacao = produtoCardapioDAO.create(novaRelacao);

        System.out.println("Produto (ID " + idProduto + ") adicionado ao Cardápio (ID " + idCardapio + ") com sucesso! (ID da Relação: " + idRelacao + ")");
    }

    private static void listarProdutosDoCardapio(Scanner console) throws Exception {
        System.out.println("\n--- Listar Produtos de um Cardápio (Otimizado) ---");

        System.out.println("Cardápios disponíveis:");
        List<Cardapio> cardapios = cardapioDAO.listAll();
        if (cardapios.isEmpty()) { System.out.println("Nenhum cardápio cadastrado."); return; }
        cardapios.forEach(System.out::println);
        System.out.print("ID do Cardápio para listar os produtos: ");
        int idCardapio = console.nextInt();

        Cardapio c = cardapioDAO.read(idCardapio);
        if (c == null) { System.out.println("Cardápio não encontrado."); return; }

        System.out.println("\nProdutos no Cardápio: " + c.getNome());

        // 1. Formata o prefixo de busca
        String prefixo = String.format("%010d-", idCardapio);

        // 2. Chama o novo método do DAO que usa a Árvore B+
        List<ProdutoCardapio> relacoes = produtoCardapioDAO.listAllBySecondaryKeyPrefix(prefixo);

        if (relacoes.isEmpty()) {
            System.out.println("Este cardápio não possui produtos associados.");
            return;
        }

        for (ProdutoCardapio relacao : relacoes) {
            Produto p = produtoDAO.read(relacao.getIdProduto());
            if (p != null) {
                System.out.println("- " + p.getNome() + " (Preço: R$ " + relacao.getPreco() + ")");
            }
        }
    }

    private static void listarCardapiosDoProduto(Scanner console) throws Exception {
        System.out.println("\n--- Listar Cardápios que contêm um Produto (Não Otimizado) ---");

        System.out.println("Produtos disponíveis:");
        List<Produto> produtos = produtoDAO.listAll();
        if (produtos.isEmpty()) { System.out.println("Nenhum produto cadastrado."); return; }
        produtos.forEach(System.out::println);
        System.out.print("ID do Produto para buscar cardápios: ");
        int idProduto = console.nextInt();

        Produto p = produtoDAO.read(idProduto);
        if (p == null) { System.out.println("Produto não encontrado."); return; }

        System.out.println("\nCardápios que contêm: " + p.getNome());

        // Lista Cardapios O(N)
        List<ProdutoCardapio> todasRelacoes = produtoCardapioDAO.listAll();
        int count = 0;
        for (ProdutoCardapio relacao : todasRelacoes) {
            if (relacao.getIdProduto() == idProduto) {
                Cardapio c = cardapioDAO.read(relacao.getIdCardapio());
                if (c != null) {
                    System.out.println("- " + c.getNome() + " (Vendido por: R$ " + relacao.getPreco() + ")");
                    count++;
                }
            }
        }

        if (count == 0) System.out.println("Este produto não está associado a nenhum cardápio.");
    }

    private static void removerProdutoDoCardapio(Scanner console) throws Exception {
        System.out.println("\n--- Remover Relação Produto-Cardápio ---");
        System.out.println("Abaixo estão todas as relações ativas:");
        List<ProdutoCardapio> relacoes = produtoCardapioDAO.listAll();
        if(relacoes.isEmpty()) { System.out.println("Nenhuma relação cadastrada."); return; }

        for (ProdutoCardapio rel : relacoes) {
            Produto p = produtoDAO.read(rel.getIdProduto());
            Cardapio c = cardapioDAO.read(rel.getIdCardapio());
            if (p != null && c != null) {
                System.out.println("ID Relação [" + rel.getID() + "]: Produto '" + p.getNome() + "' em Cardápio '" + c.getNome() + "'");
            }
        }

        System.out.print("\nDigite o ID da Relação que deseja excluir: ");
        int idRelacao = console.nextInt();

        if (produtoCardapioDAO.delete(idRelacao)) {
            System.out.println("Relação excluída com sucesso!");
        } else {
            System.out.println("Erro ao excluir ou ID da relação não encontrado.");
        }
    }
}