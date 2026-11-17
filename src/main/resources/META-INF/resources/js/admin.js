const API_URL = 'http://localhost:8080/filmes';
let listaFilmesGlobal = [];

document.addEventListener('DOMContentLoaded', () => {
    carregarFilmesAdmin();

    document.getElementById('filmeForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        salvarFilme();
    });
});

async function carregarFilmesAdmin() {
    try {
        const response = await fetch(`${API_URL}/todos`);
        if (response.status === 401 || response.status === 403) {
            alert('Acesso negado. Apenas administradores podem aceder a esta página.');
            window.location.href = '/login';
            return;
        }

        listaFilmesGlobal = await response.json();
        renderizarTabela(listaFilmesGlobal);
    } catch (error) {
        console.error('Erro ao carregar filmes:', error);
    }
}

function renderizarTabela(filmes) {
    const tbody = document.getElementById('tabela-filmes');
    tbody.innerHTML = '';

    filmes.forEach(filme => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${filme.id}</td>
            <td>${filme.titulo}</td>
            <td>${filme.genero}</td>
            <td>Nível ${filme.planoId}</td>
            <td>
                <button class="btn-edit" onclick="prepararEdicao(${filme.id})">Editar</button>
                <button class="btn-delete" onclick="deletarFilme(${filme.id})">Excluir</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function filtrarFilmes() {
    const termo = document.getElementById('search-bar').value.toLowerCase();
    const filmesFiltrados = listaFilmesGlobal.filter(f => f.titulo.toLowerCase().includes(termo));
    renderizarTabela(filmesFiltrados);
}

async function salvarFilme() {
    const id = document.getElementById('filmeId').value;

    // O backend espera um objeto Plano aninhado: { plano: { id: X } }
    const payload = {
        titulo: document.getElementById('titulo').value,
        descricao: document.getElementById('descricao').value,
        genero: document.getElementById('genero').value,
        caminhoImagemHorizontal: document.getElementById('imgHoriz').value,
        caminhoImagemVertical: document.getElementById('imgVert').value,
        plano: {
            id: parseInt(document.getElementById('planoId').value)
        }
    };

    const url = id ? `${API_URL}/${id}` : API_URL;
    const metodo = id ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: metodo,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert(id ? 'Filme atualizado com sucesso!' : 'Filme criado com sucesso!');
            limparFormulario();
            carregarFilmesAdmin();
        } else {
            alert('Erro ao guardar o filme.');
        }
    } catch (error) {
        console.error('Erro:', error);
    }
}

function prepararEdicao(id) {
    const filme = listaFilmesGlobal.find(f => f.id === id);
    if (!filme) return;

    document.getElementById('filmeId').value = filme.id;
    document.getElementById('titulo').value = filme.titulo;
    document.getElementById('descricao').value = filme.descricao;
    document.getElementById('genero').value = filme.genero;
    document.getElementById('imgHoriz').value = filme.caminhoImagemHorizontal || '';
    document.getElementById('imgVert').value = filme.caminhoImagemVertical || '';
    document.getElementById('planoId').value = filme.planoId;

    document.getElementById('btnSalvar').innerText = 'Atualizar Filme';
    window.scrollTo(0, 0);
}

async function deletarFilme(id) {
    if (!confirm('Tem a certeza que deseja excluir este filme permanentemente?')) return;

    try {
        const response = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
        if (response.ok) {
            carregarFilmesAdmin();
        } else {
            alert('Erro ao excluir o filme.');
        }
    } catch (error) {
        console.error('Erro:', error);
    }
}

function limparFormulario() {
    document.getElementById('filmeForm').reset();
    document.getElementById('filmeId').value = '';
    document.getElementById('btnSalvar').innerText = 'Salvar Filme';
}