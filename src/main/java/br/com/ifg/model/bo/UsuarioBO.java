package br.com.ifg.model.bo;

import br.com.ifg.model.dto.AtualizarPerfilRequestDTO;
import br.com.ifg.model.dto.UsuarioRequestDTO;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Set;

@ApplicationScoped
public class UsuarioBO {

    private static final Set<String> TIPOS_FOTO_PERMITIDOS = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    public void validarNovoCadastro(UsuarioRequestDTO dto, boolean emailEmUso, boolean usernameEmUso, Plano plano) {
        if (emailEmUso) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        if (usernameEmUso) {
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }
        if (plano == null) {
            throw new IllegalArgumentException("Plano selecionado inválido.");
        }
    }

    public void validarEAplicarAtualizacaoPerfil(
            Usuario usuario,
            AtualizarPerfilRequestDTO dto,
            Usuario usuarioComMesmoUsername,
            Plano plano
    ) {
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        if (usuarioComMesmoUsername != null && !usuarioComMesmoUsername.getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Este nome de usuário já está em uso.");
        }
        if (plano == null) {
            throw new IllegalArgumentException("Plano selecionado inválido.");
        }

        usuario.setUsername(dto.username());
        usuario.setPlano(plano);

        // Mantém a foto existente caso o DTO venha com caminhoFoto nulo ou em branco
        if (dto.caminhoFoto() != null && !dto.caminhoFoto().isBlank()) {
            usuario.setCaminhoFoto(dto.caminhoFoto());
        }
    }

    public void validarCredenciais(Usuario usuario, String senhaInformada) {
        if (usuario == null || !BcryptUtil.matches(senhaInformada, usuario.getSenha())) {
            throw new SecurityException("E-mail ou senha incorretos.");
        }
    }

    public Set<String> resolverGruposJwt(Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getIsAdmin())
                ? Set.of("USER", "ADMIN")
                : Set.of("USER");
    }

    public void validarUploadFotoPerfil(FileUpload arquivo) {
        if (arquivo == null || arquivo.uploadedFile() == null || arquivo.size() == 0) {
            throw new IllegalArgumentException("Nenhuma imagem foi enviada.");
        }
        String contentType = arquivo.contentType();
        if (contentType == null || !TIPOS_FOTO_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato inválido. Envie apenas PNG, JPG ou WEBP.");
        }
    }
}