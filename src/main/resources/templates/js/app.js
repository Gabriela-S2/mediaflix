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

    if (path.includes('login.html')) {
        configurarLogin();
    } else if (path.includes('recuperar-senha.html')) {
        configurarRecuperacaoSenha();
    } else if (path.includes('index.html') || path === '/' || path === '') {
        verificarAutenticacao();
        carregarFilmes();
    }
}

// ==========================================
// 1. LÓGICA DE LOGIN E JWT
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
                // O Quarkus deve retornar o token (ex: { "token": "eyJhbG..." })
                const data = await response.json();
                localStorage.setItem('mediaflix_token', data.token);
                window.location.href = 'index.html';
            } else {
                alert('Credenciais inválidas. Tente novamente.');
            }
        } catch (error) {
            console.error('Erro no login:', error);
            alert('Erro ao conectar ao servidor.');
        }
    });
}

function verificarAutenticacao() {
    const token = localStorage.getItem('mediaflix_token');
    if (!token) {
        // Se não tem token (não está logado), redireciona para o login
        window.location.href = 'login.html';
    }
}

function fazerLogout() {
    localStorage.removeItem('mediaflix_token');
    window.location.href = 'login.html';
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
                    window.location.href = 'login.html';
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
// 3. LÓGICA DE FILMES (COM AUTORIZAÇÃO)
// ==========================================
async function carregarFilmes() {
    const trendingList = document.getElementById('trending-list');
    if (!trendingList) return;

    const token = localStorage.getItem('mediaflix_token');

    try {
        // Envia o JWT no cabeçalho para provar quem é e qual o seu plano
        const response = await fetch(`${API_URL}/filmes`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

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

        // Puxa o caminho da imagem que veio do banco de dados (ex: "../midia/horizontal_img/matrix.jpg")
        item.style.backgroundImage = `url(${filme.caminhoImagemHorizontal})`;
        item.innerHTML = `<h4>${filme.titulo}</h4>`;

        item.addEventListener('click', () => {
            // Navega para a página de assistir, passando o ID na URL
            window.location.href = `watch.html?id=${filme.id}`;
        });

        trendingList.appendChild(item);
    });
}