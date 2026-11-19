const API_URL = '/filmes';
let listaFilmesGlobal = [];

function getAuthHeaders() {
    return {
        'Content-Type': 'application/json'
    };
}

async function verificarSessaoAdmin() {
    try {
        const response = await fetch('/usuarios/perfil', {
            credentials: 'same-origin'
        });

        if (!response.ok) {
            window.location.href = '/login';
            return false;
        }

        const perfil = await response.json();
        if (!perfil.isAdmin) {
            window.location.href = '/index';
            return false;
        }

        return true;
    } catch (error) {
        console.error('Erro ao verificar sessão:', error);
        window.location.href = '/login';
        return false;
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    const autenticado = await verificarSessaoAdmin();
    if (!autenticado) return;

    await carregarFilmesAdmin();

    const filmeForm = document.getElementById('filmeForm');
    if (filmeForm) {
        filmeForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await salvarFilme();
        });
    }

    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', async (e) => {
            e.preventDefault();
            await fetch('/usuarios/logout', {
                method: 'POST',
                credentials: 'same-origin'
            });
            window.location.href = '/login';
        });
    }
});

async function carregarFilmesAdmin() {
    try {
        const response = await fetch(`${API_URL}/todos`, {
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });

        if (response.status === 401 || response.status === 403) {
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

    const payload = {
        titulo: document.getElementById('titulo').value,
        descricao: document.getElementById('descricao').value,
        genero: document.getElementById('genero').value,
        caminhoImagemHorizontal: document.getElementById('imgHoriz').value,
        caminhoImagemVertical: document.getElementById('imgVert').value,
        planoId: parseInt(document.getElementById('planoId').value)
    };

    const url = id ? `${API_URL}/${id}` : API_URL;
    const metodo = id ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: metodo,
            headers: getAuthHeaders(),
            credentials: 'same-origin',
            body: JSON.stringify(payload)
        });

        if (response.status === 401 || response.status === 403) {
            window.location.href = '/login';
            return;
        }

        if (response.ok) {
            alert(id ? 'Filme atualizado com sucesso!' : 'Filme criado com sucesso!');
            limparFormulario();
            carregarFilmesAdmin();
        } else {
            const err = await response.json().catch(() => ({}));
            alert(err.detalhes ? err.detalhes.join('\n') : (err.erro || 'Erro ao guardar o filme.'));
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
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });

        if (response.status === 401 || response.status === 403) {
            window.location.href = '/login';
            return;
        }

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