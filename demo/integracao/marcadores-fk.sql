-- Executar após criação da tabela marcadores pela Pessoa 3, se ainda não houver FK equivalente.
-- Conferir a ausência de registros órfãos e restrições anteriores antes de executar.
BEGIN;
CREATE INDEX IF NOT EXISTS idx_marcadores_usuario ON marcadores(usuario_id);
ALTER TABLE marcadores ADD CONSTRAINT fk_marcadores_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE;
COMMIT;
