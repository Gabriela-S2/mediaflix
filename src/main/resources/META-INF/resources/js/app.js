const API_URL = 'http://localhost:8080'; // Ajuste se o Quarkus rodar noutra porta

document.addEventListener('DOMContentLoaded', () => {
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
    } else if (path.includes('/index') || path === '/' || path === '') {
        carregarFilmes();
        configurarMenuAvatar();
    }
}

// ==========================================
// LÓGICA DO MENU SUSPENSO DO AVATAR
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
        btnLogout.addEventListener('click', (e) => {
            e.preventDefault();
            fazerLogout();
        });
    }
}

// ==========================================
// LÓGICA DE CADASTRO
// ==========================================
function configurarCadastro() {
    const cadastroForm = document.getElementById('cadastro-form');
    if (!cadastroForm) return;

    cadastroForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('username').value;
        const email = document.getElementById('email').value;
        const senha = document.getElementById('password').value;
        const planoSelecionado = document.querySelector('input[name="plano"]:checked');

        if (!planoSelecionado) {
            alert('Por favor, selecione um plano.');
            return;
        }

        const payload = {
            username: username,
            email: email,
            senha: senha,
            planoId: parseInt(planoSelecionado.value)
        };

        try {
            const response = await fetch(`${API_URL}/usuarios`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                alert('Conta criada com sucesso! Faça login para continuar.');
                window.location.href = '/login';
            } else {
                const erro = await response.json();
                alert(`Erro ao criar conta: ${erro.message || 'Dados inválidos'}`);
            }
        } catch (error) {
            console.error('Erro no cadastro:', error);
            alert('Erro ao conectar ao servidor.');
        }
    });
}

// ==========================================
// 1. LÓGICA DE LOGIN E JWT (REDIRECIONAMENTO DINÂMICO)
// ==========================================
function configurarLogin() {
    const loginForm = document.getElementById('login-form');
    if (!loginForm) return;

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const senha = document.getElementById('password').value;

        try {
            const response = await fetch(`${API_URL}/usuarios/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, senha })
            });

            if (response.ok) {
                // Lê o corpo da resposta para descobrir o tipo de utilizador
                const data = await response.json();

                if (data.isAdmin === true) {
                    window.location.href = '/admin/filmes';
                } else {
                    window.location.href = '/index';
                }
            } else {
                alert('Credenciais inválidas. Tente novamente.');
            }
        } catch (error) {
            console.error('Erro no login:', error);
            alert('Erro ao conectar ao servidor.');
        }
    });
}

function fazerLogout() {
    // Como o cookie é HttpOnly (proteção XSS), o JavaScript não tem permissão para o apagar.
    // O ideal será criares depois um endpoint /usuarios/logout no Quarkus que apague o cookie.
    window.location.href = '/login';
}

// ==========================================
// 2. LÓGICA DE RECUPERAÇÃO DE SENHA
// ==========================================
function configurarRecuperacaoSenha() {
    const formPedir = document.getElementById('form-pedir-codigo');
    const formRedefinir = document.getElementById('form-redefinir-senha');
    let emailRecuperacao = '';

    if (formPedir) {
        formPedir.addEventListener('submit', async (e) => {
            e.preventDefault();
            emailRecuperacao = document.getElementById('recuperar-email').value;

            try {
                const response = await fetch(`${API_URL}/recuperacao/gerar`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: emailRecuperacao })
                });

                if (response.ok) {
                    document.getElementById('step-1-container').style.display = 'none';
                    document.getElementById('step-2-container').style.display = 'block';
                } else {
                    alert('Erro ao solicitar código. Verifique se o e-mail está correto.');
                }
            } catch (error) {
                console.error('Erro:', error);
            }
        });
    }

    if (formRedefinir) {
        formRedefinir.addEventListener('submit', async (e) => {
            e.preventDefault();
            const codigo = document.getElementById('codigo-verificacao').value;
            const novaSenha = document.getElementById('nova-senha').value;

            try {
                const response = await fetch(`${API_URL}/recuperacao/redefinir`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email: emailRecuperacao, codigo, novaSenha })
                });

                if (response.ok) {
                    alert('Senha redefinida com sucesso! Faça o login.');
                    window.location.href = '/login';
                } else {
                    alert('Código inválido ou expirado.');
                }
            } catch (error) {
                console.error('Erro:', error);
            }
        });
    }
}

// ==========================================
// 3. LÓGICA DE FILMES (COM AUTORIZAÇÃO VIA COOKIE)
// ==========================================
async function carregarFilmes() {
    const trendingList = document.getElementById('trending-list');
    if (!trendingList) return;

    try {
        // O navegador anexa o Cookie "jwt-token" automaticamente!
        const response = await fetch(`${API_URL}/filmes`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        // Se o Quarkus rejeitar o cookie (inválido ou expirado), redireciona.
        if (response.status === 401 || response.status === 403) {
            alert('Sessão expirada ou acesso negado.');
            fazerLogout();
            return;
        }

        const filmes = await response.json();
        renderizarFilmes(filmes);

    } catch (error) {
        console.error("Erro ao buscar filmes:", error);
        trendingList.innerHTML = '<p>Erro ao carregar a lista de filmes.</p>';
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

        item.addEventListener('click', () => {
            window.location.href = `/assistir?id=${filme.id}`;
        });

        trendingList.appendChild(item);
    });
}