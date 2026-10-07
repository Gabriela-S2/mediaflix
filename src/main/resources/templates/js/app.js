// Efeito da barra de navegação ao rolar a página
window.addEventListener('scroll', () => {
    const navbar = document.getElementById('navbar');
    if (window.scrollY > 50) {
        navbar.classList.add('black-bg');
    } else {
        navbar.classList.remove('black-bg');
    }
});

// Dados falsos (Mock) para testar enquanto a API do Quarkus não está rodando
const mockMediaData = [
    { id: 1, title: "Stranger Things", img: "https://images.unsplash.com/photo-1618666012174-83b441c0bc76?w=600&q=80" },
    { id: 2, title: "Cyberpunk", img: "https://images.unsplash.com/photo-1510511459019-5efa7ae5ca6a?w=600&q=80" },
    { id: 3, title: "A Inteligência", img: "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=600&q=80" },
    { id: 4, title: "Matrix", img: "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&q=80" },
    { id: 5, title: "O Código", img: "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=600&q=80" },
    { id: 6, title: "Hackers", img: "https://images.unsplash.com/photo-1526374865366-af181045b630?w=600&q=80" }
];

function initApp() {
    // 1. Configurar o destaque principal
    const heroSection = document.getElementById('hero-section');
    const heroTitle = document.getElementById('featured-title');
    const heroDesc = document.getElementById('featured-desc');

    heroSection.style.backgroundImage = `url('https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=1600&q=80')`;
    heroTitle.innerText = "Projeto MediaFlix";
    heroDesc.innerText = "Uma plataforma de streaming construída para fins acadêmicos. Este frontend conecta-se a uma API REST robusta desenvolvida em Quarkus e Java.";

    // 2. Renderizar a lista de mídias
    renderMediaList(mockMediaData);

    /*
    ========================================================
    INTEGRAÇÃO COM O SEU BACKEND QUARKUS (Descomente depois)
    ========================================================
    Substitua a chamada da função 'renderMediaList(mockMediaData)'
    acima pelo bloco abaixo quando o seu Quarkus (localhost:8080) estiver rodando
    e retornando JSON através de uma rota, por exemplo: /api/movies

    fetch('http://localhost:8080/api/movies')
        .then(response => response.json())
        .then(data => {
            renderMediaList(data);
        })
        .catch(error => {
            console.error("Erro ao conectar com o Quarkus:", error);
            // Fallback para mock data se o servidor estiver offline
            renderMediaList(mockMediaData);
        });
    */
}

// Função para gerar o HTML do carrossel
function renderMediaList(movies) {
    const trendingList = document.getElementById('trending-list');
    trendingList.innerHTML = ''; // Limpa o estado de "carregando"

    movies.forEach(movie => {
        const item = document.createElement('div');
        item.className = 'media-item';

        // Aqui assumimos que sua API vai retornar uma propriedade "img" ou "coverUrl"
        item.style.backgroundImage = `url(${movie.img})`;

        item.innerHTML = `<h4>${movie.title}</h4>`;

        // Exemplo de interação de clique
        item.addEventListener('click', () => {
            alert(`Você clicou em: ${movie.title}\nAqui você abriria um modal ou redirecionaria para o vídeo.`);
        });

        trendingList.appendChild(item);
    });
}

// Iniciar a aplicação quando o HTML carregar
document.addEventListener('DOMContentLoaded', initApp);

// Verifica se estamos na página de login
const loginForm = document.getElementById('login-form');
if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
        e.preventDefault(); // Evita recarregar a página
        const email = document.getElementById('email').value;

        // Simulação de login - Em produção, aqui iria um POST para o Quarkus
        alert(`Simulação de Login para: ${email}\n\nConectando à API Quarkus... Sucesso! Redirecionando...`);

        // Redireciona para a página principal
        window.location.href = 'index.html';
    });

    const toggleSignup = document.getElementById('toggleSignup');
    if(toggleSignup) {
        toggleSignup.addEventListener('click', (e) => {
            e.preventDefault();
            alert("Aqui você exibiria os campos de Cadastro (Nome, Confirmar Senha, etc).");
        });
    }
}

// Atualizar o clique dos filmes da página inicial (index.html) para ir para a página de vídeo
// Se quiser testar a navegação, substitua o evento de clique na função `renderMediaList` no app.js por:
/*
item.addEventListener('click', () => {
    // Redireciona para a página do vídeo
    window.location.href = 'watch.html?id=' + movie.id;
});
*/