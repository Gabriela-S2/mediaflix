const API_URL = '/filmes';
let listaFilmesGlobal = [];

function getAuthHeaders() {
    return { 'Content-Type': 'application/json' };
}

async function verificarSessaoAdmin() {
    try {
        const response = await fetch('/usuarios/perfil', { credentials: 'same-origin' });
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
        window.location.href = '/login';
        return false;
    }
}

function configurarPreviaArquivo(inputId, previewId, labelId) {
    const input = document.getElementById(inputId);
    const preview = document.getElementById(previewId);
    const label = document.getElementById(labelId);

    if (!input) return;
    input.addEventListener('change', () => {
        const file = input.files[0];
        if (file) {
            preview.src = URL.createObjectURL(file);
            preview.style.display = 'block';
            label.style.display = 'none';
        }
    });
}

document.addEventListener('DOMContentLoaded', async () => {
    const autenticado = await verificarSessaoAdmin();
    if (!autenticado) return;

    configurarPreviaArquivo('imgHorizFile', 'previewHoriz', 'labelHoriz');
    configurarPreviaArquivo('imgVertFile', 'previewVert', 'labelVert');

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
            await fetch('/usuarios/logout', { method: 'POST', credentials: 'same-origin' });
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
        console.error('Erro ao carregar catálogo:', error);
    }
}

function renderizarTabela(filmes) {
    const tbody = document.getElementById('tabela-filmes');
    tbody.innerHTML = '';

    filmes.forEach(filme => {
        const horizHtml = filme.caminhoImagemHorizontal
            ? `<img src="${filme.caminhoImagemHorizontal}?t=${Date.now()}" class="thumb-horiz" alt="Horizontal">`
            : '<small style="color:#777;">Sem img</small>';

        const vertHtml = filme.caminhoImagemVertical
            ? `<img src="${filme.caminhoImagemVertical}?t=${Date.now()}" class="thumb-vert" alt="Vertical">`
            : '<small style="color:#777;">Sem img</small>';

        const isSerie = filme.tipo === 'SERIE';
        const badgeTipo = isSerie
            ? '<span class="badge-tipo badge-serie">Série</span>'
            : '<span class="badge-tipo badge-filme">Filme</span>';

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${filme.id}</td>
            <td>${horizHtml}</td>
            <td>${vertHtml}</td>
            <td>${filme.titulo}</td>
            <td>${badgeTipo}</td>
            <td>${filme.genero || '-'}</td>
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

function obterGenerosSelecionados() {
    const marcados = document.querySelectorAll('input[name="generos"]:checked');
    return Array.from(marcados).map(cb => cb.value);
}

async function salvarFilme() {
    const id = document.getElementById('filmeId').value;
    const generosSelecionados = obterGenerosSelecionados();

    if (generosSelecionados.length === 0) {
        alert('Por favor, marque pelo menos um gênero.');
        return;
    }

    const inputHoriz = document.getElementById('imgHorizFile');
    const inputVert = document.getElementById('imgVertFile');

    const payload = {
        titulo: document.getElementById('titulo').value,
        descricao: document.getElementById('descricao').value,
        tipo: document.getElementById('tipo').value,
        generos: generosSelecionados,
        caminhoImagemHorizontal: document.getElementById('imgHorizAtual').value || null,
        caminhoImagemVertical: document.getElementById('imgVertAtual').value || null,
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

        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            alert(err.detalhes ? err.detalhes.join('\n') : (err.erro || 'Erro ao guardar a obra.'));
            return;
        }

        const filmeSalvo = await response.json();

        if (inputHoriz.files.length > 0 || inputVert.files.length > 0) {
            const formData = new FormData();
            if (inputHoriz.files.length > 0) {
                formData.append('imagemHorizontal', inputHoriz.files[0]);
            }
            if (inputVert.files.length > 0) {
                formData.append('imagemVertical', inputVert.files[0]);
            }

            const uploadRes = await fetch(`${API_URL}/${filmeSalvo.id}/imagens`, {
                method: 'POST',
                credentials: 'same-origin',
                body: formData
            });

            if (!uploadRes.ok) {
                const errUpload = await uploadRes.json().catch(() => ({}));
                alert(errUpload.erro || 'Obra salva, mas houve erro ao enviar as imagens.');
                await carregarFilmesAdmin();
                return;
            }
        }

        alert(id ? 'Obra atualizada com sucesso!' : 'Obra cadastrada com sucesso!');
        limparFormulario();
        await carregarFilmesAdmin();
    } catch (error) {
        console.error('Erro:', error);
    }
}

function prepararEdicao(id) {
    const filme = listaFilmesGlobal.find(f => f.id === id);
    if (!filme) return;

    document.getElementById('filmeId').value = filme.id;
    document.getElementById('titulo').value = filme.titulo;
    document.getElementById('tipo').value = filme.tipo || 'FILME';
    document.getElementById('descricao').value = filme.descricao;
    document.getElementById('planoId').value = filme.planoId;
    document.getElementById('imgHorizAtual').value = filme.caminhoImagemHorizontal || '';
    document.getElementById('imgVertAtual').value = filme.caminhoImagemVertical || '';

    // Marca os checkboxes dos gêneros da obra
    const generosDaObra = filme.generos || (filme.genero ? filme.genero.split(',').map(g => g.trim()) : []);
    document.querySelectorAll('input[name="generos"]').forEach(cb => {
        cb.checked = generosDaObra.some(g => g.toLowerCase() === cb.value.toLowerCase());
    });

    document.getElementById('imgHorizFile').value = '';
    document.getElementById('imgVertFile').value = '';

    const prevHoriz = document.getElementById('previewHoriz');
    const lblHoriz = document.getElementById('labelHoriz');
    if (filme.caminhoImagemHorizontal) {
        prevHoriz.src = `${filme.caminhoImagemHorizontal}?t=${Date.now()}`;
        prevHoriz.style.display = 'block';
        lblHoriz.style.display = 'none';
    } else {
        prevHoriz.style.display = 'none';
        lblHoriz.style.display = 'inline';
    }

    const prevVert = document.getElementById('previewVert');
    const lblVert = document.getElementById('labelVert');
    if (filme.caminhoImagemVertical) {
        prevVert.src = `${filme.caminhoImagemVertical}?t=${Date.now()}`;
        prevVert.style.display = 'block';
        lblVert.style.display = 'none';
    } else {
        prevVert.style.display = 'none';
        lblVert.style.display = 'inline';
    }

    document.getElementById('btnSalvar').innerText = 'Atualizar Obra';
    window.scrollTo(0, 0);
}

async function deletarFilme(id) {
    if (!confirm('Tem a certeza que deseja excluir esta obra permanentemente?')) return;

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
            await carregarFilmesAdmin();
        } else {
            alert('Erro ao excluir a obra.');
        }
    } catch (error) {
        console.error('Erro:', error);
    }
}

function limparFormulario() {
    document.getElementById('filmeForm').reset();
    document.getElementById('filmeId').value = '';
    document.getElementById('imgHorizAtual').value = '';
    document.getElementById('imgVertAtual').value = '';
    document.querySelectorAll('input[name="generos"]').forEach(cb => cb.checked = false);

    document.getElementById('previewHoriz').style.display = 'none';
    document.getElementById('labelHoriz').style.display = 'inline';
    document.getElementById('previewVert').style.display = 'none';
    document.getElementById('labelVert').style.display = 'inline';

    document.getElementById('btnSalvar').innerText = 'Salvar Obra';
}