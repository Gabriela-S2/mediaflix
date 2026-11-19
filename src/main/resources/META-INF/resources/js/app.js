const API_URL = '';

function getAuthHeaders() {
    return {
        'Content-Type': 'application/json'
    };
}

async function exigirAutenticacao(somenteAdmin = false) {
    try {
        const res = await fetch(`${API_URL}/usuarios/perfil`, {
            credentials: 'same-origin'
        });

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

// ==========================================
// FEEDBACK ASSÍNCRONO (SPINNER E TOASTS - REQ. 12)
// ==========================================
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
    setTimeout(() => toast.remove(), 4000);
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

function rotearPaginaAtual() {
    const path = window.location.pathname;

    if (path.includes('/login')) {
        configurarLogin();
    } else if (path.includes('/esqueci-minha-senha') || path.includes('recuperar')) {
        configurarRecuperacaoSenha();
    } else if (path.includes('/cadastro')) {
        configurarCadastro();
    } else if (path.includes('/assistir')) {
        if (exigirAutenticacao()) carregarDetalhesEProgressoFilme();
    } else if (path.includes('/index') || path === '/' || path === '') {
        if (exigirAutenticacao()) {
            carregarAvatarUsuario();
            carregarFilmes();
            configurarMenuAvatar();
            configurarPerfil();
        }

    }
}

// ==========================================
// MENU AVATAR E EDIÇÃO DE PERFIL (REQ. 3)
// ==========================================
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

function configurarPerfil() {
    const btnAbrir = document.getElementById('btn-abrir-perfil');
    const btnFechar = document.getElementById('btn-fechar-perfil');
    const modal = document.getElementById('modal-perfil');
    const formPerfil = document.getElementById('form-perfil');

    if (btnAbrir && modal) {
        btnAbrir.addEventListener('click', async (e) => {
            e.preventDefault();
            setLoading(true);
            try {
                const res = await fetch(`${API_URL}/usuarios/perfil`, { headers: getAuthHeaders() });
                if (res.ok) {
                    const perfil = await res.json();
                    document.getElementById('perfil-username').value = perfil.username || '';
                    document.getElementById('perfil-foto').value = perfil.caminhoFoto || '';
                    if (perfil.plano) document.getElementById('perfil-plano').value = perfil.plano.id;
                    if (perfil.caminhoFoto) document.getElementById('avatar-btn').src = perfil.caminhoFoto;
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
                    caminhoFoto: document.getElementById('perfil-foto').value,
                    planoId: parseInt(document.getElementById('perfil-plano').value)
                };
                const res = await fetch(`${API_URL}/usuarios/perfil`, {
                    method: 'PUT',
                    headers: getAuthHeaders(),
                    body: JSON.stringify(payload)
                });
                if (res.ok) {
                    mostrarMensagem('Perfil e plano atualizados com sucesso!');
                    modal.classList.remove('active');
                    carregarFilmes();
                } else {
                    const err = await res.json().catch(() => ({}));
                    mostrarMensagem(err.erro || 'Erro ao atualizar perfil.', 'erro');
                }
            } finally {
                setLoading(false);
            }
        });
    }
}

// ==========================================
// CADASTRO E LOGIN COM TOKEN NO HEADER (REQ. 16)
// ==========================================
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
    localStorage.removeItem('jwt-token');
    window.location.href = '/login';
}

// ==========================================
// RECUPERAÇÃO DE SENHA
// ==========================================
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
// CATÁLOGO E HISTÓRICO DE REPRODUÇÃO (REQ. 4)
// ==========================================
async function carregarFilmes() {
    const trendingList = document.getElementById('trending-list');
    if (!trendingList) return;

    setLoading(true);
    try {
        const response = await fetch(`${API_URL}/filmes`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            fazerLogout();
            return;
        }

        const filmes = await response.json();
        renderizarDestaque(filmes[0]);
        renderizarFilmes(filmes);
    } catch (error) {
        trendingList.innerHTML = '<p>Erro ao carregar a lista de filmes.</p>';
    } finally {
        setLoading(false);
    }
}

function renderizarDestaque(filmeDestaque) {
    const tituloEl = document.getElementById('featured-title');
    const descEl = document.getElementById('featured-desc');
    const heroSection = document.getElementById('hero-section');
    const btnPlay = document.querySelector('.hero-buttons .btn-play');

    if (!filmeDestaque) {
        if (tituloEl) tituloEl.innerText = 'Nenhum título disponível';
        if (descEl) descEl.innerText = 'Não existem filmes cadastrados para o seu plano atual.';
        return;
    }

    if (tituloEl) tituloEl.innerText = filmeDestaque.titulo;
    if (descEl) descEl.innerText = filmeDestaque.descricao || '';
    if (heroSection && filmeDestaque.caminhoImagemHorizontal) {
        heroSection.style.backgroundImage = `linear-gradient(to right, rgba(20,20,20,0.85), rgba(20,20,20,0.2)), url('${filmeDestaque.caminhoImagemHorizontal}')`;
    }
    if (btnPlay) {
        btnPlay.onclick = () => window.location.href = `/assistir?id=${filmeDestaque.id}`;
    }
}

function renderizarFilmes(filmes) {
    const trendingList = document.getElementById('trending-list');
    trendingList.innerHTML = '';

    filmes.forEach(filme => {
        const item = document.createElement('div');
        item.className = 'media-item';
        item.style.backgroundImage = `url(${filme.caminhoImagemHorizontal})`;
        item.innerHTML = `<h4>${filme.titulo}</h4>`;
        item.addEventListener('click', () => window.location.href = `/assistir?id=${filme.id}`);
        trendingList.appendChild(item);
    });
}

async function carregarDetalhesEProgressoFilme() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');
    if (!id) return;

    setLoading(true);
    try {
        const response = await fetch(`${API_URL}/filmes/${id}`, { headers: getAuthHeaders() });
        if (response.status === 401) {
            fazerLogout();
            return;
        }
        if (response.status === 403 || !response.ok) {
            alert('Não tem permissão para assistir a este título ou o filme não foi encontrado.');
            window.location.href = '/index';
            return;
        }

        const filme = await response.json();
        const titleEl = document.getElementById('video-title');
        const descEl = document.getElementById('video-description');
        const genreEl = document.getElementById('video-genre');

        if (titleEl) titleEl.innerText = filme.titulo;
        if (descEl) descEl.innerText = filme.descricao || 'Sem descrição disponível.';
        if (genreEl) genreEl.innerText = filme.genero || 'Não informado';

        // Retoma o tempo assistido salvo no banco (Caso de Uso 2)
        const videoEl = document.querySelector('.video-player');
        if (videoEl) {
            const histRes = await fetch(`${API_URL}/historico/${id}`, { headers: getAuthHeaders() });
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
        const res = await fetch(`${API_URL}/usuarios/perfil`, { headers: getAuthHeaders() });
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

    // Pré-visualização local imediata antes de enviar
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
                const res = await fetch(`${API_URL}/usuarios/perfil`, { headers: getAuthHeaders() });
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
                // 1. Atualiza dados textuais (username e plano)
                const payload = {
                    username: document.getElementById('perfil-username').value,
                    caminhoFoto: null,
                    planoId: parseInt(document.getElementById('perfil-plano').value)
                };

                const resPerfil = await fetch(`${API_URL}/usuarios/perfil`, {
                    method: 'PUT',
                    headers: getAuthHeaders(),
                    body: JSON.stringify(payload)
                });

                if (!resPerfil.ok) {
                    const err = await resPerfil.json().catch(() => ({}));
                    mostrarMensagem(err.erro || 'Erro ao atualizar perfil.', 'erro');
                    return;
                }

                let perfilAtualizado = await resPerfil.json();

                // 2. Se o usuário selecionou uma imagem, envia via multipart/form-data
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

                // 3. Aplica a nova foto imediatamente na barra superior
                if (perfilAtualizado.caminhoFoto) {
                    const novaUrl = `${perfilAtualizado.caminhoFoto}?t=${Date.now()}`;
                    const avatarNavbar = document.getElementById('avatar-btn');
                    if (avatarNavbar) avatarNavbar.src = novaUrl;
                }

                mostrarMensagem('Perfil atualizado com sucesso!');
                modal.classList.remove('active');
                carregarFilmes();
            } finally {
                setLoading(false);
            }
        });
    }
}