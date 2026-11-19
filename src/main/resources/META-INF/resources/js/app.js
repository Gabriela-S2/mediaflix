const API_URL = '';

function getAuthHeaders() {
    return { 'Content-Type': 'application/json' };
}

async function exigirAutenticacao(somenteAdmin = false) {
    try {
        const res = await fetch(`${API_URL}/usuarios/perfil`, { credentials: 'same-origin' });
        if (!res.ok) {
            window.location.href = '/login';
            return null;
        }
        const perfil = await res.json();
        if (somenteAdmin && !perfil.isAdmin) {
            window.location.href = '/index';
            return null;
        }
        return perfil;
    } catch (error) {
        window.location.href = '/login';
        return null;
    }
}

function inicializarUI() {
    if (!document.getElementById('loading-overlay')) {
        const loader = document.createElement('div');
        loader.id = 'loading-overlay';
        loader.className = 'loading-overlay';
        loader.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(loader);
    }
    if (!document.getElementById('toast-container')) {
        const toasts = document.createElement('div');
        toasts.id = 'toast-container';
        toasts.className = 'toast-container';
        document.body.appendChild(toasts);
    }
}

function setLoading(ativo) {
    const loader = document.getElementById('loading-overlay');
    if (loader) loader.classList.toggle('active', ativo);
}

function mostrarMensagem(texto, tipo = 'sucesso') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${tipo}`;
    toast.innerText = texto;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

document.addEventListener('DOMContentLoaded', () => {
    inicializarUI();
    configurarNavegacao();
    rotearPaginaAtual();
});

function configurarNavegacao() {
    window.addEventListener('scroll', () => {
        const navbar = document.getElementById('navbar');
        if (navbar) {
            if (window.scrollY > 50) navbar.classList.add('black-bg');
            else navbar.classList.remove('black-bg');
        }
    });
}

async function rotearPaginaAtual() {
    const path = window.location.pathname;

    if (path.includes('/login')) {
        configurarLogin();
    } else if (path.includes('/esqueci-minha-senha') || path.includes('recuperar')) {
        configurarRecuperacaoSenha();
    } else if (path.includes('/cadastro')) {
        configurarCadastro();
    } else if (path.includes('/assistir')) {
        const perfil = await exigirAutenticacao();
        if (perfil) carregarDetalhesEProgressoFilme();
    } else if (path.includes('/index') || path === '/' || path === '') {
        const perfil = await exigirAutenticacao();
        if (perfil) {
            carregarAvatarUsuario();
            carregarCatalogoHome();
            configurarMenuAvatar();
            configurarPerfil();
        }
    }
}

function configurarMenuAvatar() {
    const userMenu = document.getElementById('user-menu');
    const btnLogout = document.getElementById('btn-logout');

    if (userMenu) {
        userMenu.addEventListener('click', (e) => {
            e.stopPropagation();
            userMenu.classList.toggle('active');
        });
    }

    document.addEventListener('click', (e) => {
        if (userMenu && !userMenu.contains(e.target)) {
            userMenu.classList.remove('active');
        }
    });

    if (btnLogout) {
        btnLogout.addEventListener('click', async (e) => {
            e.preventDefault();
            await fetch(`${API_URL}/usuarios/logout`, {
                method: 'POST',
                credentials: 'same-origin'
            });
            window.location.href = '/login';
        });
    }
}

function configurarCadastro() {
    const cadastroForm = document.getElementById('cadastro-form');
    if (!cadastroForm) return;

    cadastroForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const planoSelecionado = document.querySelector('input[name="plano"]:checked');
        if (!planoSelecionado) {
            mostrarMensagem('Selecione um plano.', 'erro');
            return;
        }

        const payload = {
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            senha: document.getElementById('password').value,
            planoId: parseInt(planoSelecionado.value)
        };

        setLoading(true);
        try {
            const response = await fetch(`${API_URL}/usuarios`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                mostrarMensagem('Conta criada! Redirecionando...');
                setTimeout(() => window.location.href = '/login', 1000);
            } else {
                const erro = await response.json().catch(() => ({}));
                const msg = erro.detalhes?.length ? erro.detalhes.join(' | ') : (erro.erro || 'Dados inválidos');
                mostrarMensagem(msg, 'erro');
            }
        } finally {
            setLoading(false);
        }
    });
}

function configurarLogin() {
    const loginForm = document.getElementById('login-form');
    if (!loginForm) return;

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const senha = document.getElementById('password').value;

        setLoading(true);
        try {
            const response = await fetch(`${API_URL}/usuarios/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, senha })
            });

            if (response.ok) {
                const data = await response.json();
                window.location.href = data.isAdmin ? '/admin/filmes' : '/index';
            } else {
                const erro = await response.json().catch(() => ({}));
                mostrarMensagem(erro.erro || 'Credenciais inválidas.', 'erro');
            }
        } finally {
            setLoading(false);
        }
    });
}

