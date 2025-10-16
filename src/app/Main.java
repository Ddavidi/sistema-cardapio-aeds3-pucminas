package app;

import dao.DAO;
import model.Cardapio;
import model.Categoria;
import model.Empresa;
import model.Produto;

import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static DAO<Empresa> empresaDAO;
    private static DAO<Cardapio> cardapioDAO;
    private static DAO<Produto> produtoDAO;
    private static DAO<Categoria> categoriaDAO;

    public static void main(String[] args) {
        try {
            // --- INICIALIZAÇÃO INICIAL DOS DAOs ---
            inicializarDAOs();

            Scanner console = new Scanner(System.in);
            int opcao;

            do {
                System.out.println("\n\n--- MENU PRINCIPAL ---");
                System.out.println("1) Gerenciar Empresas");
                System.out.println("2) Gerenciar Cardápios");
                System.out.println("3) Gerenciar Categorias");
                System.out.println("4) Gerenciar Produtos");
                System.out.println("9) Apagar TODOS os dados (Resetar)");
                System.out.println("0) Sair");
                System.out.print("Opção: ");

                opcao = console.nextInt();
                console.nextLine();

                switch (opcao) {
                    case 1: menuEmpresas(console); break;
                    case 2: menuCardapios(console); break;
                    case 3: menuCategorias(console); break;
                    case 4: menuProdutos(console); break;
                    case 9: confirmarEApagarDados(console); break;
                    case 0: System.out.println("Saindo do sistema..."); break;
                    default: System.out.println("Opção inválida!");
                }
            } while (opcao != 0);

            console.close();

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
    }

    public static void fecharDAOs() throws Exception {
        empresaDAO.close();
        cardapioDAO.close();
        produtoDAO.close();
        categoriaDAO.close();
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
            new File("empresas.db").delete(); new File("empresas.hash.dir").delete(); new File("empresas.hash.bkt").delete(); new File("empresas.bptree.idx").delete();
            new File("cardapios.db").delete(); new File("cardapios.hash.dir").delete(); new File("cardapios.hash.bkt").delete(); new File("cardapios.bptree.idx").delete();
            new File("produtos.db").delete(); new File("produtos.hash.dir").delete(); new File("produtos.hash.bkt").delete(); new File("produtos.bptree.idx").delete();
            new File("categorias.db").delete(); new File("categorias.hash.dir").delete(); new File("categorias.hash.bkt").delete();

            System.out.println("Base de dados resetada. A reiniciar conexões...");
            inicializarDAOs(); // Recria os DAOs com os ficheiros limpos
            System.out.println("Sistema pronto para ser usado novamente.");
        } else {
            System.out.println("Operação cancelada.");
        }
    }

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

            opcao = console.nextInt();
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

            opcao = console.nextInt();
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

            opcao = console.nextInt();
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

            opcao = console.nextInt();
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
            }
        } while (opcao != 0);
    }
}

