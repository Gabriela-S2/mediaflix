-- 1. Nova tabela para gerenciar os Planos
CREATE TABLE planos (
                        id SERIAL PRIMARY KEY,
                        nome VARCHAR(50) NOT NULL UNIQUE, -- Ex: 'Econômico', 'Padrão', 'Premium'
                        nivel INTEGER NOT NULL UNIQUE,    -- Ex: 1 (Econômico), 2 (Padrão), 3 (Premium)
                        tem_anuncios BOOLEAN DEFAULT FALSE
);

-- 2. Tabela de Usuários (agora vinculada ao Plano)
CREATE TABLE usuarios(
                         id SERIAL PRIMARY KEY,
                         username VARCHAR(100) NOT NULL UNIQUE,
                         email VARCHAR(100) NOT NULL UNIQUE,
                         senha VARCHAR(100) NOT NULL,
                         caminho_foto TEXT,
                         codigo_recuperacao VARCHAR(6),
                         codigo_expiracao TIMESTAMP,
                         is_admin BOOLEAN DEFAULT FALSE,
                         plano_id INTEGER REFERENCES planos(id) -- Se for Admin, pode ser NULL ou receber o Premium por padrão
);

-- 3. Tabela de Filmes (vinculada ao Plano Mínimo Exigido)
CREATE TABLE filme(
                      id SERIAL PRIMARY KEY,
                      titulo VARCHAR(100) NOT NULL,
                      descricao TEXT,
                      genero VARCHAR(50),
                      caminho_imagem_horizontal VARCHAR(255),
                      caminho_imagem_vertical VARCHAR(255),
                      plano_id INTEGER REFERENCES planos(id) NOT NULL -- Define qual é o plano mínimo para assistir
);


CREATE TABLE recuperacao_senha(
                                  id SERIAL PRIMARY KEY,
                                  email VARCHAR(100) NOT NULL,
                                  codigo VARCHAR(6) NOT NULL,
                                  data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Tabela de Histórico/Relação Usuário e Filme (corrigida)
CREATE TABLE usuario_filmes(
                               usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                               filme_id INTEGER NOT NULL REFERENCES filme(id) ON DELETE CASCADE,
                               tempo_assistido BIGINT DEFAULT 0,
                               PRIMARY KEY (usuario_id, filme_id)
);