function fazerLogout() {
    window.location.href = '/login';
}

function configurarRecuperacaoSenha() {
    const formPedir = document.getElementById('form-pedir-codigo');
    const formRedefinir = document.getElementById('form-redefinir-senha');
    let emailRecuperacao = '';

    if (formPedir) {
        formPedir.addEventListener('submit', async (e) => {
            e.preventDefault();
            emailRecuperacao = document.getElementById('recuperar-email').value;
            setLoading(true);
            try {
                const response = await fetch(`${API_URL}/recuperacao/gerar`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: emailRecuperacao })
                });

                if (response.ok) {
                    document.getElementById('step-1-container').style.display = 'none';
                    document.getElementById('step-2-container').style.display = 'block';
                    mostrarMensagem('Código gerado! Verifique os logs do servidor.');
                } else {
                    const err = await response.json().catch(() => ({}));
                    mostrarMensagem(err.erro || 'E-mail não encontrado.', 'erro');
                }
            } finally {
                setLoading(false);
            }
        });
    }

    if (formRedefinir) {
        formRedefinir.addEventListener('submit', async (e) => {
            e.preventDefault();
            const codigo = document.getElementById('codigo-verificacao').value;
            const novaSenha = document.getElementById('nova-senha').value;
            setLoading(true);
            try {
                const response = await fetch(`${API_URL}/recuperacao/redefinir`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: emailRecuperacao, codigo, novaSenha })
                });

                if (response.ok) {
                    mostrarMensagem('Senha redefinida com sucesso!');
                    setTimeout(() => window.location.href = '/login', 1000);
                } else {
                    const err = await response.json().catch(() => ({}));
                    mostrarMensagem(err.erro || 'Código inválido ou expirado.', 'erro');
                }
            } finally {
                setLoading(false);
            }
        });
    }
}

// ==========================================
// CATÁLOGO SEGMENTADO (SUGERIDOS, MINHA LISTA, FILMES, SÉRIES E GÊNEROS)
// ==========================================
async function carregarCatalogoHome() {
    setLoading(true);
    try {
        const response = await fetch(`${API_URL}/filmes/catalogo`, {
            method: 'GET',
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });

        if (response.status === 401 || response.status === 403) {
            fazerLogout();
            return;
        }

        const catalogo = await response.json();

        // Destaque principal: prioriza o primeiro sugerido ou a primeira obra disponível
        const destaque = (catalogo.sugeridos && catalogo.sugeridos[0])
            || (catalogo.filmes && catalogo.filmes[0])
            || (catalogo.series && catalogo.series[0]);
        renderizarDestaque(destaque);

        // 1. Sugeridos (exibido apenas se houver preferências geradas por Like)
        const secSugeridos = document.getElementById('section-sugeridos');
        const listSugeridos = document.getElementById('sugeridos-list');
        const subSugeridos = document.getElementById('sugeridos-subtitulo');
        if (secSugeridos && listSugeridos) {
            if (catalogo.sugeridos && catalogo.sugeridos.length > 0) {
                secSugeridos.style.display = 'block';
                if (subSugeridos && catalogo.generosPreferidos?.length) {
                    subSugeridos.innerText = `(Baseado em: ${catalogo.generosPreferidos.slice(0, 3).join(', ')})`;
                }
                renderizarListaCards(listSugeridos, catalogo.sugeridos, true);
            } else {
                secSugeridos.style.display = 'none';
            }
        }

        // 2. Minha Lista (obras salvas pelo botão '+')
        const secMinhaLista = document.getElementById('section-minha-lista');
        const listMinhaLista = document.getElementById('minha-lista-carousel');
        if (secMinhaLista && listMinhaLista) {
            if (catalogo.minhaLista && catalogo.minhaLista.length > 0) {
                secMinhaLista.style.display = 'block';
                renderizarListaCards(listMinhaLista, catalogo.minhaLista, false);
            } else {
                secMinhaLista.style.display = 'none';
            }
        }

        // 3. Filmes
        const listFilmes = document.getElementById('filmes-list');
        if (listFilmes) {
            renderizarListaCards(listFilmes, catalogo.filmes || [], true, 'Nenhum filme disponível para o seu plano.');
        }

        // 4. Séries
        const listSeries = document.getElementById('series-list');
        if (listSeries) {
            renderizarListaCards(listSeries, catalogo.series || [], true, 'Nenhuma série disponível para o seu plano.');
        }

        // 5. Secções dinâmicas por gênero disponível
        const genreContainer = document.getElementById('genre-sections-container');
        if (genreContainer && catalogo.porGenero) {
            genreContainer.innerHTML = '';
            Object.entries(catalogo.porGenero).forEach(([genero, itens]) => {
                if (!itens || itens.length === 0) return;

                const row = document.createElement('div');
                row.className = 'media-row';

                const h3 = document.createElement('h3');
                h3.innerText = genero;
                row.appendChild(h3);

                const carousel = document.createElement('div');
                carousel.className = 'media-carousel';
                renderizarListaCards(carousel, itens, false);
                row.appendChild(carousel);

                genreContainer.appendChild(row);
            });
        }
    } catch (error) {
        console.error('Erro ao carregar catálogo:', error);
    } finally {
        setLoading(false);
    }
}

