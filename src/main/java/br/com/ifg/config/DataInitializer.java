package br.com.ifg.config;

import br.com.ifg.model.dao.PlanoDAO;
import br.com.ifg.model.dao.UsuarioDAO;
import br.com.ifg.model.entity.Plano;
import br.com.ifg.model.entity.Usuario;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DataInitializer {

    @Inject
    PlanoDAO planoDAO;

    @Inject
    UsuarioDAO usuarioDAO;

    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        inicializarPlanos();
        inicializarAdminPadrao();
    }

    private void inicializarPlanos() {
        if (planoDAO.contarPlanos() == 0) {
            Plano basico = new Plano();
            basico.setNome("Básico");
            basico.setNivel(1);
            basico.setTemAnuncios(true);
            planoDAO.persist(basico);

            Plano padrao = new Plano();
            padrao.setNome("Padrão");
            padrao.setNivel(2);
            padrao.setTemAnuncios(false);
            planoDAO.persist(padrao);

            Plano premium = new Plano();
            premium.setNome("Premium");
            premium.setNivel(3);
            premium.setTemAnuncios(false);
            planoDAO.persist(premium);
        }
    }

    private void inicializarAdminPadrao() {
        if (usuarioDAO.buscarPorEmail("admin@mediaflix.com") == null) {
            Plano premium = planoDAO.buscarPorNome("Premium");
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@mediaflix.com");
            admin.setSenha(BcryptUtil.bcryptHash("admin123"));
            admin.setIsAdmin(true);
            admin.setPlano(premium);
            usuarioDAO.persist(admin);
        }
    }
}