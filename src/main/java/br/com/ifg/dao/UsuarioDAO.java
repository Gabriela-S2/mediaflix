package br.com.ifg.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    /**
     * Verifica se já existe um username cadastrado.
     */
    public boolean existeUsername(String username) {
        String sql = "SELECT 1 FROM usuarios WHERE username = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            registrarLogExcecao("existeUsername", e);
            return true; // Bloqueia por segurança em caso de erro
        }
    }

    /**
     * Verifica se já existe um email cadastrado.
     */
    public boolean existeEmail(String email) {
        String sql = "SELECT 1 FROM usuarios WHERE email = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            registrarLogExcecao("existeEmail", e);
            return true;
        }
    }

    /**
     * Verifica se já existe um email cadastrado, IGNORANDO o ID do próprio usuário que está editando.
     */
    public boolean existeEmail(String email, int idUsuarioAtual) {
        String sql = "SELECT 1 FROM usuarios WHERE email = ? AND id != ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, idUsuarioAtual);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            registrarLogExcecao("existeEmail(ignorarId)", e);
            return true; // Bloqueia por segurança em caso de erro
        }
    }

    /**
     * Autentica e retorna o objeto Usuario se as credenciais estiverem corretas,
     * ou null caso contrário.
     * Login aceita tanto username quanto email.
     */
    public Usuario autenticarRetornandoUsuario(String login, String senha) {
        String sql = "SELECT id, username, email, caminho_foto, is_admin FROM usuarios WHERE (email = ? OR username = ?) AND senha = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, login);
            stmt.setString(2, login);
            stmt.setString(3, senha); // TODO: usar hash (ex: BCrypt) em produção

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Usuario(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("caminho_foto"),
                        rs.getBoolean("is_admin") // Mapeando o novo campo
                );
            }
        } catch (SQLException e) {
            registrarLogExcecao("autenticarRetornandoUsuario", e);
        }
        return null;
    }

    /**
     * Versão booleana mantida para compatibilidade (usada na validação de senha atual em EditarPerfil).
     */
    public boolean autenticar(String login, String senha) {
        return autenticarRetornandoUsuario(login, senha) != null;
    }

    public Usuario buscarPorLoginOuEmail(String loginOuEmail) {
        String sqlBusca = "SELECT id, username, email, caminho_foto, is_admin FROM usuarios WHERE username = ? OR email = ? LIMIT 1";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement ps = conn.prepareStatement(sqlBusca)) {
            ps.setString(1, loginOuEmail);
            ps.setString(2, loginOuEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("caminho_foto"),
                            rs.getBoolean("is_admin") // Mapeando o novo campo
                    );
                }
            }
        } catch (SQLException e) {
            LogDAO.registrarErro("UsuarioDAO.buscarPorLoginOuEmail", e);
        }
        return null;
    }

    /**
     * Cadastra um novo usuário no banco.
     * A senha é armazenada em texto plano por ora
     * (idealmente deveria usar BCrypt ou similar).
     */
    public void cadastrar(String username, String email, String senha) {
        String sql = "INSERT INTO usuarios (username, email, senha) VALUES (?, ?, ?)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, senha);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogDAO.registrarErro("UsuarioDAO.cadastrar", e);
            throw new RuntimeException("Erro ao cadastrar usuário: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza o email de um usuário.
     */
    public void atualizarEmail(Usuario usuario) {
        String sql = "UPDATE usuarios SET email = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getEmail());
            stmt.setInt(2, usuario.getId());
            stmt.executeUpdate();
            registrarLogUso("Email do usuário id=" + usuario.getId() + " atualizado.");

        } catch (SQLException e) {
            registrarLogExcecao("atualizarEmail", e);
        }
    }

    /**
     * Atualiza a senha de um usuário.
     */
    public void atualizarSenha(Usuario usuario, String novaSenha) {
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novaSenha); // TODO: hash
            stmt.setInt(2, usuario.getId());
            stmt.executeUpdate();
            registrarLogUso("Senha do usuário id=" + usuario.getId() + " atualizada.");

        } catch (SQLException e) {
            registrarLogExcecao("atualizarSenha", e);
        }
    }

    /**
     * Atualiza o caminho da foto de perfil.
     */
    public void atualizarFotoPerfil(Usuario usuario) {
        String sql = "UPDATE usuarios SET caminho_foto = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getCaminhoFoto());
            stmt.setInt(2, usuario.getId());
            stmt.executeUpdate();
            registrarLogUso("Foto de perfil do usuário id=" + usuario.getId() + " atualizada.");

        } catch (SQLException e) {
            registrarLogExcecao("atualizarFotoPerfil", e);
        }
    }

    /**
     * Atualiza o código de recuperação de senha gerado para o email informado.
     * O código deve ser salvo temporariamente no banco para comparação posterior.
     */
    public boolean salvarCodigoRecuperacao(String email, String codigo) {
        String sql = "UPDATE usuarios SET codigo_recuperacao = ?, codigo_expiracao = NOW() + INTERVAL '15 minutes' WHERE email = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);
            stmt.setString(2, email);
            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;

        } catch (SQLException e) {
            registrarLogExcecao("salvarCodigoRecuperacao", e);
            return false;
        }
    }

    /**
     * Valida o código de recuperação informado pelo usuário.
     * Retorna o email associado ao código, ou null se inválido/expirado.
     */
    public String validarCodigoRecuperacao(String codigo) {
        String sql = "SELECT email FROM usuarios WHERE codigo_recuperacao = ? AND codigo_expiracao > NOW()";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }

        } catch (SQLException e) {
            registrarLogExcecao("validarCodigoRecuperacao", e);
        }
        return null;
    }

    /**
     * Redefine a senha pelo email após validação do código.
     * Limpa o código de recuperação após uso.
     */
    public boolean redefinirSenhaPorEmail(String email, String novaSenha) {
        String sql = "UPDATE usuarios SET senha = ?, codigo_recuperacao = NULL, codigo_expiracao = NULL WHERE email = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novaSenha); // TODO: hash
            stmt.setString(2, email);
            int linhas = stmt.executeUpdate();
            registrarLogUso("Senha redefinida para o email: " + email);
            return linhas > 0;

        } catch (SQLException e) {
            registrarLogExcecao("redefinirSenhaPorEmail", e);
            return false;
        }
    }

    // --- Logs ---

    private void registrarLogUso(String acao) {
        System.out.println("[DAO LOG] " + acao);
    }

    private void registrarLogExcecao(String metodo, Exception e) {
        System.err.println("[DAO ERRO] UsuarioDAO." + metodo + " - " + e.getMessage());
    }

    /**
     * Valida se a senha antiga informada corresponde à senha do usuário no banco.
     */
    public boolean validarSenhaAntiga(int id, String senhaAntiga) {
        String sql = "SELECT 1 FROM usuarios WHERE id = ? AND senha = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, senhaAntiga);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            registrarLogExcecao("validarSenhaAntiga", e);
            return false;
        }
    }

    /**
     * Verifica se o username já existe, IGNORANDO o ID do próprio usuário que está editando.
     */
    public boolean existeUsername(String username, int idUsuarioAtual) {
        String sql = "SELECT 1 FROM usuarios WHERE username = ? AND id != ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, idUsuarioAtual);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            registrarLogExcecao("existeUsername(ignorarId)", e);
            return true;
        }
    }

    /**
     * Atualiza o perfil do usuário de forma dinâmica. Atualiza a senha e a foto apenas se informadas.
     */
    public boolean atualizarPerfil(int id, String username, String email, String novaSenha, String caminhoFoto) {
        // Monta a query dinamicamente baseada nos campos preenchidos
        StringBuilder sql = new StringBuilder("UPDATE usuarios SET username = ?, email = ?");

        if (novaSenha != null && !novaSenha.trim().isEmpty()) {
            sql.append(", senha = ?");
        }
        if (caminhoFoto != null && !caminhoFoto.trim().isEmpty()) {
            sql.append(", caminho_foto = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int index = 1;
            stmt.setString(index++, username);
            stmt.setString(index++, email);

            if (novaSenha != null && !novaSenha.trim().isEmpty()) {
                stmt.setString(index++, novaSenha); // TODO: Lembre-se de aplicar o Hash aqui depois
            }
            if (caminhoFoto != null && !caminhoFoto.trim().isEmpty()) {
                stmt.setString(index++, caminhoFoto);
            }

            stmt.setInt(index, id);

            int linhasAfetadas = stmt.executeUpdate();
            if (linhasAfetadas > 0) {
                registrarLogUso("Perfil do usuário id=" + id + " atualizado com sucesso.");
                return true;
            }
            return false;

        } catch (SQLException e) {
            registrarLogExcecao("atualizarPerfil", e);
            return false;
        }
    }

    /**
     * Busca um usuário pelo ID. Necessário para popular a tela de edição de admin
     * quando um usuário é escolhido no ChoiceBox.
     */
    public Usuario buscarPorId(int id) {
        String sql = "SELECT id, username, email, senha, caminho_foto, is_admin FROM usuarios WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            registrarLogExcecao("buscarPorId", e);
        }
        return null;
    }

    /**
     * Atualiza um usuário a partir da tela administrativa (EditarAdmin).
     * - username e email são sempre atualizados (se vierem vazios, o controller já
     *   deve ter preenchido com o valor atual antes de chamar este método).
     * - is_admin é SEMPRE atualizado, pois o CheckBox sempre representa um estado
     *   definido (marcado/desmarcado). É aqui que a conversão comum <-> admin
     *   efetivamente acontece — antes essa coluna nunca era enviada no UPDATE.
     * - senha e caminhoFoto só entram na query se tiverem sido informados,
     *   permitindo a edição de itens isolados (não precisa preencher tudo de novo).
     */
    public boolean atualizarUsuarioAdmin(int id, String username, String email, String novaSenha, String caminhoFoto, boolean isAdmin) {
        StringBuilder sql = new StringBuilder("UPDATE usuarios SET username = ?, email = ?, is_admin = ?");

        boolean temNovaSenha = novaSenha != null && !novaSenha.trim().isEmpty();
        boolean temNovaFoto = caminhoFoto != null && !caminhoFoto.trim().isEmpty();

        if (temNovaSenha) {
            sql.append(", senha = ?");
        }
        if (temNovaFoto) {
            sql.append(", caminho_foto = ?");
        }
        sql.append(" WHERE id = ?");

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int index = 1;
            stmt.setString(index++, username);
            stmt.setString(index++, email);
            stmt.setBoolean(index++, isAdmin);

            if (temNovaSenha) {
                stmt.setString(index++, novaSenha); // TODO: aplicar hash
            }
            if (temNovaFoto) {
                stmt.setString(index++, caminhoFoto);
            }

            stmt.setInt(index, id);

            int linhasAfetadas = stmt.executeUpdate();
            if (linhasAfetadas > 0) {
                registrarLogUso("Usuário id=" + id + " atualizado via painel admin (is_admin=" + isAdmin + ").");
                return true;
            }
            return false;

        } catch (SQLException e) {
            registrarLogExcecao("atualizarUsuarioAdmin", e);
            return false;
        }
    }

    public void cadastrarAdmin(String username, String email, String senha) {
        String sql = "INSERT INTO usuarios (username, email, senha, is_admin) VALUES (?, ?, ?, TRUE)";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, senha);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogDAO.registrarErro("UsuarioDAO.cadastrarAdmin", e);
            throw new RuntimeException("Erro ao cadastrar administrador", e);
        }
    }

    public static List<Usuario> listarAdmins() {
        List<Usuario> lista = new ArrayList<>();
        // CORREÇÃO: Adicionado 'senha' na query para não dar erro no mapeamento
        String query = "SELECT id, username, email, senha, caminho_foto, is_admin FROM usuarios WHERE is_admin = true ORDER BY id";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        } catch (SQLException ex) {
            LogDAO.registrarErro("UsuarioDAO.listarAdmins", ex);
        }
        return lista;
    }

    // NOVO: Para o admin poder ver e editar qualquer usuário (comum ou admin)
    public static List<Usuario> listarTodosUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        String query = "SELECT id, username, email, senha, caminho_foto, is_admin FROM usuarios ORDER BY id";

        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        } catch (SQLException ex) {
            LogDAO.registrarErro("UsuarioDAO.listarTodosUsuarios", ex);
        }
        return lista;
    }

    // NOVO: Método para promover/rebaixar admin (como comentado no seu Controller)
    public void atualizarStatusAdmin(int id, boolean isAdmin) {
        String sql = "UPDATE usuarios SET is_admin = ? WHERE id = ?";
        try (Connection conn = ConexaoDB.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isAdmin);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            registrarLogExcecao("atualizarStatusAdmin", e);
        }
    }

    // CORREÇÃO: Leitura correta das colunas do banco
    private static Usuario mapearUsuario(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("senha"),
                rs.getString("caminho_foto"), // Corrigido de caminho_imagem para caminho_foto
                rs.getBoolean("is_admin")
        );
    }
}