function renderizarDestaque(obraDestaque) {
    const tituloEl = document.getElementById('featured-title');
    const descEl = document.getElementById('featured-desc');
    const heroSection = document.getElementById('hero-section');
    const btnPlay = document.querySelector('.hero-buttons .btn-play');

    if (!obraDestaque) {
        if (tituloEl) tituloEl.innerText = 'Nenhum título disponível';
        if (descEl) descEl.innerText = 'Não existem filmes ou séries cadastrados para o seu plano atual.';
        return;
    }

    if (tituloEl) tituloEl.innerText = obraDestaque.titulo;
    if (descEl) descEl.innerText = obraDestaque.descricao || '';
    const banner = obraDestaque.caminhoImagemHorizontal || obraDestaque.caminhoImagemVertical;
    if (heroSection && banner) {
        heroSection.style.backgroundImage = `linear-gradient(to right, rgba(20,20,20,0.85), rgba(20,20,20,0.2)), url('${banner}')`;
    }
    if (btnPlay) {
        btnPlay.onclick = () => window.location.href = `/assistir?id=${obraDestaque.id}`;
    }
}

function renderizarListaCards(container, itens, usarPosterVertical = false, mensagemVazia = '') {
    container.innerHTML = '';
    if (!itens || itens.length === 0) {
        if (mensagemVazia) {
            container.innerHTML = `<p style="color:#888; font-size:0.95rem;">${mensagemVazia}</p>`;
        }
        return;
    }

    itens.forEach(obra => {
        const card = document.createElement('div');
        card.className = usarPosterVertical ? 'media-item-vertical' : 'media-item';

        const imagem = usarPosterVertical
            ? (obra.caminhoImagemVertical || obra.caminhoImagemHorizontal)
            : (obra.caminhoImagemHorizontal || obra.caminhoImagemVertical);

        if (imagem) {
            card.style.backgroundImage = `url('${imagem}')`;
        }

        const labelTipo = obra.tipo === 'SERIE' ? 'Série' : 'Filme';
        if (usarPosterVertical) {
            card.innerHTML = `<span class="card-badge">${labelTipo}</span><h4>${obra.titulo}</h4>`;
        } else {
            card.innerHTML = `<h4>${obra.titulo}</h4>`;
        }

        card.addEventListener('click', () => window.location.href = `/assistir?id=${obra.id}`);
        container.appendChild(card);
    });
}

// ==========================================
// TELA DE ASSISTIR: PLAYER, MINHA LISTA (+) E LIKE (👍)
// ==========================================
function atualizarVisualBotaoLista(btn, naMinhaLista) {
    if (!btn) return;
    btn.classList.toggle('active', naMinhaLista);
    btn.innerText = naMinhaLista ? '✓' : '+';
    btn.title = naMinhaLista ? 'Remover da Minha Lista' : 'Adicionar à Minha Lista';
}

function atualizarVisualBotaoLike(btn, curtido) {
    if (!btn) return;
    btn.classList.toggle('active', curtido);
    btn.title = curtido ? 'Você curtiu esta obra (clique para remover)' : 'Gostei (melhora suas recomendações)';
}

async function carregarDetalhesEProgressoFilme() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');
    if (!id) return;

    setLoading(true);
    try {
        const response = await fetch(`${API_URL}/filmes/${id}`, {
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });

        if (response.status === 401) {
            fazerLogout();
            return;
        }
        if (response.status === 403 || !response.ok) {
            alert('Não tem permissão para assistir a este título ou a obra não foi encontrada.');
            window.location.href = '/index';
            return;
        }

        const obra = await response.json();
        const titleEl = document.getElementById('video-title');
        const descEl = document.getElementById('video-description');
        const genreEl = document.getElementById('video-genre');
        const tipoEl = document.getElementById('video-tipo');
        const formatoTextoEl = document.getElementById('video-formato-texto');

        const labelTipo = obra.tipo === 'SERIE' ? 'Série' : 'Filme';
        if (titleEl) titleEl.innerText = obra.titulo;
        if (descEl) descEl.innerText = obra.descricao || 'Sem descrição disponível.';
        if (genreEl) genreEl.innerText = obra.genero || 'Não informado';
        if (tipoEl) tipoEl.innerText = labelTipo;
        if (formatoTextoEl) formatoTextoEl.innerText = labelTipo;

        // Estado inicial de Minha Lista (+) e Like (👍)
        const btnLista = document.getElementById('btn-minha-lista');
        const btnLike = document.getElementById('btn-like');

        const interacaoRes = await fetch(`${API_URL}/filmes/${id}/interacao`, {
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });
        if (interacaoRes.ok) {
            const status = await interacaoRes.json();
            atualizarVisualBotaoLista(btnLista, status.naMinhaLista);
            atualizarVisualBotaoLike(btnLike, status.curtido);
        }

        if (btnLista) {
            btnLista.addEventListener('click', async () => {
                const res = await fetch(`${API_URL}/filmes/${id}/lista`, {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    credentials: 'same-origin'
                });
                if (res.ok) {
                    const status = await res.json();
                    atualizarVisualBotaoLista(btnLista, status.naMinhaLista);
                    mostrarMensagem(
                        status.naMinhaLista
                            ? 'Adicionado à sua Minha Lista!'
                            : 'Removido da sua Minha Lista.'
                    );
                }
            });
        }

        if (btnLike) {
            btnLike.addEventListener('click', async () => {
                const res = await fetch(`${API_URL}/filmes/${id}/like`, {
                    method: 'POST',
                    headers: getAuthHeaders(),
                    credentials: 'same-origin'
                });
                if (res.ok) {
                    const status = await res.json();
                    atualizarVisualBotaoLike(btnLike, status.curtido);
                    mostrarMensagem(
                        status.curtido
                            ? 'Curtiu! Suas sugestões no catálogo foram atualizadas.'
                            : 'Curtida removida.'
                    );
                }
            });
        }

        // Player e histórico de reprodução
        const videoEl = document.querySelector('.video-player');
        const btnReplay = document.getElementById('btn-replay');

        if (videoEl) {
            if (btnReplay) {
                btnReplay.addEventListener('click', () => {
                    videoEl.currentTime = 0;
                    videoEl.play();
                });
            }

            const histRes = await fetch(`${API_URL}/historico/${id}`, {
                headers: getAuthHeaders(),
                credentials: 'same-origin'
            });
            if (histRes.ok) {
                const hist = await histRes.json();
                if (hist.tempoAssistido > 0) {
                    videoEl.currentTime = hist.tempoAssistido;
                    mostrarMensagem(`Retomando reprodução aos ${hist.tempoAssistido}s`);
                }
            }

            let ultimoSegundoSalvo = 0;
            videoEl.addEventListener('timeupdate', async () => {
                const segundoAtual = Math.floor(videoEl.currentTime);
                if (segundoAtual > 0 && segundoAtual - ultimoSegundoSalvo >= 5) {
                    ultimoSegundoSalvo = segundoAtual;
                    await fetch(`${API_URL}/historico`, {
                        method: 'POST',
                        headers: getAuthHeaders(),
                        credentials: 'same-origin',
                        body: JSON.stringify({ filmeId: parseInt(id), tempoAssistido: segundoAtual })
                    });
                }
            });
        }
    } finally {
        setLoading(false);
    }
}

async function carregarAvatarUsuario() {
    try {
        const res = await fetch(`${API_URL}/usuarios/perfil`, {
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });
        if (res.ok) {
            const perfil = await res.json();
            if (perfil.caminhoFoto) {
                const urlComCacheBust = `${perfil.caminhoFoto}?t=${Date.now()}`;
                const avatarNavbar = document.getElementById('avatar-btn');
                const avatarPreview = document.getElementById('perfil-preview');
                if (avatarNavbar) avatarNavbar.src = urlComCacheBust;
                if (avatarPreview) avatarPreview.src = urlComCacheBust;
            }
        }
    } catch (error) {
        console.error('Erro ao carregar avatar do usuário:', error);
    }
}

function configurarPerfil() {
    const btnAbrir = document.getElementById('btn-abrir-perfil');
    const btnFechar = document.getElementById('btn-fechar-perfil');
    const modal = document.getElementById('modal-perfil');
    const formPerfil = document.getElementById('form-perfil');
    const inputFoto = document.getElementById('perfil-foto-input');
    const previewFoto = document.getElementById('perfil-preview');

    if (inputFoto && previewFoto) {
        inputFoto.addEventListener('change', () => {
            const file = inputFoto.files[0];
            if (file) {
                previewFoto.src = URL.createObjectURL(file);
            }
        });
    }

    if (btnAbrir && modal) {
        btnAbrir.addEventListener('click', async (e) => {
            e.preventDefault();
            setLoading(true);
            try {
                const res = await fetch(`${API_URL}/usuarios/perfil`, {
                    headers: getAuthHeaders(),
                    credentials: 'same-origin'
                });
                if (res.ok) {
                    const perfil = await res.json();
                    document.getElementById('perfil-username').value = perfil.username || '';
                    if (perfil.plano) document.getElementById('perfil-plano').value = perfil.plano.id;
                    if (perfil.caminhoFoto && previewFoto) {
                        previewFoto.src = `${perfil.caminhoFoto}?t=${Date.now()}`;
                    }
                    if (inputFoto) inputFoto.value = '';
                    modal.classList.add('active');
                }
            } finally {
                setLoading(false);
            }
        });
    }

    if (btnFechar && modal) {
        btnFechar.addEventListener('click', () => modal.classList.remove('active'));
    }

    if (formPerfil) {
        formPerfil.addEventListener('submit', async (e) => {
            e.preventDefault();
            setLoading(true);
            try {
                const payload = {
                    username: document.getElementById('perfil-username').value,
                    caminhoFoto: null,
                    planoId: parseInt(document.getElementById('perfil-plano').value)
                };

                const resPerfil = await fetch(`${API_URL}/usuarios/perfil`, {
                    method: 'PUT',
                    headers: getAuthHeaders(),
                    credentials: 'same-origin',
                    body: JSON.stringify(payload)
                });

                if (!resPerfil.ok) {
                    const err = await resPerfil.json().catch(() => ({}));
                    mostrarMensagem(err.erro || 'Erro ao atualizar perfil.', 'erro');
                    return;
                }

                let perfilAtualizado = await resPerfil.json();

                if (inputFoto && inputFoto.files.length > 0) {
                    const formData = new FormData();
                    formData.append('foto', inputFoto.files[0]);

                    const resFoto = await fetch(`${API_URL}/usuarios/perfil/foto`, {
                        method: 'POST',
                        credentials: 'same-origin',
                        body: formData
                    });

                    if (!resFoto.ok) {
                        const errFoto = await resFoto.json().catch(() => ({}));
                        mostrarMensagem(errFoto.erro || 'Erro ao enviar foto de perfil.', 'erro');
                        return;
                    }
                    perfilAtualizado = await resFoto.json();
                }

                if (perfilAtualizado.caminhoFoto) {
                    const novaUrl = `${perfilAtualizado.caminhoFoto}?t=${Date.now()}`;
                    const avatarNavbar = document.getElementById('avatar-btn');
                    if (avatarNavbar) avatarNavbar.src = novaUrl;
                }

                mostrarMensagem('Perfil atualizado com sucesso!');
                modal.classList.remove('active');
                carregarCatalogoHome();
            } finally {
                setLoading(false);
            }
        });
    }
}